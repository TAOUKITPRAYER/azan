#requires -version 5.1
<#
    instapk_hijriadmin.ps1 — build/signature/deploiement de Hijri Admin
    (net.tawkit.hijriadmin), le petit outil perso pour saisir les dates
    officielles de debut de mois hijri par pays dans Supabase
    (table public.hijri_month_starts). Miroir de release\instapk.ps1,
    mais pour ce module separe (hijri-admin/).

    Usage :
        powershell -ExecutionPolicy Bypass -File release\instapk_hijriadmin.ps1 <action>

    Actions :
        apk         Compile la release (gradlew :hijri-admin:assembleRelease),
                    aligne (zipalign) et signe (apksigner) avec tawkit.jks
                    -> genere release\hijri-admin.apk
        install     Installe/met a jour release\hijri-admin.apk sur l'appareil
                    adb courant
        help        Affiche ce message (comportement par defaut si aucun
                    parametre n'est fourni)

    Exemples :
        powershell -ExecutionPolicy Bypass -File release\instapk_hijriadmin.ps1 apk
        powershell -ExecutionPolicy Bypass -File release\instapk_hijriadmin.ps1 install
#>

param(
    [Parameter(Position = 0)]
    [string]$Action
)

$ErrorActionPreference = 'Stop'

$ReleaseDir  = $PSScriptRoot
$ProjectRoot = Split-Path $ReleaseDir -Parent
$Keystore    = Join-Path $ProjectRoot 'tawkit.jks'
$OutputApk   = Join-Path $ReleaseDir 'hijri-admin.apk'
$AlignedApk  = Join-Path $ReleaseDir 'hijri-admin-aligned.tmp.apk'
$GradlewBat  = Join-Path $ProjectRoot 'gradlew.bat'

$PackageName  = 'net.tawkit.hijriadmin'
$KeyAlias     = 'tawkit'
$StorePass    = $env:TAWKIT_KEYSTORE_PASSWORD
$KeyPass      = $env:TAWKIT_KEYSTORE_PASSWORD

function Write-Info    { param([string]$Msg) Write-Host $Msg -ForegroundColor Cyan }
function Write-Success { param([string]$Msg) Write-Host $Msg -ForegroundColor Green }
function Write-ErrorMsg{ param([string]$Msg) Write-Host $Msg -ForegroundColor Red }

function Get-AndroidSdkDir {
    $localProps = Join-Path $ProjectRoot 'local.properties'
    if (Test-Path $localProps) {
        $line = Get-Content $localProps | Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -First 1
        if ($line) {
            $raw = ($line -split '=', 2)[1].Trim()
            $unescaped = $raw -replace '\\:', ':' -replace '\\\\', '\'
            if (Test-Path $unescaped) { return $unescaped }
        }
    }
    foreach ($envVar in @('ANDROID_HOME', 'ANDROID_SDK_ROOT')) {
        $val = [Environment]::GetEnvironmentVariable($envVar)
        if ($val -and (Test-Path $val)) { return $val }
    }
    throw "Impossible de localiser le SDK Android (verifiez local.properties ou ANDROID_HOME)."
}

function Get-LatestBuildToolsDir {
    param([string]$SdkDir)
    $btRoot = Join-Path $SdkDir 'build-tools'
    if (-not (Test-Path $btRoot)) { throw "Dossier build-tools introuvable sous $SdkDir" }
    $latest = Get-ChildItem $btRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1
    if (-not $latest) { throw "Aucune version de build-tools trouvee sous $btRoot" }
    return $latest.FullName
}

function Get-AdbExe {
    param([string]$SdkDir)
    $adbPath = Join-Path $SdkDir 'platform-tools\adb.exe'
    if (Test-Path $adbPath) { return $adbPath }
    return 'adb'
}

function Show-Help {
    Write-Host ""
    Write-Host "instapk_hijriadmin.ps1 - build / signature / deploiement de Hijri Admin" -ForegroundColor Yellow
    Write-Host "========================================================================"
    Write-Host ""
    Write-Host "Usage :"
    Write-Host "  powershell -ExecutionPolicy Bypass -File release\instapk_hijriadmin.ps1 <action>"
    Write-Host ""
    Write-Host "Actions disponibles :" -ForegroundColor Yellow
    Write-Host "  apk       Compile, aligne et signe -> release\hijri-admin.apk"
    Write-Host "  install   Installe/met a jour sur l'appareil adb courant"
    Write-Host "  help      Affiche ce message"
    Write-Host ""
}

function Invoke-BuildApk {
    if (-not (Test-Path $Keystore)) { throw "Keystore introuvable : $Keystore" }
    if (-not (Test-Path $GradlewBat)) { throw "gradlew.bat introuvable : $GradlewBat" }
    if (-not $StorePass) { throw "Variable d'environnement TAWKIT_KEYSTORE_PASSWORD non definie (mot de passe du keystore tawkit.jks)." }

    $sdkDir        = Get-AndroidSdkDir
    $buildToolsDir = Get-LatestBuildToolsDir -SdkDir $sdkDir
    $zipalign      = Join-Path $buildToolsDir 'zipalign.exe'
    $apksigner     = Join-Path $buildToolsDir 'apksigner.bat'
    if (-not (Test-Path $zipalign))  { throw "zipalign introuvable : $zipalign" }
    if (-not (Test-Path $apksigner)) { throw "apksigner introuvable : $apksigner" }

    Write-Info "==> Compilation release (gradlew :hijri-admin:assembleRelease)..."
    Push-Location $ProjectRoot
    try {
        & $GradlewBat ':hijri-admin:assembleRelease'
        if ($LASTEXITCODE -ne 0) { throw "Echec de gradlew :hijri-admin:assembleRelease (code $LASTEXITCODE)" }
    } finally {
        Pop-Location
    }

    $unsignedApk = Get-ChildItem (Join-Path $ProjectRoot 'hijri-admin\build\outputs\apk\release') `
        -Filter '*-release-unsigned.apk' -File -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
    if (-not $unsignedApk) { throw "APK release non-signe introuvable sous hijri-admin\build\outputs\apk\release" }
    Write-Info "APK non-signe : $unsignedApk"

    if (Test-Path $AlignedApk) { Remove-Item $AlignedApk -Force }
    if (Test-Path $OutputApk)  { Remove-Item $OutputApk -Force }

    Write-Info "==> zipalign..."
    & $zipalign -p -f 4 $unsignedApk $AlignedApk
    if ($LASTEXITCODE -ne 0) { throw "Echec de zipalign (code $LASTEXITCODE)" }

    Write-Info "==> Signature (apksigner, alias '$KeyAlias')..."
    & $apksigner sign `
        --ks $Keystore `
        --ks-key-alias $KeyAlias `
        --ks-pass "pass:$StorePass" `
        --key-pass "pass:$KeyPass" `
        --out $OutputApk `
        $AlignedApk
    if ($LASTEXITCODE -ne 0) { throw "Echec de la signature apksigner (code $LASTEXITCODE)" }

    Remove-Item $AlignedApk -Force -ErrorAction SilentlyContinue

    Write-Info "==> Verification de la signature..."
    & $apksigner verify $OutputApk
    if ($LASTEXITCODE -ne 0) { throw "La verification de signature a echoue (code $LASTEXITCODE)" }

    $size = [math]::Round((Get-Item $OutputApk).Length / 1MB, 1)
    Write-Success "OK : $OutputApk genere et signe ($size Mo)."
}

function Invoke-Install {
    if (-not (Test-Path $OutputApk)) {
        throw "release\hijri-admin.apk introuvable. Lancez d'abord : instapk_hijriadmin.ps1 apk."
    }
    $sdkDir = Get-AndroidSdkDir
    $adb    = Get-AdbExe -SdkDir $sdkDir

    Write-Info "==> Installation (mise a jour) sur l'appareil courant..."
    & $adb install -r $OutputApk
    if ($LASTEXITCODE -ne 0) { throw "Echec de adb install (code $LASTEXITCODE)" }
    Write-Success "OK : installe."
}

$normalizedAction = if ($Action) { $Action.Trim().ToLowerInvariant() } else { '' }

switch ($normalizedAction) {
    'apk'     { Invoke-BuildApk }
    'install' { Invoke-Install }
    'help'    { Show-Help }
    ''        { Show-Help }
    default {
        Write-ErrorMsg "Parametre inconnu : '$Action'"
        Show-Help
        exit 1
    }
}
