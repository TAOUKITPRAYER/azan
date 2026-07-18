#!/usr/bin/env python3
"""
rename_ids.py — Replace obfuscated identifiers in a source file using a CSV mapping.

Usage:
    python rename_ids.py <mapping.csv> [<source_file>] [options]

Positional arguments:
    mapping.csv          CSV file containing the identifier mappings (required)
    source_file          File to process. If omitted, runs batch mode on the
                         default file list (index.html, m1prime.js, …)

Options:
    --output, -o <file>  Output file (overwrites source in-place if omitted)
                         Ignored in batch mode.
    --dry-run            Show stats without writing anything
    --verbose, -v        Print every replaced identifier with its count
    --encoding <enc>     File encoding (default: utf-8-sig, handles BOM)
    --obfus-pattern <rx> Regex to detect remaining obfuscated tokens after replacement
                         Default: r'\\b[a-z]{1,3}\\d{5,}\\b'
                         Examples: r'\\bv\\d{7}\\b'  r'\\bjc\\d{4,}\\b'
    --no-scan            Skip the post-replacement scan for leftover obfuscated tokens

CSV format accepted:
    - Delimiter: auto-detected (comma or semicolon)
    - Required columns (case-insensitive, either name accepted):
        key column  : "original"  or "objet"
        value column: "proposed"  or "correspondance"
    - Optional column: description / any other column (ignored)
    - Lines starting with # are treated as comments and skipped
    - Empty key/value values are skipped
"""

import re
import sys
import csv
import argparse
from io import StringIO
from pathlib import Path


# ---------------------------------------------------------------------------
# CSV loading
# ---------------------------------------------------------------------------

# Accepted column names for the key and value columns (case-insensitive)
_KEY_ALIASES   = {"original", "objet"}
_VALUE_ALIASES = {"proposed", "correspondance"}


def load_mappings(csv_path: Path, encoding: str = "utf-8-sig") -> dict[str, str]:
    """
    Parse the CSV and return {original: proposed}.

    Handles:
    - Auto-detected delimiter (comma or semicolon via csv.Sniffer)
    - Column names: "original"/"objet" → key, "proposed"/"correspondance" → value
    - Comment lines (start with #)
    - Trailing empty columns
    - Duplicate originals (last one wins — consistent with override logic)
    - Rows where original == proposed (skipped — no-op replacement)
    """
    raw = csv_path.read_text(encoding=encoding)

    # Strip comment lines before feeding to csv.DictReader
    clean_lines = [
        line for line in raw.splitlines(keepends=True)
        if not line.lstrip().startswith("#")
    ]
    clean_text = "".join(clean_lines)

    # Auto-detect delimiter from the first non-empty line
    first_line = next((l for l in clean_lines if l.strip()), "")
    try:
        dialect = csv.Sniffer().sniff(first_line, delimiters=",;")
        delimiter = dialect.delimiter
    except csv.Error:
        delimiter = ","  # fallback

    reader = csv.DictReader(StringIO(clean_text), delimiter=delimiter)

    # Normalise column names: strip whitespace, lower-case for lookup
    if reader.fieldnames is None:
        raise ValueError(f"CSV file appears empty: {csv_path}")

    fieldnames_lower = [f.strip().lower() for f in reader.fieldnames]

    orig_idx = next((i for i, f in enumerate(fieldnames_lower) if f in _KEY_ALIASES), None)
    prop_idx = next((i for i, f in enumerate(fieldnames_lower) if f in _VALUE_ALIASES), None)

    if orig_idx is None or prop_idx is None:
        raise ValueError(
            f"CSV must have a key column ({'/'.join(sorted(_KEY_ALIASES))}) "
            f"and a value column ({'/'.join(sorted(_VALUE_ALIASES))}).\n"
            f"Found: {reader.fieldnames}\nin: {csv_path}"
        )

    orig_col = reader.fieldnames[orig_idx]
    prop_col = reader.fieldnames[prop_idx]

    mappings: dict[str, str] = {}
    for row in reader:
        original = (row.get(orig_col) or "").strip()
        proposed = (row.get(prop_col) or "").strip()
        if not original or not proposed:
            continue
        if original == proposed:
            continue
        mappings[original] = proposed

    return mappings


# ---------------------------------------------------------------------------
# Single-pass replacement engine
# ---------------------------------------------------------------------------

def build_pattern(keys: list[str]) -> re.Pattern:
    """
    Build a single regex that matches any of the keys as whole words.
    Keys are sorted longest-first so that e.g. 'fooBar' wins over 'foo'
    when both appear in the mapping (though with \b this rarely matters).
    """
    # Sort by descending length as a safety measure
    sorted_keys = sorted(keys, key=len, reverse=True)
    alternation = "|".join(re.escape(k) for k in sorted_keys)
    return re.compile(r"\b(?:" + alternation + r")\b")


def apply_replacements(
    content: str,
    mappings: dict[str, str],
    verbose: bool = False,
) -> tuple[str, dict[str, int]]:
    """
    Replace all mapped identifiers in *content* in a single regex pass.
    Returns (new_content, {original: count_of_replacements}).
    """
    if not mappings:
        return content, {}

    pattern = build_pattern(list(mappings.keys()))
    counts: dict[str, int] = {}

    def replacer(m: re.Match) -> str:
        token = m.group(0)
        counts[token] = counts.get(token, 0) + 1
        return mappings[token]

    new_content = pattern.sub(replacer, content)

    if verbose:
        for orig in sorted(counts, key=lambda k: -counts[k]):
            print(f"  {orig:30s} → {mappings[orig]:30s}  ({counts[orig]}×)")

    return new_content, counts


# ---------------------------------------------------------------------------
# Post-replacement scan for leftover obfuscated tokens
# ---------------------------------------------------------------------------

# Default pattern: 1-3 lowercase letters followed by 5+ digits
# Catches: v9472863, jc12345, ab99999, etc.
DEFAULT_OBFUS_PATTERN = r'\b[a-z]{1,3}\d{5,}\b'


def scan_unresolved(
    content: str,
    pattern: str = DEFAULT_OBFUS_PATTERN,
    known_proposed: set[str] | None = None,
) -> list[dict]:
    """
    Scan *content* for tokens that still look like obfuscated identifiers.

    Returns a list of dicts:
        { 'line': int, 'token': str, 'context': str }

    Parameters
    ----------
    content         : text to scan (after replacements have been applied)
    pattern         : regex identifying obfuscated tokens
    known_proposed  : set of 'proposed' names from the CSV — tokens that match
                      the obfus pattern but are actually valid proposed names
                      are excluded from the results.
    """
    try:
        rx = re.compile(pattern)
    except re.error as exc:
        raise ValueError(f"Invalid --obfus-pattern regex: {exc}") from exc

    known_proposed = known_proposed or set()
    results: list[dict] = []
    seen: set[tuple[int, str]] = set()   # (line_no, token) dedup

    for line_no, line in enumerate(content.splitlines(), start=1):
        for m in rx.finditer(line):
            token = m.group(0)
            if token in known_proposed:
                continue
            key = (line_no, token)
            if key in seen:
                continue
            seen.add(key)
            # Trim context line to 120 chars, mark the token with >><<
            ctx = line.strip()
            if len(ctx) > 120:
                start = max(0, m.start() - 40)
                ctx = ("…" if start > 0 else "") + line[start:start + 120].strip() + "…"
            results.append({"line": line_no, "token": token, "context": ctx})

    return results


def write_unresolved_csv(results: list[dict], dest: Path) -> None:
    """Write scan results to a CSV file."""
    with dest.open("w", encoding="utf-8", newline="") as f:
        f.write("line;token;context\n")
        for r in results:
            # Escape semicolons in context
            ctx = r["context"].replace(";", "·")
            f.write(f"{r['line']};{r['token']};{ctx}\n")


# ---------------------------------------------------------------------------
# Fichiers traités en mode batch (aucun paramètre fourni)
# ---------------------------------------------------------------------------

BATCH_FILES = [
    "index.html",
    "m1prime.js",
    "m2body.js",
    "style0.css",
    "style1.css",
    "style2.css",
]


# ---------------------------------------------------------------------------
# Traitement d'un fichier unique (cœur de la logique)
# ---------------------------------------------------------------------------

def process_file(source_path: Path, csv_path: Path, args) -> dict:
    """
    Traite un fichier source unique.
    Retourne un dict de statistiques :
      { ok, skipped, replaced_ids, total_hits, unchanged_ids,
        remaining, unresolved_count, error }
    """
    stats = dict(ok=False, skipped=False, replaced_ids=0, total_hits=0,
                 unchanged_ids=0, remaining=0, unresolved_count=0, error=None)

    # ── Load mappings ────────────────────────────────────────────────────────
    try:
        mappings = load_mappings(csv_path, encoding=args.encoding)
    except Exception as exc:
        stats["error"] = f"Error reading CSV: {exc}"
        return stats

    if not mappings:
        stats["error"] = "CSV contains no valid original→proposed pairs."
        return stats

    print(f"  Mappings     : {len(mappings)} identifier(s) to replace")

    # ── Read source ──────────────────────────────────────────────────────────
    try:
        content = source_path.read_text(encoding=args.encoding)
    except UnicodeDecodeError:
        print(f"  Warning: UTF-8 decode failed, retrying with latin-1")
        content = source_path.read_text(encoding="latin-1")

    # ── Apply replacements ────────────────────────────────────────────────────
    new_content, counts = apply_replacements(content, mappings, verbose=args.verbose)

    replaced_ids  = len(counts)
    total_hits    = sum(counts.values())
    unchanged_ids = len(mappings) - replaced_ids

    stats["replaced_ids"]  = replaced_ids
    stats["total_hits"]    = total_hits
    stats["unchanged_ids"] = unchanged_ids

    print(f"  Identifiers found & replaced : {replaced_ids} / {len(mappings)}")
    print(f"  Total occurrences replaced   : {total_hits}")
    if unchanged_ids:
        print(f"  Identifiers not found in file: {unchanged_ids}")

    # ── Write remaining CSV ───────────────────────────────────────────────────
    not_found = {orig: prop for orig, prop in mappings.items() if orig not in counts}
    stats["remaining"] = len(not_found)
    if not_found and not args.dry_run:
        remaining_path = source_path.with_name(source_path.stem + "_remaining.csv")
        with remaining_path.open("w", encoding="utf-8", newline="") as f:
            f.write("original;proposed;\n")
            for orig, prop in not_found.items():
                f.write(f"{orig};{prop};\n")
        print(f"  Remaining CSV: {remaining_path.name}  ({len(not_found)} entry/entries)")

    # ── Scan for leftover obfuscated tokens ───────────────────────────────────
    if not args.no_scan:
        known_proposed = set(mappings.values())
        try:
            unresolved = scan_unresolved(new_content, args.obfus_pattern, known_proposed)
        except ValueError as exc:
            print(f"  Warning: {exc} — scan skipped.")
            unresolved = []

        unique_tokens = sorted({r["token"] for r in unresolved})
        stats["unresolved_count"] = len(unique_tokens)
        if unresolved:
            print(f"  {'─'*56}")
            print(f"  ⚠  Unresolved tokens still present: {len(unique_tokens)} unique / {len(unresolved)} occurrence(s)")
            for tok in unique_tokens:
                hits = [r for r in unresolved if r["token"] == tok]
                lines_str = ", ".join(str(r["line"]) for r in hits[:5])
                if len(hits) > 5:
                    lines_str += f" … (+{len(hits)-5} more)"
                print(f"     {tok:28s}  line(s): {lines_str}")
            if not args.dry_run:
                unresolved_path = source_path.with_name(source_path.stem + "_unresolved.csv")
                write_unresolved_csv(unresolved, unresolved_path)
                print(f"  Unresolved CSV : {unresolved_path.name}")
            print(f"  {'─'*56}")
        else:
            print("  Scan OK — no obfuscated tokens remaining.")

    # ── Write output ─────────────────────────────────────────────────────────
    if new_content == content:
        print("  No changes — file already up to date or no identifiers matched.")
        stats["skipped"] = True
        stats["ok"] = True
        return stats

    if args.dry_run:
        print("  Dry run — no file written.")
        stats["ok"] = True
        return stats

    output_path = Path(args.output).resolve() if getattr(args, "output", None) else source_path
    output_path.parent.mkdir(parents=True, exist_ok=True)
    write_enc = "utf-8-sig" if content.startswith("\ufeff") else "utf-8"
    output_path.write_text(new_content, encoding=write_enc)

    label = "File updated" if output_path == source_path else "Output written"
    print(f"  {label} : {output_path}")
    stats["ok"] = True
    return stats


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Replace obfuscated identifiers in a source file using a CSV mapping.\n"
            "With only the CSV argument, processes the default batch: "
            + ", ".join(BATCH_FILES)
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument("csv_file", metavar="mapping.csv",
        help="CSV file containing the identifier mappings (required)")
    parser.add_argument("source", nargs="?", default=None,
        help="Source file to process. If omitted, runs batch mode on: "
             + ", ".join(BATCH_FILES))
    parser.add_argument("--output", "-o", metavar="FILE",
        help="Output path (overwrites source if omitted — ignored in batch mode)")
    parser.add_argument("--dry-run",  action="store_true", help="Report stats without writing any file")
    parser.add_argument("--verbose",  "-v", action="store_true", help="List every replaced identifier")
    parser.add_argument("--encoding", default="utf-8-sig", help="File encoding (default: utf-8-sig)")
    parser.add_argument(
        "--obfus-pattern", default=DEFAULT_OBFUS_PATTERN, metavar="REGEX",
        help=f"Regex to detect remaining obfuscated tokens (default: {DEFAULT_OBFUS_PATTERN!r})",
    )
    parser.add_argument(
        "--no-scan", action="store_true",
        help="Skip the post-replacement scan for leftover obfuscated tokens",
    )
    args = parser.parse_args()

    # ── Resolve & validate CSV ────────────────────────────────────────────────
    csv_path = Path(args.csv_file).resolve()
    if not csv_path.is_file():
        sys.exit(f"Error: CSV file not found: {csv_path}")

    # ── Batch mode : aucun paramètre source ──────────────────────────────────
    if args.source is None:
        if args.output:
            sys.exit("Error: --output cannot be used in batch mode (no single source file).")

        print("=" * 62)
        print("  BATCH MODE")
        print(f"  CSV   : {csv_path.name}")
        print(f"  Files : {', '.join(BATCH_FILES)}")
        print("=" * 62)

        batch_stats = []
        for filename in BATCH_FILES:
            source_path = Path(filename).resolve()
            print(f"\n▶  {filename}")
            print(f"  {'─'*56}")
            if not source_path.is_file():
                print(f"  ⚠  File not found — skipped.")
                batch_stats.append({"file": filename, "error": "file not found"})
                continue
            s = process_file(source_path, csv_path, args)
            s["file"] = filename
            batch_stats.append(s)

        # ── Résumé global ─────────────────────────────────────────────────
        print("\n" + "=" * 62)
        print("  BATCH SUMMARY")
        print("=" * 62)
        total_replaced = sum(s.get("total_hits", 0)    for s in batch_stats)
        total_unresol  = sum(s.get("unresolved_count", 0) for s in batch_stats)
        total_remain   = sum(s.get("remaining", 0)     for s in batch_stats)
        errors         = [s for s in batch_stats if s.get("error")]
        skipped        = [s for s in batch_stats if s.get("skipped") and not s.get("error")]
        updated        = [s for s in batch_stats if s.get("ok") and not s.get("skipped") and not s.get("error")]

        for s in batch_stats:
            fname = s["file"]
            if s.get("error"):
                status = f"✗  ERROR   — {s['error']}"
            elif s.get("skipped"):
                status = "–  skipped  (no changes)"
            else:
                status = (f"✔  updated  "
                          f"({s['total_hits']} replacements"
                          f"{', ⚠ ' + str(s['unresolved_count']) + ' unresolved' if s['unresolved_count'] else ''}"
                          f")")
            print(f"  {fname:<18}  {status}")

        print(f"\n  Files updated      : {len(updated)}")
        print(f"  Files skipped      : {len(skipped)}")
        print(f"  Errors             : {len(errors)}")
        print(f"  Total replacements : {total_replaced}")
        if total_remain:
            print(f"  Total remaining    : {total_remain}  (see *_remaining.csv)")
        if total_unresol:
            print(f"  Total unresolved   : {total_unresol}  (see *_unresolved.csv)")
        print("=" * 62)
        return

    # ── Single file mode ─────────────────────────────────────────────────────
    source_path = Path(args.source).resolve()
    if not source_path.is_file():
        sys.exit(f"Error: source file not found: {source_path}")

    print(f"▶  {source_path.name}")
    print("─" * 60)
    stats = process_file(source_path, csv_path, args)
    if stats.get("error"):
        sys.exit(f"Error: {stats['error']}")


if __name__ == "__main__":
    main()
