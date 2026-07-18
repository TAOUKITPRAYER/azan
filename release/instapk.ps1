#requires -version 5.1
<#
    instapk.ps1 — build/signature/deploiement de Tawkit (net.tawkit.mobile)

    Usage :
        powershell -ExecutionPolicy Bypass -File release\instapk.ps1 <action>

    Actions :
        apk         Compile la release (gradlew assembleRelease), aligne
                    (zipalign) et signe (apksigner) avec tawkit.jks
                    -> genere release\taoukit.apk
        install     Installe/met a jour release\taoukit.apk sur l'appareil
                    adb courant (adb install -r : conserve les donnees/config
                    deja presentes sur l'appareil)
        fullinstall Desinstalle net.tawkit.mobile s'il est present (efface
                    les donnees de l'appareil) puis installe release\taoukit.apk
        all         Enchaine apk puis fullinstall
        setversion  <version> : met a jour la version partout en une seule
                    commande (app\build.gradle : versionName + versionCode
                    incremente ; spec\custom.js : CUSTOM_APP_VERSION) -> plus
                    besoin d'editer les fichiers a la main ni de repasser par
                    Claude a chaque changement de version.
        getversion  Affiche la version actuellement configuree (app\build.gradle
                    versionName/versionCode), et signale un ecart eventuel avec
                    spec\custom.js (CUSTOM_APP_VERSION).
        help        Affiche ce message (comportement par defaut si aucun
                    parametre n'est fourni)

    Exemples :
        powershell -ExecutionPolicy Bypass -File release\instapk.ps1 apk
        powershell -ExecutionPolicy Bypass -File release\instapk.ps1 install
        powershell -ExecutionPolicy Bypass -File release\instapk.ps1 fullinstall
        powershell -ExecutionPolicy Bypass -File release\instapk.ps1 all
        powershell -ExecutionPolicy Bypass -File release\instapk.ps1 setversion 11.3A
        powershell -ExecutionPolicy Bypass -File release\instapk.ps1 getversion
#>

param(
    [Parameter(Position = 0)]
    [string]$Action,
    [Parameter(Position = 1)]
    [string]$Version
)

$ErrorActionPreference = 'Stop'

# ── Chemins projet ───────────────────────────────────────────────────────
$ReleaseDir  = $PSScriptRoot
$ProjectRoot = Split-Path $ReleaseDir -Parent
$Keystore    = Join-Path $ProjectRoot 'tawkit.jks'
$OutputApk   = Join-Path $ReleaseDir 'taoukit.apk'
$AlignedApk  = Join-Path $ReleaseDir 'taoukit-aligned.tmp.apk'
$GradlewBat  = Join-Path $ProjectRoot 'gradlew.bat'
$BuildGradle = Join-Path $ProjectRoot 'app\build.gradle'
$CustomJs    = Join-Path $ProjectRoot 'app\src\main\assets\spec\custom.js'

$PackageName  = 'net.tawkit.mobile'
$KeyAlias     = 'tawkit'
$StorePass    = '19770327'
$KeyPass      = '19770327'

function Write-Info    { param([string]$Msg) Write-Host $Msg -ForegroundColor Cyan }
function Write-Success { param([string]$Msg) Write-Host $Msg -ForegroundColor Green }
function Write-ErrorMsg{ param([string]$Msg) Write-Host $Msg -ForegroundColor Red }

# ── Localise le SDK Android (local.properties, sinon variables d'env) ───
function Get-AndroidSdkDir {
    $localProps = Join-Path $ProjectRoot 'local.properties'
    if (Test-Path $localProps) {
        $line = Get-Content $localProps | Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -First 1
        if ($line) {
            $raw = ($line -split '=', 2)[1].Trim()
            # Format Java properties : "\:" -> ":", "\\" -> "\"
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
    return 'adb'  # repli : esperer qu'adb soit dans le PATH
}

function Show-Help {
    Write-Host ""
    Write-Host "instapk.ps1 - build / signature / deploiement de Tawkit" -ForegroundColor Yellow
    Write-Host "=========================================================="
    Write-Host ""
    Write-Host "Usage :"
    Write-Host "  powershell -ExecutionPolicy Bypass -File release\instapk.ps1 <action>"
    Write-Host ""
    Write-Host "Actions disponibles :" -ForegroundColor Yellow
    Write-Host "  apk          Compile la release (gradlew assembleRelease), aligne"
    Write-Host "               (zipalign) et signe (apksigner) avec tawkit.jks"
    Write-Host "               -> genere release\taoukit.apk"
    Write-Host ""
    Write-Host "  install      Installe/met a jour release\taoukit.apk sur l'appareil"
    Write-Host "               adb courant (adb install -r : conserve les donnees deja"
    Write-Host "               presentes sur l'appareil, ex. config mosquee)."
    Write-Host ""
    Write-Host "  fullinstall  Desinstalle net.tawkit.mobile s'il est deja present"
    Write-Host "               (efface les donnees existantes sur l'appareil) puis"
    Write-Host "               installe release\taoukit.apk."
    Write-Host ""
    Write-Host "  all          Enchaine apk puis fullinstall."
    Write-Host ""
    Write-Host "  setversion <version>"
    Write-Host "               Met a jour la version partout en une seule commande :"
    Write-Host "               app\build.gradle (versionName = <version>, versionCode"
    Write-Host "               incremente de 1) ET spec\custom.js (CUSTOM_APP_VERSION,"
    Write-Host "               seule constante qui pilote l'affichage a tous les"
    Write-Host "               endroits : onglet navigateur, ecran principal, 'A propos',"
    Write-Host "               menu lateral). Ex: instapk.ps1 setversion 11.3A"
    Write-Host ""
    Write-Host "  getversion   Affiche la version actuellement configuree (versionName +"
    Write-Host "               versionCode depuis app\build.gradle), et signale un ecart"
    Write-Host "               eventuel avec spec\custom.js (CUSTOM_APP_VERSION)."
    Write-Host ""
    Write-Host "  help         Affiche ce message (comportement par defaut si aucun"
    Write-Host "               parametre n'est fourni)."
    Write-Host ""
    Write-Host "Exemples :" -ForegroundColor Yellow
    Write-Host "  powershell -ExecutionPolicy Bypass -File release\instapk.ps1 apk"
    Write-Host "  powershell -ExecutionPolicy Bypass -File release\instapk.ps1 install"
    Write-Host "  powershell -ExecutionPolicy Bypass -File release\instapk.ps1 fullinstall"
    Write-Host "  powershell -ExecutionPolicy Bypass -File release\instapk.ps1 all"
    Write-Host "  powershell -ExecutionPolicy Bypass -File release\instapk.ps1 setversion 11.3A"
    Write-Host "  powershell -ExecutionPolicy Bypass -File release\instapk.ps1 getversion"
    Write-Host ""
}

function Invoke-BuildApk {
    if (-not (Test-Path $Keystore)) { throw "Keystore introuvable : $Keystore" }
    if (-not (Test-Path $GradlewBat)) { throw "gradlew.bat introuvable : $GradlewBat" }

    $sdkDir        = Get-AndroidSdkDir
    $buildToolsDir = Get-LatestBuildToolsDir -SdkDir $sdkDir
    $zipalign      = Join-Path $buildToolsDir 'zipalign.exe'
    $apksigner     = Join-Path $buildToolsDir 'apksigner.bat'
    if (-not (Test-Path $zipalign))  { throw "zipalign introuvable : $zipalign" }
    if (-not (Test-Path $apksigner)) { throw "apksigner introuvable : $apksigner" }

    Write-Info "==> Compilation release (gradlew assembleRelease)..."
    Push-Location $ProjectRoot
    try {
        & $GradlewBat assembleRelease
        if ($LASTEXITCODE -ne 0) { throw "Echec de gradlew assembleRelease (code $LASTEXITCODE)" }
    } finally {
        Pop-Location
    }

    $unsignedApk = Get-ChildItem (Join-Path $ProjectRoot 'app\build\outputs\apk\release') `
        -Filter '*-release-unsigned.apk' -File -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
    if (-not $unsignedApk) { throw "APK release non-signe introuvable sous app\build\outputs\apk\release" }
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

function Invoke-SetVersion {
    param([string]$NewVersion)

    if (-not $NewVersion) { throw "Usage : instapk.ps1 setversion <version>  (ex: 11.3A)" }
    if (-not (Test-Path $BuildGradle)) { throw "Introuvable : $BuildGradle" }
    if (-not (Test-Path $CustomJs))    { throw "Introuvable : $CustomJs" }

    # ── app\build.gradle : versionName + versionCode incremente ─────────────
    $gradleContent = [System.IO.File]::ReadAllText($BuildGradle)

    $codeMatch = [regex]::Match($gradleContent, 'versionCode\s*=\s*(\d+)')
    if (-not $codeMatch.Success) { throw "versionCode introuvable dans $BuildGradle" }
    $oldCode = [int]$codeMatch.Groups[1].Value
    $newCode = $oldCode + 1

    $nameMatch = [regex]::Match($gradleContent, 'versionName\s*=\s*"([^"]*)"')
    if (-not $nameMatch.Success) { throw "versionName introuvable dans $BuildGradle" }
    $oldName = $nameMatch.Groups[1].Value

    $gradleContent = $gradleContent -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
    $gradleContent = $gradleContent -replace 'versionName\s*=\s*"[^"]*"', "versionName = `"$NewVersion`""
    [System.IO.File]::WriteAllText($BuildGradle, $gradleContent, [System.Text.UTF8Encoding]::new($false))

    # ── spec\custom.js : constante unique CUSTOM_APP_VERSION ────────────────
    # Encodage explicite UTF-8 sans BOM (lecture ET ecriture) : le fichier
    # contient de l'arabe, un BOM ou un mauvais encodage le corromprait.
    $jsContent = [System.IO.File]::ReadAllText($CustomJs, [System.Text.Encoding]::UTF8)
    $jsMatch = [regex]::Match($jsContent, "CUSTOM_APP_VERSION\s*=\s*'([^']*)'")
    if (-not $jsMatch.Success) { throw "CUSTOM_APP_VERSION introuvable dans $CustomJs" }
    $oldJsVersion = $jsMatch.Groups[1].Value
    $jsContent = $jsContent -replace "CUSTOM_APP_VERSION\s*=\s*'[^']*'", "CUSTOM_APP_VERSION = '$NewVersion'"
    [System.IO.File]::WriteAllText($CustomJs, $jsContent, [System.Text.UTF8Encoding]::new($false))

    Write-Success "OK : version mise a jour."
    Write-Info "  app\build.gradle   : versionName '$oldName' -> '$NewVersion', versionCode $oldCode -> $newCode"
    Write-Info "  spec\custom.js     : CUSTOM_APP_VERSION '$oldJsVersion' -> '$NewVersion'"
    Write-Info "Lance ensuite 'instapk.ps1 apk' (ou 'all') pour reconstruire l'APK signe."
}

function Invoke-GetVersion {
    if (-not (Test-Path $BuildGradle)) { throw "Introuvable : $BuildGradle" }

    $gradleContent = [System.IO.File]::ReadAllText($BuildGradle)

    $codeMatch = [regex]::Match($gradleContent, 'versionCode\s*=\s*(\d+)')
    if (-not $codeMatch.Success) { throw "versionCode introuvable dans $BuildGradle" }
    $versionCode = $codeMatch.Groups[1].Value

    $nameMatch = [regex]::Match($gradleContent, 'versionName\s*=\s*"([^"]*)"')
    if (-not $nameMatch.Success) { throw "versionName introuvable dans $BuildGradle" }
    $versionName = $nameMatch.Groups[1].Value

    Write-Success "versionName : $versionName"
    Write-Success "versionCode : $versionCode"

    # Verifie la coherence avec spec\custom.js (CUSTOM_APP_VERSION), que
    # setversion tient normalement synchronisee avec versionName -- utile
    # pour detecter une edition manuelle d'un seul des deux fichiers.
    if (Test-Path $CustomJs) {
        $jsContent = [System.IO.File]::ReadAllText($CustomJs, [System.Text.Encoding]::UTF8)
        $jsMatch = [regex]::Match($jsContent, "CUSTOM_APP_VERSION\s*=\s*'([^']*)'")
        if ($jsMatch.Success) {
            $jsVersion = $jsMatch.Groups[1].Value
            if ($jsVersion -ne $versionName) {
                Write-ErrorMsg "ATTENTION : spec\custom.js CUSTOM_APP_VERSION ('$jsVersion') differe de versionName ('$versionName') -- desynchronise."
            } else {
                Write-Info "spec\custom.js CUSTOM_APP_VERSION : $jsVersion (coherent)"
            }
        }
    }

    # Verifie que release\taoukit.apk (le fichier qui serait installe) a bien
    # ete reconstruit APRES ce changement de version -- setversion ne modifie
    # que les sources, 'apk' doit etre relance pour que l'APK reflete la
    # nouvelle version. Sans ce controle, 'getversion' peut afficher une
    # version que l'APK installable n'a pas encore (piege verifie en pratique :
    # version bumpee puis 'install' relance sans 'apk' entre les deux).
    if (Test-Path $OutputApk) {
        try {
            $sdkDir        = Get-AndroidSdkDir
            $buildToolsDir = Get-LatestBuildToolsDir -SdkDir $sdkDir
            $aapt          = Join-Path $buildToolsDir 'aapt.exe'
            if (Test-Path $aapt) {
                $badging = & $aapt dump badging $OutputApk 2>$null
                $apkMatch = [regex]::Match(($badging -join "`n"), "versionName='([^']*)'")
                if ($apkMatch.Success) {
                    $apkVersion = $apkMatch.Groups[1].Value
                    if ($apkVersion -ne $versionName) {
                        Write-ErrorMsg "ATTENTION : release\taoukit.apk contient encore la version '$apkVersion' (source configuree : '$versionName') -- l'APK n'a pas ete reconstruit depuis ce changement. Lancez 'instapk.ps1 apk' avant d'installer."
                    } else {
                        Write-Info "release\taoukit.apk : $apkVersion (a jour, correspond a la source)"
                    }
                }
            }
        } catch {
            # Best-effort : ne bloque pas getversion si aapt/SDK est introuvable.
        }
    } else {
        Write-Info "release\taoukit.apk : absent (jamais construit) -- lancez 'instapk.ps1 apk'."
    }

    return $versionName
}

function Invoke-Install {
    if (-not (Test-Path $OutputApk)) {
        throw "release\taoukit.apk introuvable. Lancez d'abord : instapk.ps1 apk (ou 'all')."
    }
    $sdkDir = Get-AndroidSdkDir
    $adb    = Get-AdbExe -SdkDir $sdkDir

    Write-Info "==> Installation (mise a jour) sur l'appareil courant..."
    & $adb install -r $OutputApk
    if ($LASTEXITCODE -ne 0) { throw "Echec de adb install (code $LASTEXITCODE)" }
    Write-Success "OK : mise a jour installee."
}

function Invoke-FullInstall {
    if (-not (Test-Path $OutputApk)) {
        throw "release\taoukit.apk introuvable. Lancez d'abord : instapk.ps1 apk (ou 'all')."
    }
    $sdkDir = Get-AndroidSdkDir
    $adb    = Get-AdbExe -SdkDir $sdkDir

    Write-Info "==> Desinstallation de $PackageName si present (efface les donnees existantes)..."
    & $adb uninstall $PackageName | Out-Null

    Write-Info "==> Installation propre sur l'appareil courant..."
    & $adb install $OutputApk
    if ($LASTEXITCODE -ne 0) { throw "Echec de adb install (code $LASTEXITCODE)" }
    Write-Success "OK : installation propre terminee."
}

# ── Point d'entree ────────────────────────────────────────────────────────
$normalizedAction = if ($Action) { $Action.Trim().ToLowerInvariant() } else { '' }

switch ($normalizedAction) {
    'apk'         { Invoke-BuildApk }
    'install'     { Invoke-Install }
    'fullinstall' { Invoke-FullInstall }
    'all'         { Invoke-BuildApk; Invoke-FullInstall }
    'setversion'  { Invoke-SetVersion -NewVersion $Version }
    'getversion'  { Invoke-GetVersion | Out-Null }
    'help'        { Show-Help }
    ''            { Show-Help }
    default {
        Write-ErrorMsg "Parametre inconnu : '$Action'"
        Show-Help
        exit 1
    }
}
