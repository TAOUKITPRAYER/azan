<#
.SYNOPSIS
    Transfert et synchronisation vers Google Drive via rclone.
.DESCRIPTION
    Utilise rclone pour transferer des fichiers locaux vers Google Drive.
    Necessite une connexion prealablement configuree via driveconnect.ps1.

    Modes :
    - Fichier = "ALL"  : copie toute l'arborescence (ou miroir avec -Miroir)
    - Fichier = <nom>  : transfere toutes les occurrences en preservant les chemins

.PARAMETER RacinePath
    Chemin local de la racine. Ex: C:\ANDROID\livraison
.PARAMETER Fichier
    "ALL" ou nom de fichier. Ex: ALL | mosquee.js | tawkit.apk
.PARAMETER Compte
    Email du compte Google Drive. Ex: tawkit.net@gmail.com
.PARAMETER Force
    Supprime la confirmation avant transfert.
.PARAMETER SimulationSeulement
    Dry-run : affiche ce qui serait transfere sans modifier.
.PARAMETER Miroir
    Avec ALL uniquement : sync miroir (supprime sur GDrive ce qui n'est plus local).
.PARAMETER LogFile
    Chemin du fichier de log rclone. Par defaut : .\rclone_<date>.log
.PARAMETER Retries
    Nombre de tentatives rclone en cas d'erreur reseau (defaut : 3).
.EXAMPLE
    .\pulldriveapk.ps1 C:\ANDROID\livraison ALL tawkit.net@gmail.com
.EXAMPLE
    .\pulldriveapk.ps1 C:\ANDROID\livraison mosquee.js tawkit.net@gmail.com -Force
.EXAMPLE
    .\pulldriveapk.ps1 C:\ANDROID\livraison ALL tawkit.net@gmail.com -SimulationSeulement
.EXAMPLE
    .\pulldriveapk.ps1 C:\ANDROID\livraison ALL tawkit.net@gmail.com -Miroir -Force
.EXAMPLE
    .\pulldriveapk.ps1
    Lancement interactif guide.
#>

[CmdletBinding()]
param(
    [Parameter(Position = 0)] [string]$RacinePath = "",
    [Parameter(Position = 1)] [string]$Fichier    = "",
    [Parameter(Position = 2)] [string]$Compte     = "",

    [switch]$Force,
    [switch]$SimulationSeulement,
    [switch]$Miroir,

    [string]$LogFile  = "",
    [int]   $Retries  = 3,

    [Alias("h")] [switch]$Help
)

# ─────────────────────────────────────────────────────────────
#  Encodage console
# ─────────────────────────────────────────────────────────────
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding           = [System.Text.Encoding]::UTF8

# ─────────────────────────────────────────────────────────────
#  Fonctions utilitaires
# ─────────────────────────────────────────────────────────────
function Write-ColorLog {
    param([string]$Message, [string]$Type = "INFO")
    $ts = Get-Date -Format "HH:mm:ss"
    switch ($Type) {
        "SUCCESS" { Write-Host "[$ts] OK  $Message" -ForegroundColor Green  }
        "ERROR"   { Write-Host "[$ts] !!  $Message" -ForegroundColor Red    }
        "WARNING" { Write-Host "[$ts] /!\ $Message" -ForegroundColor Yellow }
        "INFO"    { Write-Host "[$ts]     $Message" -ForegroundColor Cyan   }
        default   { Write-Host "[$ts]     $Message" -ForegroundColor Gray   }
    }
}

function Get-RemoteNameFromEmail {
    param([string]$Email)
    return $Email -replace '@', '_' -replace '[^a-zA-Z0-9._-]', '_'
}

function Test-RcloneInstallation {
    if (-not (Get-Command "rclone" -ErrorAction SilentlyContinue)) {
        Write-ColorLog "rclone absent du PATH. Installez : winget install Rclone.Rclone" "ERROR"
        return $false
    }
    return $true
}

function Test-RemoteExists {
    param([string]$RemoteName)
    $remotes = rclone listremotes 2>&1
    return $null -ne ($remotes | Where-Object { $_ -ieq "${RemoteName}:" })
}

function Test-GDriveConnection {
    param([string]$RemoteName)
    rclone lsd "${RemoteName}:" --max-depth 1 2>&1 | Out-Null
    return ($LASTEXITCODE -eq 0)
}

function Confirm-OuiNon {
    param([string]$Question, [string]$MotCle = "O")
    do {
        $rep = (Read-Host $Question).Trim().ToUpper()
    } while ($rep -notin @($MotCle, 'N'))
    return ($rep -eq $MotCle)
}

function Show-Aide {
    Write-Host ""
    Write-Host "USAGE" -ForegroundColor Yellow
    Write-Host "  .\pulldriveapk.ps1 [RacinePath] [Fichier] [Compte]"
    Write-Host "                     [-Force] [-SimulationSeulement] [-Miroir]"
    Write-Host "                     [-LogFile <chemin>] [-Retries <n>]"
    Write-Host ""
    Write-Host "PARAMETRES" -ForegroundColor Yellow
    Write-Host "  RacinePath           Dossier local racine"
    Write-Host "  Fichier              ALL ou nom de fichier (toutes occurrences)"
    Write-Host "  Compte               Email Google Drive"
    Write-Host "  -Force               Pas de confirmation"
    Write-Host "  -SimulationSeulement Dry-run (rien n'est modifie)"
    Write-Host "  -Miroir              Sync miroir avec ALL (supprime sur GDrive)"
    Write-Host "  -LogFile             Fichier de log rclone (defaut: .\rclone_<date>.log)"
    Write-Host "  -Retries             Tentatives en cas d'erreur reseau (defaut: 3)"
    Write-Host ""
    Write-Host "PREREQUIS" -ForegroundColor Yellow
    Write-Host "  .\driveconnect.ps1 -Compte <email>"
    Write-Host ""
    Write-Host "NOTE LIENS DE PARTAGE" -ForegroundColor Yellow
    Write-Host "  rclone copy/sync met a jour les fichiers existants (fileId conserve)."
    Write-Host "  Avec -Miroir : un fichier supprime puis re-uploade perd son lien."
    Write-Host ""
}

# ─────────────────────────────────────────────────────────────
#  Aide / lancement sans parametre
# ─────────────────────────────────────────────────────────────
if ($Help -or
    ([string]::IsNullOrWhiteSpace($RacinePath) -and
     [string]::IsNullOrWhiteSpace($Fichier)    -and
     [string]::IsNullOrWhiteSpace($Compte))) {
    Show-Aide
    exit 0
}

# ─────────────────────────────────────────────────────────────
#  Banniere
# ─────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host "      TRANSFERT GOOGLE DRIVE - pulldriveapk.ps1"             -ForegroundColor Magenta
Write-Host "============================================================" -ForegroundColor Magenta
if ($SimulationSeulement) {
    Write-Host "  *** MODE SIMULATION -- aucun fichier ne sera transfere ***" -ForegroundColor DarkYellow
}
Write-Host ""

# ─────────────────────────────────────────────────────────────
#  Collecte interactive
# ─────────────────────────────────────────────────────────────
while ([string]::IsNullOrWhiteSpace($RacinePath)) {
    Write-Host "Chemin local racine (ex: C:\ANDROID\livraison) :" -ForegroundColor Yellow
    $RacinePath = (Read-Host "  RacinePath").Trim()
}

while ([string]::IsNullOrWhiteSpace($Fichier)) {
    Write-Host "Fichier a transferer ou ALL :" -ForegroundColor Yellow
    $Fichier = (Read-Host "  Fichier").Trim()
}

while ($Compte -notmatch '@') {
    Write-Host "Email du compte Google Drive :" -ForegroundColor Yellow
    $Compte = (Read-Host "  Compte").Trim()
}

# ─────────────────────────────────────────────────────────────
#  Validation rclone
# ─────────────────────────────────────────────────────────────
Write-Host ""
if (-not (Test-RcloneInstallation)) { exit 1 }
Write-ColorLog "rclone : $((rclone version 2>&1 | Select-Object -First 1))" "SUCCESS"

# ─────────────────────────────────────────────────────────────
#  Validation chemin local
# ─────────────────────────────────────────────────────────────
try {
    if (-not [System.IO.Path]::IsPathRooted($RacinePath)) {
        $RacinePath = Join-Path (Get-Location) $RacinePath
    }
    $RacinePath = (Resolve-Path $RacinePath -ErrorAction Stop).Path
} catch {
    Write-ColorLog "Chemin introuvable : '$RacinePath'" "ERROR"
    exit 1
}
if (-not (Test-Path $RacinePath -PathType Container)) {
    Write-ColorLog "Pas un dossier : $RacinePath" "ERROR"; exit 1
}
$nomRacine = Split-Path $RacinePath -Leaf
Write-ColorLog "Racine locale : $RacinePath" "SUCCESS"

# ─────────────────────────────────────────────────────────────
#  Remote rclone
# ─────────────────────────────────────────────────────────────
$remoteName  = Get-RemoteNameFromEmail $Compte
$remoteCible = "${remoteName}:${nomRacine}"
Write-ColorLog "Remote rclone : $remoteName  ->  GDrive : $remoteCible" "SUCCESS"

if (-not (Test-RemoteExists -RemoteName $remoteName)) {
    Write-ColorLog "Remote '$remoteName' non configure. Lancez : .\driveconnect.ps1 -Compte $Compte" "ERROR"
    exit 1
}
Write-Host "     Test connectivite..." -ForegroundColor DarkGray
if (-not (Test-GDriveConnection -RemoteName $remoteName)) {
    Write-ColorLog "Connexion Google Drive echouee. Rafraichissez : rclone config reconnect ${remoteName}:" "ERROR"
    exit 1
}
Write-ColorLog "Connectivite Google Drive OK" "SUCCESS"

# ─────────────────────────────────────────────────────────────
#  Fichier de log
# ─────────────────────────────────────────────────────────────
if ([string]::IsNullOrWhiteSpace($LogFile)) {
    $LogFile = Join-Path (Get-Location) ("rclone_" + (Get-Date -Format "yyyyMMdd_HHmmss") + ".log")
}
Write-ColorLog "Log rclone : $LogFile" "INFO"

# ─────────────────────────────────────────────────────────────
#  Recapitulatif et confirmation
# ─────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "------------------------------------------------------------" -ForegroundColor DarkGray
$modeLabel = if ($Fichier -ieq "ALL") {
    if ($Miroir) { "MIROIR (copie + suppressions GDrive)" } else { "COPIE COMPLETE (sans suppression)" }
} else { "FICHIER SPECIFIQUE : $Fichier" }
Write-Host "  Mode    : $modeLabel"   -ForegroundColor Cyan
Write-Host "  Source  : $RacinePath"  -ForegroundColor Cyan
Write-Host "  GDrive  : $remoteCible" -ForegroundColor Cyan
Write-Host "  Compte  : $Compte"      -ForegroundColor Cyan
Write-Host "  Retries : $Retries"     -ForegroundColor Cyan
if ($SimulationSeulement) {
    Write-Host "  [SIMULATION uniquement]" -ForegroundColor DarkYellow
}
Write-Host "------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host ""

if (-not $Force -and -not $SimulationSeulement) {
    if (-not (Confirm-OuiNon "Lancer le transfert ? (O/N)" "O")) {
        Write-Host "Annule." -ForegroundColor Yellow; exit 0
    }
}

# ─────────────────────────────────────────────────────────────
#  Arguments rclone communs
# ─────────────────────────────────────────────────────────────
$argsCommuns = @(
    "--progress",
    "--stats",     "10s",
    "--transfers", "4",
    "--checkers",  "8",
    "--retries",   "$Retries",
    "--log-file",  $LogFile,
    "--log-level", "INFO"
)
if ($SimulationSeulement) { $argsCommuns += "--dry-run" }

# ─────────────────────────────────────────────────────────────
#  Variables de resultat (initialisees inconditionnellement)
# ─────────────────────────────────────────────────────────────
$nbOK = 0 ; $nbEchec = 0 ; $codeRetour = 0

# ─────────────────────────────────────────────────────────────
#  Transfert — mode ALL
# ─────────────────────────────────────────────────────────────
if ($Fichier -ieq "ALL") {

    if ($Miroir) {
        Write-Host ""
        Write-Host "  ATTENTION - MODE MIROIR :" -ForegroundColor Red
        Write-Host "  Les fichiers presents sur GDrive mais absents en local seront SUPPRIMES." -ForegroundColor Red
        Write-Host "  Les fichiers supprimes puis re-uploades perdront leur lien de partage." -ForegroundColor Yellow
        Write-Host ""
        # Confirmation renforcee : tape exactement "OUI"
        if (-not $Force -and -not $SimulationSeulement) {
            if (-not (Confirm-OuiNon "Confirmer la synchronisation miroir ? (OUI/N)" "OUI")) {
                Write-Host "Annule." -ForegroundColor Yellow; exit 0
            }
        }
        Write-ColorLog "rclone sync (miroir)..." "WARNING"
        rclone sync $RacinePath $remoteCible @argsCommuns
    } else {
        Write-ColorLog "rclone copy (sans suppression)..." "INFO"
        rclone copy $RacinePath $remoteCible @argsCommuns
    }
    $codeRetour = $LASTEXITCODE

# ─────────────────────────────────────────────────────────────
#  Transfert — fichier specifique
# ─────────────────────────────────────────────────────────────
} else {

    Write-ColorLog "Recherche de '$Fichier' dans l'arborescence..." "INFO"
    $fichiers = Get-ChildItem -Path $RacinePath -Recurse -Filter $Fichier -File -ErrorAction SilentlyContinue

    if ($fichiers.Count -eq 0) {
        Write-ColorLog "Fichier '$Fichier' introuvable sous : $RacinePath" "ERROR"
        exit 1
    }

    Write-ColorLog "$($fichiers.Count) occurrence(s) trouvee(s) :" "SUCCESS"
    foreach ($f in $fichiers) {
        Write-Host "  - $($f.FullName.Substring($RacinePath.Length).TrimStart('\','/'))" -ForegroundColor Gray
    }
    Write-Host ""

    foreach ($f in $fichiers) {
        $rel     = $f.FullName.Substring($RacinePath.Length).TrimStart('\','/')
        $relDir  = Split-Path $rel -Parent
        $destDir = if ([string]::IsNullOrEmpty($relDir)) {
            $remoteCible
        } else {
            "$remoteCible/$($relDir -replace '\\','/')"
        }

        Write-Host "  --> $rel" -ForegroundColor Gray
        Write-Host "      vers : $destDir" -ForegroundColor DarkGray

        rclone copy $f.FullName $destDir @argsCommuns
        if ($LASTEXITCODE -eq 0) {
            $nbOK++
            Write-Host "      [OK]" -ForegroundColor Green
        } else {
            $nbEchec++
            $codeRetour = 1
            Write-Host "      [!!] echec (code rclone : $LASTEXITCODE)" -ForegroundColor Red
        }
        Write-Host ""
    }
}

# ─────────────────────────────────────────────────────────────
#  Rapport final
# ─────────────────────────────────────────────────────────────
$couleur = if ($codeRetour -eq 0) { "Green" } else { "Red" }
$titre   = if ($codeRetour -eq 0) {
    if ($SimulationSeulement) { "SIMULATION TERMINEE" } else { "TRANSFERT TERMINE" }
} else { "TRANSFERT TERMINE AVEC ERREURS" }

Write-Host ""
Write-Host "============================================================" -ForegroundColor $couleur
Write-Host "  $titre"                                                     -ForegroundColor $couleur
Write-Host "============================================================" -ForegroundColor $couleur
Write-Host ""
Write-Host "  Compte  : $Compte"      -ForegroundColor Cyan
Write-Host "  Source  : $RacinePath"  -ForegroundColor Cyan
Write-Host "  GDrive  : $remoteCible" -ForegroundColor Cyan
Write-Host "  Log     : $LogFile"     -ForegroundColor Cyan

if ($Fichier -ine "ALL") {
    Write-Host "  OK      : $nbOK"    -ForegroundColor Green
    if ($nbEchec -gt 0) {
        Write-Host "  Echecs  : $nbEchec" -ForegroundColor Red
    }
}

if ($SimulationSeulement) {
    Write-Host ""
    Write-Host "  Relancez sans -SimulationSeulement pour effectuer le transfert." -ForegroundColor DarkYellow
}

if ($codeRetour -ne 0) {
    Write-Host ""
    Write-Host "  Pistes :" -ForegroundColor Yellow
    Write-Host "    - Verifiez la connexion Internet"
    Write-Host "    - Consultez le log : $LogFile"
    Write-Host "    - Rafraichissez l'auth : rclone config reconnect ${remoteName}:"
}

Write-Host ""
exit $codeRetour
