# ============================================================
#  propagate_smali.ps1
#  Propage MainTAWKIT.smali depuis HIDAYA-KSIBET
#  vers tous les autres dossiers mosquée (32 et 64 bits).
#
#  Usage : .\propagate_smali.ps1
# ============================================================

$ROOT      = "C:\Users\Youssef\Desktop\ANDROID\livraison"
$EXCLUDE   = "SERVEUR|SERVHTTP|HIDAYA-KSIBET"

$SOURCE_32 = "$ROOT\HIDAYA-KSIBET\32\tawkit32\smali_classes2\net\tawkit\MainTAWKIT.smali"
$SOURCE_64 = "$ROOT\HIDAYA-KSIBET\64\android16\smali_classes2\net\tawkit\MainTAWKIT.smali"

$REL_32    = "32\tawkit32\smali_classes2\net\tawkit\MainTAWKIT.smali"
$REL_64    = "64\android16\smali_classes2\net\tawkit\MainTAWKIT.smali"

$ok32 = 0 ; $ok64 = 0 ; $skip32 = 0 ; $skip64 = 0

Write-Host ""
Write-Host "============================================================"
Write-Host "   PROPAGATION MainTAWKIT.smali -> toutes les mosquees"
Write-Host "============================================================"
Write-Host ""

# -- Vérification des sources -----------------------------------------
foreach ($src in @($SOURCE_32, $SOURCE_64)) {
    if (-not (Test-Path $src)) {
        Write-Host "ERREUR : source introuvable :" -ForegroundColor Red
        Write-Host "  $src" -ForegroundColor Red
        exit 1
    }
}
Write-Host "Sources OK"
Write-Host "  32 bits : $SOURCE_32"
Write-Host "  64 bits : $SOURCE_64"
Write-Host ""

# -- Parcours des dossiers mosquée ------------------------------------
$mosquees = Get-ChildItem -Path $ROOT -Directory |
            Where-Object { $_.Name -notmatch $EXCLUDE }

if ($mosquees.Count -eq 0) {
    Write-Host "Aucun dossier mosquee trouve sous $ROOT" -ForegroundColor Yellow
    exit 0
}

foreach ($m in $mosquees) {

    Write-Host ">>> $($m.Name)"

    # 32 bits
    $dest32 = Join-Path $m.FullName $REL_32
    if (Test-Path $dest32) {
        Copy-Item $SOURCE_32 $dest32 -Force
        Write-Host "    [32]  OK  $dest32" -ForegroundColor Green
        $ok32++
    } else {
        Write-Host "    [32]  --  (absent, ignore)" -ForegroundColor DarkGray
        $skip32++
    }

    # 64 bits
    $dest64 = Join-Path $m.FullName $REL_64
    if (Test-Path $dest64) {
        Copy-Item $SOURCE_64 $dest64 -Force
        Write-Host "    [64]  OK  $dest64" -ForegroundColor Green
        $ok64++
    } else {
        Write-Host "    [64]  --  (absent, ignore)" -ForegroundColor DarkGray
        $skip64++
    }

    Write-Host ""
}

# -- Résumé -----------------------------------------------------------
Write-Host "============================================================"
Write-Host "   RESUME"
Write-Host "   32 bits : $ok32 copie(s), $skip32 ignore(s)"
Write-Host "   64 bits : $ok64 copie(s), $skip64 ignore(s)"
Write-Host "============================================================"
Write-Host ""
