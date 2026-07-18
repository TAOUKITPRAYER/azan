#Requires -Version 5.1

[CmdletBinding()]
param(
    [Parameter(Position=0)]
    [string]$ipLast,

    [switch]$Help
)

Set-StrictMode -Off

# ===== CONSTANTES =====
$REMOTE_PATH   = "/data/local/tmp/busybox"
$DOWNLOAD_BASE = "https://busybox.net/downloads/binaries/1.35.0-x86_64-linux-musl"

# Table arch Android -> nom du binaire busybox
$ARCH_MAP = @{
    "arm64-v8a"    = "busybox-armv8l"
    "armeabi-v7a"  = "busybox-armv7l"
    "armeabi"      = "busybox-armv7l"
    "x86_64"       = "busybox-x86_64"
    "x86"          = "busybox-i686"
}

# ===== AIDE =====
function Show-Help {
    $s = Split-Path $MyInvocation.ScriptName -Leaf
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "  install-busybox.ps1  -  BusyBox via ADB" -ForegroundColor Cyan
    Write-Host "  tar, gzip & 300 autres commandes" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan

    Write-Host ""
    Write-Host "DESCRIPTION :" -ForegroundColor Yellow
    Write-Host "  Detecte l'architecture de la box Android, verifie si le binaire"
    Write-Host "  BusyBox correspondant est present localement, puis l'installe"
    Write-Host "  dans /data/local/tmp/busybox sur la box (sans root requis)."

    Write-Host ""
    Write-Host "SYNTAXE :" -ForegroundColor Yellow
    Write-Host "  install-busybox.ps1 <IP_LAST> [-Help]"

    Write-Host ""
    Write-Host "PARAMETRES :" -ForegroundColor Yellow
    Write-Host "  IP_LAST   Derniers chiffres de l'IP  ex: 28  =>  192.168.1.28"
    Write-Host "  -Help     Affiche cette aide"

    Write-Host ""
    Write-Host "EXEMPLE :" -ForegroundColor Yellow
    Write-Host "  install-busybox.ps1 28"

    Write-Host ""
    Write-Host "ARCHITECTURES SUPPORTEES :" -ForegroundColor Yellow
    Write-Host "  arm64-v8a   ->  busybox-armv8l"
    Write-Host "  armeabi-v7a ->  busybox-armv7l"
    Write-Host "  x86_64      ->  busybox-x86_64"
    Write-Host "  x86         ->  busybox-i686"

    Write-Host ""
    Write-Host "TELECHARGEMENT MANUEL :" -ForegroundColor Yellow
    Write-Host "  $DOWNLOAD_BASE/"
    Write-Host ""
}

if ($Help -or -not $ipLast) {
    Show-Help
    exit 0
}

# ===== CONFIG =====
$adbPort = 5555
$ip      = "192.168.1.${ipLast}:${adbPort}"

# ===== CHECK ADB =====
Write-Host ""
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host "  [ERREUR] ADB non trouve dans le PATH" -ForegroundColor Red
    exit 1
}

# ===== CONNEXION =====
Write-Host "  [1/5] Connexion a $ip ..." -ForegroundColor Cyan
adb connect $ip 2>&1 | Out-Null
Start-Sleep -Seconds 1

$ipEscaped = [regex]::Escape($ip)
$deviceOK  = (adb devices) | Select-String -Pattern "${ipEscaped}\s+device"
if (-not $deviceOK) {
    Write-Host "  [ERREUR] Box non connectee ou non autorisee ($ip)" -ForegroundColor Red
    Write-Host "           Verifiez que ADB WiFi est active sur la box (port $adbPort)" -ForegroundColor Yellow
    exit 1
}
Write-Host "           Connecte" -ForegroundColor Green

# ===== DETECTION ARCHITECTURE =====
Write-Host "  [2/5] Detection de l'architecture ..." -ForegroundColor Cyan

# Essai 1 : sans guillemets (plus compatible sur certaines boxes)
$abi = adb shell getprop ro.product.cpu.abi 2>$null
if (-not $abi) {
    # Essai 2 : avec guillemets
    $abi = adb shell "getprop ro.product.cpu.abi" 2>$null
}
if (-not $abi) {
    # Essai 3 : via cmd /c pour contourner les problemes de pipe PS
    $abi = cmd /c "adb shell getprop ro.product.cpu.abi" 2>$null
}

# Nettoyer : trim + supprimer caracteres de controle eventuels
$abi = "$abi".Trim() -replace '[^\x20-\x7E]', ''

if (-not $abi) {
    Write-Host "  [ERREUR] Impossible de lire ro.product.cpu.abi" -ForegroundColor Red
    Write-Host "           Verifiez la connexion ADB avec : adb shell getprop ro.product.cpu.abi" -ForegroundColor Yellow
    exit 1
}

Write-Host "           ABI detecte : $abi" -ForegroundColor White

if (-not $ARCH_MAP.ContainsKey($abi)) {
    Write-Host "  [ERREUR] Architecture non supportee : '$abi'" -ForegroundColor Red
    Write-Host "           Architectures supportees : $($ARCH_MAP.Keys -join ', ')" -ForegroundColor Yellow
    exit 1
}

$binaryName = $ARCH_MAP[$abi]
Write-Host "           Binaire requis  : $binaryName" -ForegroundColor White

# ===== VERIFICATION BINAIRE LOCAL =====
Write-Host "  [3/5] Recherche de '$binaryName' dans le repertoire courant ..." -ForegroundColor Cyan
$localPath = Join-Path (Get-Location) $binaryName

if (-not (Test-Path $localPath)) {
    Write-Host "           Introuvable." -ForegroundColor Red
    Write-Host ""
    Write-Host "  ┌──────────────────────────────────────────────────────────────┐" -ForegroundColor Yellow
    Write-Host "  │  BINAIRE MANQUANT - Comment le telecharger :                 │" -ForegroundColor Yellow
    Write-Host "  │                                                              │" -ForegroundColor Yellow
    Write-Host "  │  Option A - PowerShell (recommande) :                        │" -ForegroundColor Yellow
    Write-Host "  │                                                              │" -ForegroundColor Yellow
    Write-Host "  │    Invoke-WebRequest ``                                       │" -ForegroundColor White
    Write-Host "  │      -Uri `"$DOWNLOAD_BASE/$binaryName`" ``" -ForegroundColor White
    Write-Host "  │      -OutFile `"$binaryName`"                                  │" -ForegroundColor White
    Write-Host "  │                                                              │" -ForegroundColor Yellow
    Write-Host "  │  Option B - Navigateur :                                     │" -ForegroundColor Yellow
    Write-Host "  │    $DOWNLOAD_BASE/  │" -ForegroundColor White
    Write-Host "  │    Telecharger : $binaryName                                 │" -ForegroundColor White
    Write-Host "  │    Placer dans  : $(Get-Location)   │" -ForegroundColor White
    Write-Host "  │                                                              │" -ForegroundColor Yellow
    Write-Host "  │  Puis relancer ce script.                                    │" -ForegroundColor Yellow
    Write-Host "  └──────────────────────────────────────────────────────────────┘" -ForegroundColor Yellow
    Write-Host ""

    # Proposition de telechargement automatique
    $dl = Read-Host "  Telecharger automatiquement maintenant ? (y/n)"
    if ($dl -eq "y") {
        Write-Host "  Telechargement en cours ..." -ForegroundColor Cyan
        try {
            Invoke-WebRequest -Uri "$DOWNLOAD_BASE/$binaryName" -OutFile $localPath -UseBasicParsing
            Write-Host "  Telechargement OK : $localPath" -ForegroundColor Green
        } catch {
            Write-Host "  [ERREUR] Telechargement echoue : $_" -ForegroundColor Red
            Write-Host "           Telechargez manuellement et relancez le script." -ForegroundColor Yellow
            adb disconnect $ip | Out-Null
            exit 1
        }
    } else {
        Write-Host "  Annule. Relancez apres avoir place '$binaryName' ici." -ForegroundColor Yellow
        adb disconnect $ip | Out-Null
        exit 0
    }
}

$fileSize = (Get-Item $localPath).Length
Write-Host "           Trouve ($([math]::Round($fileSize/1KB)) KB)" -ForegroundColor Green

# ===== INSTALLATION SUR LA BOX =====
Write-Host "  [4/5] Installation sur la box -> $REMOTE_PATH ..." -ForegroundColor Cyan
adb push $localPath $REMOTE_PATH 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "  [ERREUR] adb push echoue (code $LASTEXITCODE)" -ForegroundColor Red
    adb disconnect $ip | Out-Null
    exit 1
}

adb shell "chmod 755 $REMOTE_PATH" 2>&1 | Out-Null
Write-Host "           Push + chmod OK" -ForegroundColor Green

# ===== VERIFICATION =====
Write-Host "  [5/5] Verification ..." -ForegroundColor Cyan
$version = adb shell "$REMOTE_PATH --help 2>&1 | head -1"

if ($version) {
    Write-Host "           BusyBox operationnel" -ForegroundColor Green
    Write-Host "           $version" -ForegroundColor Gray

    Write-Host ""
    Write-Host "  Installation reussie !" -ForegroundColor Green
    Write-Host "  Chemin sur la box : $REMOTE_PATH" -ForegroundColor White
    Write-Host ""
    Write-Host "  Test tar :" -ForegroundColor Yellow
    Write-Host "    adb shell `"$REMOTE_PATH tar --help`"" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host "  [ERREUR] Le binaire ne repond pas apres installation" -ForegroundColor Red
    Write-Host "           Verifiez que l'architecture '$abi' est bien supportee" -ForegroundColor Yellow
    adb disconnect $ip | Out-Null
    exit 1
}

# ========= adb disconnect $ip | Out-Null
