#Requires -Version 5.1
<#
.SYNOPSIS
    Copie de fichiers vers une box Android via ADB over WiFi.
.DESCRIPTION
    Supporte la copie par pattern (*.mp3, *.*), de dossiers complets (-R / -RF)
    et un mode ultra rapide via tar (-fast).
.PARAMETER ipLast
    Dernier(s) octet(s) de l'adresse IP (ex: 28  ->  192.168.1.28)
.PARAMETER modeOrPattern
    -R   : copie recursive avec confirmation si le dossier cible existe deja
    -RF  : copie recursive sans confirmation (ecrasement force)
    *.*  / *.mp3 / etc. : filtre de fichiers dans $srcDir (non recursif)
.PARAMETER srcDir
    Repertoire source
.PARAMETER threads
    Copies paralleles (defaut : 4, min : 1, max : 32)
.PARAMETER filter
    Filtre additionnel applique apres $modeOrPattern (utile en mode -R)
.PARAMETER fast
    Mode ultra rapide : tar local -> pipe -> tar distant (si tar dispo sur la box)
.PARAMETER log
    Active l'ecriture d'un fichier log ($PSScriptRoot\cpfile.log)
.PARAMETER Help
    Affiche cette aide
.EXAMPLE
    cpfile.ps1 28 *.* C:\DATA
.EXAMPLE
    cpfile.ps1 28 *.mp3 C:\CORAN -threads 8
.EXAMPLE
    cpfile.ps1 28 -R C:\CORAN\ABDULBASIT
.EXAMPLE
    cpfile.ps1 28 -RF C:\CORAN\ABDULBASIT
.EXAMPLE
    cpfile.ps1 28 -R C:\CORAN\ABDULBASIT -fast
.NOTES
    Destination par defaut : /sdcard/Download
    Prerequis : ADB dans le PATH, Android Debug Bridge active sur la box.
#>

# ── param() DOIT etre la premiere instruction non-commentaire ─────────────────
[CmdletBinding()]
param(
    [Parameter(Position=0)]
    [string]$ipLast,

    # Accepte soit -R/-RF comme vrais switchs, soit un pattern positionnel (*.mp3, *.*)
    [Parameter(Position=1)]
    [string]$modeOrPattern,

    [Parameter(Position=2)]
    [string]$srcDir,

    [ValidateRange(1,32)]
    [int]$threads = 4,

    [string]$filter,

    [switch]$R,
    [switch]$RF,
    [switch]$fast,
    [switch]$log,
    [switch]$Help
)

Set-StrictMode -Off

# Normalise $modeOrPattern depuis les switchs -R / -RF
# Quand -R/-RF est un vrai switch, le chemin source tombe en position 1
# ($modeOrPattern) au lieu de la position 2 ($srcDir) -> on le recupere.
if ($RF) {
    if (-not $srcDir -and $modeOrPattern) { $srcDir = $modeOrPattern }
    $modeOrPattern = '-RF'
} elseif ($R) {
    if (-not $srcDir -and $modeOrPattern) { $srcDir = $modeOrPattern }
    $modeOrPattern = '-R'
}

# ===== HELP =====
if ($Help -or -not $ipLast) {
    $s = Split-Path $MyInvocation.MyCommand.Path -Leaf
    Write-Host ""
    Write-Host "=============================================" -ForegroundColor Cyan
    Write-Host "  $s  -  Copie de fichiers vers box ADB WiFi" -ForegroundColor Cyan
    Write-Host "=============================================" -ForegroundColor Cyan

    Write-Host ""
    Write-Host "SYNTAXE :" -ForegroundColor Yellow
    Write-Host "  $s <IP_LAST> <MODE|PATTERN> <SOURCE> [options]"

    Write-Host ""
    Write-Host "PARAMETRES POSITIONNELS :" -ForegroundColor Yellow
    Write-Host "  IP_LAST       Derniers chiffres de l'IP   ex: 28  =>  192.168.1.28"
    Write-Host "  MODE|PATTERN  -R   : copie recursive (confirmation si cible existe)"
    Write-Host "                -RF  : copie recursive forcee (sans confirmation)"
    Write-Host "                *.*  / *.mp3 / etc. : filtre de fichiers (non recursif)"
    Write-Host "  SOURCE        Repertoire source local"

    Write-Host ""
    Write-Host "OPTIONS :" -ForegroundColor Yellow
    Write-Host "  -threads N    Copies paralleles          (defaut : 4, min 1, max 32)"
    Write-Host "  -filter PAT   Filtre additionnel         ex: -filter *.mp3"
    Write-Host "  -fast         Mode ultra-rapide via tar  (si tar dispo sur la box)"
    Write-Host "  -log          Ecriture du log dans       $PSScriptRoot\cpfile.log"
    Write-Host "  -Help         Affiche cette aide"

    Write-Host ""
    Write-Host "DESTINATION :" -ForegroundColor Yellow
    Write-Host "  /sdcard/Download  (fixe)"

    Write-Host ""
    Write-Host "EXEMPLES :" -ForegroundColor Yellow
    Write-Host "  $s 28 *.*  C:\DATA"
    Write-Host "  $s 28 *.mp3 C:\CORAN -threads 8"
    Write-Host "  $s 28 -R   C:\CORAN\ABDULBASIT"
    Write-Host "  $s 28 -RF  C:\CORAN\ABDULBASIT"
    Write-Host "  $s 28 -R   C:\CORAN\ABDULBASIT -fast"
    Write-Host "  $s 28 -R   C:\CORAN\ABDULBASIT -filter *.mp3"

    Write-Host ""
    Write-Host "PREREQUIS :" -ForegroundColor Yellow
    Write-Host "  - ADB present dans le PATH"
    Write-Host "  - Android Debug Bridge active sur la box (port 5555)"
    Write-Host "  - Box sur le meme reseau WiFi (sous-reseau 192.168.1.x)"
    Write-Host ""
    exit 0
}

# ===== CONFIG =====
$adbPort    = 5555
$ip         = "192.168.1.${ipLast}:${adbPort}"
$targetBase = "/sdcard/Download"
$logFile    = "$PSScriptRoot\cpfile.log"

# ===== LOG =====
function Write-Log([string]$msg) {
    if ($script:log) {
        Add-Content -Path $script:logFile -Value "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') - $msg"
    }
}

# ===== CHECK ADB =====
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host "X ADB non trouve dans le PATH" -ForegroundColor Red
    exit 1
}

# ===== CHECK SOURCE =====
if (-not $srcDir -or -not (Test-Path $srcDir)) {
    Write-Host "X Repertoire source introuvable : '$srcDir'" -ForegroundColor Red
    exit 1
}
$srcDir = (Resolve-Path $srcDir).Path   # chemin canonique, sans slash final

# ===== CONNECT =====
Write-Host "Connexion a $ip ..." -ForegroundColor Cyan
adb connect $ip 2>&1 | Out-Null
Start-Sleep -Seconds 1

# FIX : echapper les caracteres regex dans $ip (points, deux-points)
$ipEscaped = [regex]::Escape($ip)
$deviceOK  = (adb devices) | Select-String -Pattern "${ipEscaped}\s+device"
if (-not $deviceOK) {
    Write-Host "X Box non connectee ou non autorisee ($ip)" -ForegroundColor Red
    Write-Host "  Verifiez que le mode ADB WiFi est active (port $adbPort)" -ForegroundColor Yellow
    exit 1
}
Write-Host "OK Connecte a $ip" -ForegroundColor Green
Write-Log "CONNECT $ip"

# ===== MODE RAPIDE (tar) =====
if ($fast) {
    Write-Host "Mode ultra rapide (tar)" -ForegroundColor Cyan

    $folderName = Split-Path $srcDir -Leaf
    $targetDir  = "$targetBase/$folderName"

    Write-Host "  -> Preparation de '$targetDir' sur la box..." -ForegroundColor Gray
    adb shell "rm -rf '$targetDir' && mkdir -p '$targetDir'" 2>&1 | Out-Null

    $tarAvail = adb shell "which tar 2>/dev/null"
    if ($tarAvail) {
        Write-Host "  -> Envoi par pipe tar..." -ForegroundColor Gray
        cmd /c "tar -cf - -C `"$srcDir`" . | adb shell `"cd '$targetDir' && tar -xf -`""
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Copie rapide terminee -> $targetDir" -ForegroundColor Green
            Write-Log "FAST_OK $srcDir -> $targetDir"
        } else {
            Write-Host "X Echec tar (code $LASTEXITCODE)" -ForegroundColor Red
            Write-Log "FAST_FAIL $srcDir code=$LASTEXITCODE"
        }
    } else {
        Write-Host "WARN tar non disponible sur la box -> fallback adb push" -ForegroundColor Yellow
        adb push "$srcDir" "$targetBase" 2>&1
        Write-Log "FAST_FALLBACK $srcDir -> $targetBase"
    }

    adb disconnect $ip | Out-Null
    exit
}

# ===== COLLECTE DES FICHIERS =====
$folderName  = $null
$isRecursive = ($modeOrPattern -eq "-R" -or $modeOrPattern -eq "-RF")

if ($isRecursive) {
    $folderName = Split-Path $srcDir -Leaf
    $targetDir  = "$targetBase/$folderName"

    $exists = adb shell "[ -d '$targetDir' ] && echo EXISTS"
    if ($exists -match "EXISTS") {
        if ($modeOrPattern -eq "-R") {
            $confirm = Read-Host "WARN '$targetDir' existe deja sur la box. Ecraser ? (y/n)"
            if ($confirm -ne "y") {
                Write-Host "Annule." -ForegroundColor Yellow
                adb disconnect $ip | Out-Null
                exit 0
            }
        }
        Write-Host "  -> Suppression de '$targetDir'..." -ForegroundColor Gray
        adb shell "rm -rf '$targetDir'" 2>&1 | Out-Null
    }

    adb shell "mkdir -p '$targetBase'" 2>&1 | Out-Null
    $files = Get-ChildItem -Path $srcDir -Recurse -File

} else {
    $targetDir = $targetBase
    $files = Get-ChildItem -Path $srcDir -Filter $modeOrPattern -File
}

# ===== FILTRE ADDITIONNEL =====
if ($filter) {
    $files = $files | Where-Object { $_.Name -like $filter }
}

$total = @($files).Count
if ($total -eq 0) {
    Write-Host "X Aucun fichier correspondant au critere" -ForegroundColor Red
    adb disconnect $ip | Out-Null
    exit 0
}

Write-Host "$total fichier(s) a copier -> $targetDir" -ForegroundColor Cyan
Write-Log "START total=$total src=$srcDir"

# ===== THREAD JOB : chargement avec fallback Start-Job =====
$useThreadJob = $false
if (Get-Module -ListAvailable -Name ThreadJob -ErrorAction SilentlyContinue) {
    Import-Module ThreadJob -ErrorAction SilentlyContinue
    $useThreadJob = $true
}

# ===== BOUCLE DE COPIE =====
$allJobs    = [System.Collections.Generic.List[object]]::new()
$dispatched = 0

$jobBlock = {
    param([string]$src, [string]$dst, [string]$isRec)

    # Creer le repertoire distant si necessaire (mode recursif)
    if ($isRec -eq '1') {
        $remoteDir = $dst -replace '/[^/]+$', ''
        adb shell "mkdir -p '$remoteDir'" 2>&1 | Out-Null
    }

    # Retry x3
    for ($i = 0; $i -lt 3; $i++) {
        adb push "$src" "$dst" 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) { return "OK:$dst" }
        Start-Sleep -Seconds 1
    }
    return "FAIL:$src"
}

foreach ($file in $files) {

    # Throttle : attendre qu'un slot se libere
    while (($allJobs | Where-Object { $_.State -eq 'Running' }).Count -ge $threads) {
        Start-Sleep -Milliseconds 150
    }

    $dispatched++
    $pct = [int](($dispatched / $total) * 100)
    Write-Progress -Activity "Copie en cours" `
                   -Status "$dispatched / $total  --  $($file.Name)" `
                   -PercentComplete $pct

    # Calcul destination
    if ($isRecursive) {
        $relative = $file.FullName.Substring($srcDir.Length).TrimStart('\').Replace('\','/')
        $dst = "$targetBase/$folderName/$relative"
    } else {
        $dst = "$targetBase/$($file.Name)"
    }

    $argList = @($file.FullName, $dst, ([string][int]$isRecursive))

    if ($useThreadJob) {
        $j = Start-ThreadJob -ScriptBlock $jobBlock -ArgumentList $argList
    } else {
        $j = Start-Job -ScriptBlock $jobBlock -ArgumentList $argList
    }
    $allJobs.Add($j)

    Write-Log "DISPATCH $($file.FullName) -> $dst"
}

# ===== ATTENTE FIN DES TRANSFERTS =====
Write-Progress -Activity "Copie en cours" -Status "Finalisation des transferts..." -PercentComplete 99
$allJobs | Wait-Job | Out-Null
Write-Progress -Activity "Copie en cours" -Completed

# ===== COLLECTE DES RESULTATS =====
$failed = [System.Collections.Generic.List[string]]::new()
foreach ($j in $allJobs) {
    $result = Receive-Job $j 2>&1
    if ($result -like "FAIL:*") {
        $failed.Add($result.Substring(5))
        Write-Log "FAIL $($result.Substring(5))"
    }
}
$allJobs | Remove-Job

# ===== RESUME FINAL =====
$ok = $total - $failed.Count
Write-Host ""
if ($failed.Count -eq 0) {
    Write-Host "OK $ok / $total fichier(s) copie(s) avec succes" -ForegroundColor Green
} else {
    Write-Host "WARN $ok / $total reussi(s)  --  $($failed.Count) echec(s) :" -ForegroundColor Yellow
    $failed | ForEach-Object { Write-Host "   X $_" -ForegroundColor Red }
}

Write-Log "DONE ok=$ok fail=$($failed.Count)"
adb disconnect $ip | Out-Null
