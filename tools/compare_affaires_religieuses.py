#!/usr/bin/env python3
"""Compare les horaires de priere calcules localement (data/TN/wtimes-tn.*.js)
avec ceux publies par le site officiel du ministere tunisien des Affaires
Religieuses (https://www.affaires-religieuses.tn/public/ar).

Le site expose un point d'acces JSON non documente mais public, sans
authentification :

    GET /public/getData/{ville_id}   -> horaires du jour pour une ville
    GET /public/getData/0            -> TOUTE la table (42 villes x 365
                                         jours, 15330 lignes) en un seul
                                         appel. Table figee depuis 2020
                                         (created_at/updated_at inchanges
                                         d'une execution a l'autre), donc pas
                                         besoin de sondage quotidien : un seul
                                         telechargement suffit a obtenir
                                         l'annee complete.

Ce script telecharge cette table une fois, filtre sur la ville demandee, la
reformate au format "MM-DD~~~~~HH:MM|..." utilise par nos fichiers
wtimes-tn.*.js, puis affiche les differences jour par jour avec CHAQUE
fichier local correspondant a cette ville (il peut y en avoir plusieurs, ex.
tn.monastir vs tn.monastir_).

IMPORTANT (regle d'or du depot) : data/TN/*.js sont des fichiers CORE, jamais
modifies directement. Ce script ne fait donc QUE LIRE ces fichiers pour la
comparaison. En cas d'ecrasement confirme, la version corrigee est ecrite
sous spec/data/TN/ (surcharge, meme arborescence que le core sous spec/ --
convention du depot), jamais dans data/TN/ -- c'est a l'utilisateur de
decider ensuite, au cas par cas, s'il veut cabler cette surcharge dans le
chargement reel de l'appli (custom.js).

Le site ne fournit que 5 horaires (Fajr/Dohr/Asr/Maghrib/Isha) -- pas de
Chourouk (lever du soleil). La colonne Chourouk du fichier local est
TOUJOURS conservee telle quelle dans la surcharge generee ; seules les 5
autres colonnes sont remplacees par les valeurs du site.

Usage:
    python tools/compare_affaires_religieuses.py --list
    python tools/compare_affaires_religieuses.py --ville 16
    python tools/compare_affaires_religieuses.py -h
    python tools/compare_affaires_religieuses.py /?      (alias Windows de -h)

Affichage arabe (noms de villes) : AUCUN terminal Windows ne rend l'arabe
correctement (ni bidi ni liaison des lettres, testes conhost et Windows
Terminal, limitation confirmee du Unicode Consortium sur les terminaux en
general -- pas quelque chose qu'un script peut corriger de maniere fiable).
Rediriger vers un fichier et l'ouvrir dans un editeur de texte :
    python tools/compare_affaires_religieuses.py --list > tools/city_list.txt
puis ouvrir tools/city_list.txt dans VS Code (ou tout editeur) -- le texte y
est correct.
"""
import argparse
import json
import re
import sys
import urllib.request
from pathlib import Path

# La console Windows utilise par defaut une page de code heritee (cp1252)
# incapable d'afficher l'arabe -- force stdout/stderr en UTF-8 des le
# demarrage, avant tout print() contenant du texte arabe (noms de villes).
for _stream in (sys.stdout, sys.stderr):
    if hasattr(_stream, "reconfigure"):
        _stream.reconfigure(encoding="utf-8")

# Aucun terminal Windows (conhost ni Windows Terminal, verifie 15/08/2026 --
# cf. microsoft/terminal#20302, toujours ouvert) n'affiche l'arabe correctement
# -- ni le sens de lecture (bidi) ni la liaison des lettres. Deux tentatives
# de correctif cote script ont echoue en conditions reelles (rapporte par
# l'utilisateur le 15/08/2026) : formes de presentation Unicode -> plus
# aucune lettre visible (police du terminal incomplete) ; reordonnancement
# seul -> lettres visibles et dans le bon sens mais non liees entre elles.
# Confirme non resoluble proprement par un fil de discussion du Unicode
# Consortium lui-meme (rendu de texte complexe = hors de portee d'un terminal
# "grille de caracteres", cf. lien dans l'historique de conversation). Seule
# methode fiable : rediriger la sortie vers un fichier et l'ouvrir dans un
# editeur de texte (VS Code, Notepad...), qui gerent le bidi correctement --
# cf. --list ci-dessous.
def term(s):
    return s

TOOLS_DIR = Path(__file__).resolve().parent
REPO_ROOT = TOOLS_DIR.parent
DATA_DIR = REPO_ROOT / "app" / "src" / "main" / "assets" / "data" / "TN"
OVERRIDE_DIR = REPO_ROOT / "app" / "src" / "main" / "assets" / "spec" / "data" / "TN"
API_BASE = "https://www.affaires-religieuses.tn/public/getData"

# Catalogue des 42 villes acceptees par le site (id -> nom arabe), genere une
# fois depuis le <select id="GovList"> de https://www.affaires-religieuses.tn/public/ar
# (voir tools/_city_catalog.json -- jamais retape a la main pour eviter tout
# risque de corruption de texte arabe).
with open(TOOLS_DIR / "_city_catalog.json", encoding="utf-8") as f:
    CITY_CATALOG = {int(k): v for k, v in json.load(f).items()}

# Correspondance ville (id cote site) -> slug(s) de fichier(s) locaux
# data/TN/wtimes-tn.<slug>.js a comparer. Table volontairement explicite (les
# libelles arabes du site, avec article defini "ال", ne correspondent pas
# toujours a ceux de tn.js/custom.js) -- a completer au besoin.
LOCAL_FILE_MAP = {
    1: ["tunis", "tunis_"],
    16: ["monastir", "monastir_"],
}

MONTH_MAP_FR = {
    "JANVIER": 1, "FEVRIER": 2, "MARS": 3, "AVRIL": 4, "MAI": 5, "JUIN": 6,
    "JUILLET": 7, "AOUT": 8, "SEPTEMBRE": 9, "OCTOBRE": 10, "NOVEMBRE": 11,
    "DECEMBRE": 12,
}

FIELDS = ["Fajr", "Dohr", "Asr", "Maghrib", "Isha"]

LINE_RE = re.compile(
    r'^"(\d{2}-\d{2})~~~~~(\d{2}:\d{2})\|(\d{2}:\d{2})\|(\d{2}:\d{2})\|'
    r'(\d{2}:\d{2})\|(\d{2}:\d{2})\|(\d{2}:\d{2})",?$'
)


def show_city_list():
    print("\nVilles acceptees (parametre --ville) :\n")
    for vid, name in CITY_CATALOG.items():
        mark = "*" if vid in LOCAL_FILE_MAP else " "
        print(f"  {vid:2d} {mark} {term(name)}")
    print("\n(*) = fichier(s) local/locaux connu(s) pour cette ville, comparaison possible.")
    print("Pour les autres villes, ajouter une entree dans LOCAL_FILE_MAP en tete de script.\n")
    print("Exemple : python tools/compare_affaires_religieuses.py --ville 16\n")


def fetch_full_table():
    req = urllib.request.Request(f"{API_BASE}/0", headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        payload = json.load(resp)
    rows = payload.get("data") or []
    if not rows:
        raise RuntimeError("Reponse vide/inattendue du site.")
    return rows


def format_hm(raw):
    h, m = raw.split(":")
    return f"{int(h):02d}:{int(m):02d}"


def to_minutes(hm):
    h, m = hm.split(":")
    return int(h) * 60 + int(m)


def build_site_by_date(rows, ville_id):
    site_rows = [r for r in rows if r.get("ville_id") == ville_id]
    site_by_date = {}
    for row in site_rows:
        bits = row["date_priere"].split("-", 1)
        if len(bits) != 2:
            continue
        day = int(bits[0])
        month_name = bits[1].strip().upper()
        month = MONTH_MAP_FR.get(month_name)
        if not month:
            continue
        key = f"{month:02d}-{day:02d}"
        site_by_date[key] = {
            "Fajr": format_hm(row["T_fajr"]),
            "Dohr": format_hm(row["T_Dohr"]),
            "Asr": format_hm(row["T_Asr"]),
            "Maghrib": format_hm(row["T_Maghrib"]),
            "Isha": format_hm(row["T_Isha"]),
        }
    return site_by_date, len(site_rows)


def parse_local_file(path):
    lines = path.read_text(encoding="utf-8").splitlines()
    by_date = {}
    for line in lines:
        m = LINE_RE.match(line.strip())
        if not m:
            continue
        date = m.group(1)
        by_date[date] = {
            "Fajr": m.group(2),
            "Shourouk": m.group(3),
            "Dohr": m.group(4),
            "Asr": m.group(5),
            "Maghrib": m.group(6),
            "Isha": m.group(7),
        }
    return lines, by_date


def compare_and_maybe_override(slug, ville_id, site_by_date, no_confirm):
    path = DATA_DIR / f"wtimes-tn.{slug}.js"
    print("\n" + "=" * 70)
    print(f"Fichier local (core, lecture seule) : data/TN/wtimes-tn.{slug}.js")
    print("=" * 70)
    if not path.exists():
        print("  (fichier introuvable, ignore)")
        return

    local_lines, local_by_date = parse_local_file(path)
    print(f"  {len(local_by_date)} jours lus localement.")

    diff_count = 0
    missing_on_site = 0
    field_diff_counts = {f: 0 for f in FIELDS}

    for date, loc in local_by_date.items():
        site = site_by_date.get(date)
        if site is None:
            missing_on_site += 1
            continue
        row_diffs = []
        for field in FIELDS:
            if loc[field] != site[field]:
                delta = to_minutes(site[field]) - to_minutes(loc[field])
                sign = "+" if delta >= 0 else ""
                row_diffs.append(f"{field} local={loc[field]} site={site[field]} ({sign}{delta}min)")
                field_diff_counts[field] += 1
        if row_diffs:
            diff_count += 1
            print(f"  {date}  " + "  |  ".join(row_diffs))

    print()
    compared = len(local_by_date) - missing_on_site
    if diff_count == 0:
        print("  Aucune difference sur les jours communs.")
    else:
        print(f"  {diff_count} jour(s) different(s) sur {compared} compares.")
        rep = "  ".join(f"{f}={field_diff_counts[f]}" for f in FIELDS)
        print(f"  Repartition : {rep}")
    if missing_on_site:
        print(f"  {missing_on_site} jour(s) local/locaux absent(s) de la table du site (ex. 29 fevrier).")

    if diff_count == 0:
        return

    print()
    if no_confirm:
        print("  (--no-confirm : surcharge non generee)")
        return
    answer = input(
        f"Ecrire une surcharge sous spec/data/TN/wtimes-tn.{slug}.js avec les "
        f"valeurs du site (Chourouk local conserve, fichier core INCHANGE) ? (o/N) "
    ).strip().lower()
    if answer not in ("o", "oui", "y", "yes"):
        print("  -> Ignore.")
        return

    OVERRIDE_DIR.mkdir(parents=True, exist_ok=True)
    out_path = OVERRIDE_DIR / f"wtimes-tn.{slug}.js"
    new_lines = []
    for line in local_lines:
        m = LINE_RE.match(line.strip())
        if not m:
            new_lines.append(line)
            continue
        date = m.group(1)
        site = site_by_date.get(date)
        if site is None:
            new_lines.append(line)
            continue
        shourouk = m.group(3)  # toujours conserve, le site n'a pas cette donnee
        new_lines.append(
            f'"{date}~~~~~{site["Fajr"]}|{shourouk}|{site["Dohr"]}|{site["Asr"]}|'
            f'{site["Maghrib"]}|{site["Isha"]}",'
        )
    out_path.write_text("\n".join(new_lines) + "\n", encoding="utf-8")
    print(f"  -> Surcharge ecrite : {out_path.relative_to(REPO_ROOT)}")
    print("     (fichier core data/TN/... non modifie ; a cabler manuellement si retenu)")


def main():
    # Alias Windows classique "/?" -> --help (le module argparse ne le
    # reconnait pas nativement, cf. demande explicite).
    if "/?" in sys.argv[1:]:
        sys.argv = [sys.argv[0], "--help"]

    parser = argparse.ArgumentParser(
        description="Compare les horaires wtimes locaux avec le site du ministere tunisien des Affaires Religieuses.",
        epilog="Alias : /? equivaut a --help.",
    )
    parser.add_argument("--ville", type=int, metavar="ID",
                         help="Identifiant numerique de la ville cote site (1 a 42). Voir --list.")
    parser.add_argument("--list", action="store_true",
                         help="Affiche la liste des 42 villes acceptees par le site puis quitte.")
    parser.add_argument("--no-confirm", action="store_true",
                         help="N'affiche que les differences, ne propose jamais d'ecrire une surcharge.")
    args = parser.parse_args()

    if args.list or not args.ville:
        show_city_list()
        return 0

    if args.ville not in CITY_CATALOG:
        print(f"Ville id={args.ville} inconnue du site.")
        show_city_list()
        return 1
    if args.ville not in LOCAL_FILE_MAP:
        print(f"Aucun fichier local associe a '{term(CITY_CATALOG[args.ville])}' (id={args.ville}) dans LOCAL_FILE_MAP.")
        print("Ajoute une entree dans le script pour pouvoir comparer cette ville.")
        return 1

    print("Telechargement de la table complete du site (id=0)...")
    rows = fetch_full_table()
    print(f"Table recue : {len(rows)} lignes (toutes villes).")

    site_by_date, n_rows = build_site_by_date(rows, args.ville)
    print(f"{n_rows} lignes pour '{term(CITY_CATALOG[args.ville])}' (id={args.ville}).")

    for slug in LOCAL_FILE_MAP[args.ville]:
        compare_and_maybe_override(slug, args.ville, site_by_date, args.no_confirm)

    return 0


if __name__ == "__main__":
    sys.exit(main())
