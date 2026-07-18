<#
.SYNOPSIS
    Copie un fichier ou un dossier vers une box Android via ADB WiFi.

.DESCRIPTION
    Copie un fichier ou un dossier depuis Windows vers /sdcard/Download
    sur un appareil Android connecté en ADB over WiFi (192.168.1.x:5555).

.PARAMETER IP_LAST
    Dernier octet de l'IP (ex: 47 -> 192.168.1.47)

.PARAMETER SRC_PATH
    Chemin source (fichier ou dossier)

.EXAMPLE
    .\cpcoran.ps1 47 "C:\Users\Youssef\Desktop\CORAN\C4"
    .\cpcoran.ps1 47 C:\CORAN\fichier.mp3
#>

param (
    [Parameter(Position=0, Mandatory=$true)]
    [string]$IP_LAST,

    [Parameter(Position=1, Mandatory=$true)]
    [string]$SRC_PATH,

    [switch]$Help
)

if ($Help) {
    Write-Host @"
Usage: .\cpcoran.ps1 IP_LAST SRC_PATH

Exemples:
  .\cpcoran.ps1 47 "C:\Users\Youssef\Desktop\CORAN\C4"
  .\cpcoran.ps1 47 C:\CORAN\fichier.mp3

Description:
  Copie un fichier ou dossier vers /sdcard/Download sur la box Android.
  IP fixe: 192.168.1.x:5555
"@ -ForegroundColor Cyan
    exit 0
}

# ── Configuration ────────────────────────────────────────────────────────────
$BASE_IP   = "192.168.1."
$PORT      = "5555"
$DEST_BASE = "/sdcard/Download"

# ── Logging ──────────────────────────────────────────────────────────────────
function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $ts = Get-Date -Format "HH:mm:ss"
    switch ($Level) {
        "ERROR"   { Write-Host "[$ts] ERROR:   $Message" -ForegroundColor Red     }
        "WARNING" { Write-Host "[$ts] WARNING: $Message" -ForegroundColor Yellow  }
        "SUCCESS" { Write-Host "[$ts] OK:      $Message" -ForegroundColor Green   }
        "DEBUG"   { Write-Host "[$ts] DEBUG:   $Message" -ForegroundColor DarkGray}
        default   { Write-Host "[$ts] INFO:    $Message" -ForegroundColor White   }
    }
}

# ── Prérequis ADB ────────────────────────────────────────────────────────────
function Test-Prerequisites {
    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        Write-Log "ADB introuvable dans le PATH." "ERROR"
        return $false
    }
    Write-Log "ADB disponible." "SUCCESS"
    return $true
}

# ── Résolution du chemin source ───────────────────────────────────────────────
function Resolve-SourcePath {
    param([string]$Path)

    $Path = $Path.Trim('"').Trim("'")

    if (-not (Test-Path -LiteralPath $Path)) {
        Write-Log "Chemin source introuvable : '$Path'" "ERROR"
        return $null
    }

    $resolved = (Resolve-Path -LiteralPath $Path -ErrorAction Stop).Path
    Write-Log "Source : $resolved" "INFO"
    return $resolved
}

# ── Connexion ADB ─────────────────────────────────────────────────────────────
function Connect-ADB {
    param([string]$FullIP)

    Write-Log "Connexion à $FullIP..." "INFO"
    & adb connect $FullIP 2>&1 | Out-Null
    Start-Sleep -Seconds 2

    $ipEscaped = [regex]::Escape($FullIP)
    $ok = (& adb devices 2>&1) | Select-String -Pattern "${ipEscaped}\s+device"
    if (-not $ok) {
        Write-Log "Impossible de joindre $FullIP. ADB WiFi activé sur la box ?" "ERROR"
        return $false
    }

    Write-Log "Connecté à $FullIP." "SUCCESS"
    return $true
}

# ── Taille source ─────────────────────────────────────────────────────────────
function Get-SourceSize {
    param([string]$Path)
    try {
        if (Test-Path -LiteralPath $Path -PathType Container) {
            $bytes = (Get-ChildItem -LiteralPath $Path -Recurse -Force -ErrorAction SilentlyContinue |
                      Measure-Object Length -Sum).Sum
        } else {
            $bytes = (Get-Item -LiteralPath $Path).Length
        }
        $bytes = [int64]($bytes)
        if ($bytes -ge 1GB) {
            Write-Log ("Taille source : {0:0.00} GB" -f ($bytes / 1GB)) "INFO"
        } else {
            Write-Log ("Taille source : {0:0.00} MB" -f ($bytes / 1MB)) "INFO"
        }
        return $bytes
    } catch {
        Write-Log "Impossible de calculer la taille : $_" "WARNING"
        return $null
    }
}

# ── Espace libre sur la box ───────────────────────────────────────────────────
function Get-DeviceFreeSpace {
    # Essaie /sdcard puis le stockage interne
    foreach ($mountPoint in @("/sdcard", "/storage/emulated/0", "/data")) {
        $df = & adb shell "df '$mountPoint' 2>/dev/null" 2>$null
        if (-not $df) { continue }

        # Format Android : ligne "filesystem 1K-blocks Used Available Use% Mountpoint"
        $dataLine = $df | Select-String -Pattern '^\S' | Select-Object -Last 1
        if (-not $dataLine) { continue }

        $parts = ($dataLine.Line -split '\s+') | Where-Object { $_ -ne '' }
        # Cherche la colonne "Available" (4e champ en général, parfois en Ko)
        if ($parts.Count -ge 4 -and $parts[3] -match '^\d+$') {
            $freeKB = [int64]$parts[3]
            $freeBytes = $freeKB * 1024
            if ($freeBytes -ge 1GB) {
                Write-Log ("Espace libre : {0:0.00} GB" -f ($freeBytes / 1GB)) "INFO"
            } else {
                Write-Log ("Espace libre : {0:0.00} MB" -f ($freeBytes / 1MB)) "INFO"
            }
            return $freeBytes
        }
    }

    Write-Log "Impossible de lire l'espace disponible (non bloquant)." "WARNING"
    return $null
}

# ── Copie principale avec indicateur d'avancement ────────────────────────────
#
#  Stratégie :
#    - Dossier  : énumération locale puis push fichier par fichier
#                 -> Write-Progress [X/N fichiers + %]
#    - Fichier  : push direct avec spinner (pas de byte-level progress en PS)
#
function Copy-ToDevice {
    param(
        [string]$SourcePath,
        [string]$DestBase       # /sdcard/Download
    )

    $isDir = Test-Path -LiteralPath $SourcePath -PathType Container

    # ── CAS DOSSIER ──────────────────────────────────────────────────────────
    if ($isDir) {
        $folderName = Split-Path $SourcePath -Leaf
        $destFolder = "$DestBase/$folderName"

        Write-Log "Mode dossier -> destination : $destFolder/" "INFO"

        # Enumération complète avant de commencer
        $files = @(Get-ChildItem -LiteralPath $SourcePath -Recurse -File -Force)
        $total = $files.Count

        if ($total -eq 0) {
            Write-Log "Aucun fichier trouvé dans '$SourcePath'." "WARNING"
            return $true
        }

        Write-Log "$total fichier(s) à copier." "INFO"

        $done   = 0
        $failed = 0
        $srcLen = $SourcePath.TrimEnd('\').Length

        foreach ($file in $files) {
            $done++

            # Chemin relatif -> chemin distant (backslash -> slash)
            $relative = $file.FullName.Substring($srcLen).TrimStart('\').Replace('\', '/')
            $destPath = "$destFolder/$relative"
            $destDir  = $destPath -replace '/[^/]+$', ''

            # Créer le sous-dossier distant si besoin
            & adb shell "mkdir -p '$destDir'" 2>&1 | Out-Null

            $pct    = [int](($done / $total) * 100)
            $sizeMB = "{0:0.0} MB" -f ($file.Length / 1MB)

            Write-Progress `
                -Activity "Copie vers $destFolder" `
                -Status   "[$done/$total]  $($file.Name)  ($sizeMB)" `
                -PercentComplete $pct

            $out = & adb push "$($file.FullName)" "$destPath" 2>&1
            if ($LASTEXITCODE -ne 0) {
                Write-Log "ÉCHEC [$done/$total] $($file.Name) : $out" "ERROR"
                $failed++
            } else {
                Write-Log "OK    [$done/$total] $($file.Name)" "DEBUG"
            }
        }

        Write-Progress -Activity "Copie vers $destFolder" -Completed

        if ($failed -gt 0) {
            Write-Log "$failed fichier(s) en échec sur $total." "ERROR"
            return $false
        }

    # ── CAS FICHIER UNIQUE ───────────────────────────────────────────────────
    } else {
        $fileName = Split-Path $SourcePath -Leaf
        $destFile = "$DestBase/$fileName"
        $sizeMB   = "{0:0.0} MB" -f ((Get-Item -LiteralPath $SourcePath).Length / 1MB)

        Write-Log "Mode fichier -> $destFile  ($sizeMB)" "INFO"

        # Lancer le push dans un job pour pouvoir afficher un spinner en parallèle
        $job = Start-Job -ScriptBlock {
            param($src, $dst)
            & adb push "$src" "$dst" 2>&1
        } -ArgumentList $SourcePath, $destFile

        $spin  = '|', '/', '-', '\'
        $frame = 0
        while ($job.State -eq 'Running') {
            Write-Progress `
                -Activity "Copie en cours..." `
                -Status   "$($spin[$frame % 4])  $fileName  ($sizeMB)" `
                -PercentComplete -1
            $frame++
            Start-Sleep -Milliseconds 200
        }
        Write-Progress -Activity "Copie en cours..." -Completed

        $out      = Receive-Job $job
        $exitCode = $job.ChildJobs[0].JobStateInfo.Reason
        Remove-Job $job

        # Un job ne remonte pas $LASTEXITCODE directement ; on détecte l'échec
        # sur la présence de "error" dans la sortie adb
        if ($out -match 'error') {
            Write-Log "Échec : $out" "ERROR"
            return $false
        }

        Write-Log ($out | Select-Object -Last 1) "DEBUG"
    }

    Write-Log "Copie terminée avec succès." "SUCCESS"
    return $true
}

# ── Main ──────────────────────────────────────────────────────────────────────
function Main {
    Write-Log "=== cpcoran.ps1 démarrage ===" "INFO"
    Write-Log "IP_LAST  : $IP_LAST" "DEBUG"
    Write-Log "SRC_PATH : $SRC_PATH" "DEBUG"

    if (-not (Test-Prerequisites)) { exit 1 }

    $src = Resolve-SourcePath -Path $SRC_PATH
    if (-not $src) { exit 1 }

    $fullIP = "${BASE_IP}${IP_LAST}:${PORT}"

    if (-not (Connect-ADB -FullIP $fullIP)) { exit 1 }

    $srcSize  = Get-SourceSize -Path $src
    $freeSize = Get-DeviceFreeSpace

    if ($srcSize -and $freeSize -and ($srcSize -gt $freeSize)) {
        $needGB = [math]::Round($srcSize  / 1GB, 2)
        $freeGB = [math]::Round($freeSize / 1GB, 2)
        Write-Log "Espace insuffisant : besoin $needGB GB, disponible $freeGB GB." "ERROR"
        exit 1
    }

    if (-not (Copy-ToDevice -SourcePath $src -DestBase $DEST_BASE)) {
        Write-Log "=== Échec ===" "ERROR"
        exit 1
    }

    Write-Log "=== Terminé avec succès ===" "SUCCESS"
    exit 0
}

try {
    Main
} catch {
    Write-Log "Erreur non gérée : $_" "ERROR"
    Write-Log $_.ScriptStackTrace "DEBUG"
    exit 1
}
