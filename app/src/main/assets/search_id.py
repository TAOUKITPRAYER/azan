#!/usr/bin/env python3
"""
search_id.py — Recherche une expression (ou toutes les entrées d'un CSV) dans des fichiers.

Usage:
    python search_id.py <expression|fichier.csv> [fichier_ou_extension] [options]

Exemples — expression simple :
    python search_id.py v9472863                  # tous les fichiers du répertoire courant
    python search_id.py v9472863 .js              # tous les .js
    python search_id.py v9472863 index.html       # fichier précis
    python search_id.py "v94[0-9]+" .js --regex   # regex

Exemples — fichier CSV :
    python search_id.py mappings.csv              # cherche tous les 'original' du CSV
    python search_id.py mappings.csv .js          # idem mais uniquement dans les .js
    python search_id.py style1_remaining.csv .css # cherche les non-traités dans les css

    Le CSV doit avoir un séparateur ; et une colonne 'original' (+ optionnel 'proposed').
    Si 'proposed' est présent, il est affiché à côté de chaque correspondance.

Options:
    --regex, -r      Traite l'expression comme une regex (ignoré en mode CSV)
    --word, -w       Correspondance mot entier uniquement (word boundary \\b)
    --case, -c       Sensible à la casse (défaut: insensible)
    --context N      Affiche N lignes de contexte avant/après (défaut: 0)
    --recursive      Parcourt les sous-répertoires (défaut: répertoire courant uniquement)
    --encoding <enc> Encodage des fichiers source (défaut: utf-8-sig)
    --no-color       Désactive la coloration ANSI
"""

import re
import sys
import csv
import argparse
from io import StringIO
from pathlib import Path


# ---------------------------------------------------------------------------
# Chargement CSV
# ---------------------------------------------------------------------------

def load_csv_mappings(csv_path: Path, encoding: str = "utf-8-sig") -> dict[str, str]:
    """
    Lit un CSV ; et retourne {original: proposed}.
    Si la colonne 'proposed' est absente, la valeur sera une chaîne vide.
    Ignore les lignes commentaires (#) et les valeurs vides.
    """
    raw = csv_path.read_text(encoding=encoding, errors="replace")
    clean_lines = [l for l in raw.splitlines(keepends=True)
                   if not l.lstrip().startswith("#")]

    reader = csv.DictReader(StringIO("".join(clean_lines)), delimiter=";")

    if reader.fieldnames is None:
        sys.exit(f"Erreur : CSV vide ou illisible : {csv_path}")

    fieldnames_lower = [f.strip().lower() for f in reader.fieldnames]
    if "original" not in fieldnames_lower:
        sys.exit(
            f"Erreur : colonne 'original' introuvable dans {csv_path}\n"
            f"Colonnes trouvées : {list(reader.fieldnames)}"
        )

    orig_col = reader.fieldnames[fieldnames_lower.index("original")]
    prop_col = (reader.fieldnames[fieldnames_lower.index("proposed")]
                if "proposed" in fieldnames_lower else None)

    mappings: dict[str, str] = {}
    for row in reader:
        original = (row.get(orig_col) or "").strip()
        proposed = (row.get(prop_col) or "").strip() if prop_col else ""
        if original:
            mappings[original] = proposed

    return mappings


def is_csv_input(value: str) -> bool:
    """Retourne True si le premier paramètre est un fichier CSV existant."""
    p = Path(value)
    return p.suffix.lower() == ".csv" and p.is_file()


# ---------------------------------------------------------------------------
# Construction du pattern regex
# ---------------------------------------------------------------------------

def build_pattern(terms: list[str], word_boundary: bool, flags: int) -> re.Pattern:
    """
    Construit un pattern combiné qui matche n'importe lequel des termes.
    Trie par longueur décroissante pour éviter les sous-correspondances.
    """
    sorted_terms = sorted(terms, key=len, reverse=True)
    alternation = "|".join(re.escape(t) for t in sorted_terms)
    expr = r"\b(?:" + alternation + r")\b" if word_boundary else "(?:" + alternation + ")"
    return re.compile(expr, flags)


# ---------------------------------------------------------------------------
# Recherche dans un fichier
# ---------------------------------------------------------------------------

def search_in_file(
    filepath: Path,
    pattern: re.Pattern,
    context_lines: int = 0,
    encoding: str = "utf-8-sig",
) -> list[dict]:
    """
    Retourne toutes les correspondances dans filepath.
    Chaque entrée : { line, col, text, match, before, after }
    """
    try:
        lines = filepath.read_text(encoding=encoding, errors="replace").splitlines()
    except Exception as exc:
        print(f"  [!] Impossible de lire {filepath}: {exc}", file=sys.stderr)
        return []

    results = []
    for i, line in enumerate(lines):
        for m in pattern.finditer(line):
            before = lines[max(0, i - context_lines): i] if context_lines else []
            after  = lines[i + 1: i + 1 + context_lines] if context_lines else []
            results.append({
                "line":   i + 1,
                "col":    m.start() + 1,
                "text":   line,
                "match":  m.group(0),
                "before": before,
                "after":  after,
            })
    return results


# ---------------------------------------------------------------------------
# Résolution des fichiers cibles
# ---------------------------------------------------------------------------

def resolve_targets(target: str | None, recursive: bool) -> list[Path]:
    """
    Retourne la liste des fichiers à analyser.
      - None          → tous les fichiers du répertoire courant
      - ".js"         → tous les .js (récursif si --recursive)
      - "index.html"  → ce fichier précis
      - "*.css"       → glob
    """
    cwd = Path(".")

    if target is None:
        glob = "**/*" if recursive else "*"
        return sorted(p for p in cwd.glob(glob) if p.is_file())

    if target.startswith(".") and "*" not in target and "/" not in target and "\\" not in target:
        ext = target.lower()
        glob = f"**/*{ext}" if recursive else f"*{ext}"
        return sorted(p for p in cwd.glob(glob) if p.is_file())

    if "*" in target or "?" in target:
        return sorted(p for p in cwd.glob(target) if p.is_file())

    p = Path(target)
    if p.is_file():
        return [p]

    sys.exit(f"Erreur : fichier introuvable ou extension invalide : {target!r}")


# ---------------------------------------------------------------------------
# Affichage
# ---------------------------------------------------------------------------

RESET  = "\033[0m"
BOLD   = "\033[1m"
RED    = "\033[91m"
CYAN   = "\033[96m"
YELLOW = "\033[93m"
GREEN  = "\033[92m"
GRAY   = "\033[90m"


def supports_color() -> bool:
    return hasattr(sys.stdout, "isatty") and sys.stdout.isatty()


def highlight(line: str, pattern: re.Pattern, use_color: bool) -> str:
    if not use_color:
        return line
    return pattern.sub(lambda m: f"{RED}{BOLD}{m.group(0)}{RESET}", line)


def format_mapping_hint(match_token: str, mappings: dict[str, str], use_color: bool) -> str:
    """Retourne '  →  proposed_name' si la colonne proposed est disponible."""
    proposed = mappings.get(match_token, "")
    if not proposed:
        return ""
    arrow = "  →  "
    if use_color:
        return f"{GRAY}{arrow}{GREEN}{proposed}{RESET}"
    return f"{arrow}{proposed}"


def print_results(
    filepath: Path,
    results: list[dict],
    pattern: re.Pattern,
    mappings: dict[str, str],   # vide {} en mode expression simple
    context_lines: int,
    use_color: bool,
    first_file: bool,
) -> None:
    if not results:
        return

    sep = "─" * 70
    header = (
        f"{CYAN}{BOLD}{filepath}{RESET}  ({len(results)} occurrence(s))"
        if use_color else
        f"{filepath}  ({len(results)} occurrence(s))"
    )

    if not first_file:
        print()
    print(header)
    print(sep)

    prev_line = -999
    for r in results:
        if context_lines and r["line"] > prev_line + context_lines * 2 + 2 and prev_line != -999:
            print(f"  {'·' * 30}")

        for j, ctx in enumerate(r["before"]):
            lno = r["line"] - len(r["before"]) + j
            print(f"  {GRAY}{lno:5d}  {ctx}{RESET}" if use_color else f"  {lno:5d}  {ctx}")

        display = highlight(r["text"], pattern, use_color)
        hint    = format_mapping_hint(r["match"], mappings, use_color)
        lno_str = f"{YELLOW}{r['line']:5d}{RESET}" if use_color else f"{r['line']:5d}"
        print(f"  {lno_str}  {display}{hint}")

        for j, ctx in enumerate(r["after"]):
            lno = r["line"] + 1 + j
            print(f"  {GRAY}{lno:5d}  {ctx}{RESET}" if use_color else f"  {lno:5d}  {ctx}")

        prev_line = r["line"] + len(r["after"])


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Recherche une expression ou un fichier CSV de mappings dans des fichiers source.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument("expression",
        help="Expression à chercher, regex, ou chemin vers un fichier .csv")
    parser.add_argument("target", nargs="?", default=None,
        help="Fichier précis, extension (.js .html .css) ou glob. "
             "Si absent : tous les fichiers du répertoire courant.")
    parser.add_argument("--regex", "-r", action="store_true",
        help="Traite l'expression comme une regex (ignoré si CSV)")
    parser.add_argument("--word", "-w", action="store_true",
        help="Correspondance mot entier (word boundary \\b)")
    parser.add_argument("--case", "-c", action="store_true",
        help="Sensible à la casse (défaut: insensible)")
    parser.add_argument("--context", "-C", type=int, default=0, metavar="N",
        help="Lignes de contexte avant/après chaque résultat (défaut: 0)")
    parser.add_argument("--recursive", action="store_true",
        help="Parcourt les sous-répertoires")
    parser.add_argument("--encoding", default="utf-8-sig",
        help="Encodage des fichiers (défaut: utf-8-sig)")
    parser.add_argument("--no-color", action="store_true",
        help="Désactive la coloration ANSI")
    args = parser.parse_args()

    flags = 0 if args.case else re.IGNORECASE
    use_color = not args.no_color and supports_color()

    # ── Mode CSV ou expression simple ? ──────────────────────────────────────
    mappings: dict[str, str] = {}
    csv_mode = is_csv_input(args.expression)

    if csv_mode:
        csv_path = Path(args.expression)
        mappings = load_csv_mappings(csv_path, encoding=args.encoding)
        if not mappings:
            sys.exit(f"Erreur : aucune entrée 'original' valide dans {csv_path}")

        terms = list(mappings.keys())
        print(f"Mode CSV : {csv_path.name}  —  {len(terms)} expression(s) à rechercher")
        try:
            pattern = build_pattern(terms, word_boundary=args.word, flags=flags)
        except re.error as exc:
            sys.exit(f"Erreur de construction du pattern : {exc}")
    else:
        # Expression simple (texte exact ou regex)
        expr = args.expression if args.regex else re.escape(args.expression)
        if args.word:
            expr = r"\b" + expr + r"\b"
        try:
            pattern = re.compile(expr, flags)
        except re.error as exc:
            sys.exit(f"Erreur dans l'expression régulière : {exc}")

    # ── Résolution des fichiers cibles ────────────────────────────────────────
    # En mode CSV, exclure le fichier CSV lui-même des cibles
    files = resolve_targets(args.target, args.recursive)
    if csv_mode:
        csv_abs = Path(args.expression).resolve()
        files = [f for f in files if f.resolve() != csv_abs]

    if not files:
        sys.exit(f"Aucun fichier trouvé pour la cible : {args.target!r}")

    # ── Recherche ─────────────────────────────────────────────────────────────
    total_files = 0
    total_hits  = 0
    first_file  = True
    # En mode CSV : compter quels originals ont été trouvés
    found_tokens: set[str] = set()

    for filepath in files:
        results = search_in_file(filepath, pattern, args.context, args.encoding)
        if results:
            print_results(filepath, results, pattern, mappings,
                          args.context, use_color, first_file)
            total_files += 1
            total_hits  += len(results)
            first_file   = False
            if csv_mode:
                found_tokens.update(r["match"] for r in results)

    # ── Résumé ────────────────────────────────────────────────────────────────
    print()
    if total_hits == 0:
        label = f"{len(mappings)} expression(s) du CSV" if csv_mode else repr(args.expression)
        print(f"Aucune occurrence trouvée pour {label} dans {len(files)} fichier(s) analysé(s).")
    else:
        print(f"{'═'*70}")
        print(f"Résultat : {total_hits} occurrence(s) dans {total_files} fichier(s) "
              f"(sur {len(files)} analysé(s))")

    if csv_mode:
        not_found_tokens = sorted(set(mappings.keys()) - found_tokens)
        found_count = len(found_tokens)
        print(f"Expressions trouvées : {found_count} / {len(mappings)}")
        if not_found_tokens:
            print(f"Non trouvées ({len(not_found_tokens)}) :")
            for tok in not_found_tokens:
                proposed = mappings[tok]
                suffix   = f"  →  {proposed}" if proposed else ""
                print(f"  {tok}{suffix}")


if __name__ == "__main__":
    main()
