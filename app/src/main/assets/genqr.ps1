<#
.SYNOPSIS
    Genere spec/images/qrmosquee.png a partir d'une URL passee en parametre.

.DESCRIPTION
    Ce script appelle l'API qrserver.com pour telecharger le QR code en PNG, puis
    le sauvegarde dans spec/images/qrmosquee.png. La taille par defaut est 220x220,
    legerement superieure a ps.png actuel (182x182).

    A executer depuis le dossier racine du projet :
        powershell -ExecutionPolicy Bypass -File spec\genqr.ps1 -Url "https://example.com"

    Prerequis : connexion internet sur le PC administrateur.
#>

param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Url,

    [int]$Size = 220
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# -- Trouver la racine du projet ---------------------------------------------
$scriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Definition

$outputDir = Join-Path $scriptDir 'images'
$outputPng = Join-Path $outputDir  'qrmosquee.png'

if ([string]::IsNullOrWhiteSpace($Url)) {
    Write-Error "URL vide. Exemple : powershell -ExecutionPolicy Bypass -File spec\genqr.ps1 -Url `"https://example.com`""
    exit 1
}

Write-Host "URL QR : $Url"

# -- Construire l'URL de l'API ------------------------------------------------
Add-Type -AssemblyName System.Web
$encoded = [System.Web.HttpUtility]::UrlEncode($Url.Trim())
$apiUrl  = "https://api.qrserver.com/v1/create-qr-code/?size=${Size}x${Size}&data=$encoded"

Write-Host "Telechargement depuis : $apiUrl"

# -- Telecharger et sauvegarder ----------------------------------------------
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

try {
    Invoke-WebRequest -Uri $apiUrl -OutFile $outputPng -UseBasicParsing
    Write-Host "QR code sauvegarde : $outputPng ($Size x $Size px)"
} catch {
    Write-Error "Echec du telechargement : $_"
    exit 1
}
