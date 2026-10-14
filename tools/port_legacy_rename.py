#!/usr/bin/env python3
"""Port pre-rename Ntoma Studio file contents to the FabricFit identifiers.

This tool deliberately edits file contents only. It never renames source paths;
move the old module/package directories separately after reviewing the dry run.
"""

from __future__ import annotations

import argparse
import difflib
import os
import sys
from pathlib import Path, PurePosixPath


# Keep this list explicit: replacing the bare word "ntoma" would corrupt Twi
# translations (where it means "cloth") and could break the demo keystore.
# Longer spellings come first to avoid partial replacements.
REPLACEMENTS = (
    ("support@ntoma.app", "support@fabricfit.app"),
    ("Theme.Ntoma.Splash", "Theme.FabricFit.Splash"),
    ("Theme.Ntoma", "Theme.FabricFit"),
    ("com.ntoma.studio", "com.fabricfit.app"),
    ("com/ntoma/studio", "com/fabricfit/app"),
    ("NtomaDatabase", "FabricFitDatabase"),
    ("NtomaNotifier", "FabricFitNotifier"),
    ("NtomaNavHost", "FabricFitNavHost"),
    ("NtomaApp", "FabricFitApp"),
    ("Ntoma Studio", "FabricFit"),
    ("NtomaStudio", "FabricFit"),
    ("ntoma-studio", "fabricfit-app"),
    ("ntoma_icon_512.png", "fabricfit_icon_512.png"),
    ("ntoma.db", "fabricfit.db"),
    ("ntoma_", "fabricfit_"),
)

# Do not recurse into generated output or VCS/tool caches in a worktree.
SKIP_DIRS = {
    ".git",
    ".gradle",
    ".idea",
    ".kotlin",
    ".venv",
    "__pycache__",
    "build",
    "coverage",
    "dist",
    "node_modules",
    "out",
    "target",
    "venv",
}
BINARY_SUFFIXES = {
    ".aar",
    ".aab",
    ".apk",
    ".class",
    ".dex",
    ".gif",
    ".gz",
    ".ico",
    ".jar",
    ".jpeg",
    ".jpg",
    ".jks",
    ".keystore",
    ".onnx",
    ".pdf",
    ".png",
    ".pt",
    ".pth",
    ".so",
    ".tflite",
    ".webp",
    ".zip",
}
MAX_TEXT_FILE_SIZE = 8 * 1024 * 1024


def rewrite_text(text: str) -> tuple[str, list[tuple[str, int]]]:
    """Return renamed text and per-rule replacement counts."""
    counts: list[tuple[str, int]] = []
    for old, new in REPLACEMENTS:
        count = text.count(old)
        if count:
            text = text.replace(old, new)
            counts.append((old, count))
    return text, counts


def is_preserved_document(relative_path: Path) -> bool:
    """Protect docs that intentionally record the old name and rename map."""
    parts = tuple(part.lower() for part in PurePosixPath(relative_path.as_posix()).parts)
    if not parts:
        return False
    if parts[-1] in {"build.md", "readme.md", "handover.md"}:
        return True
    return len(parts) >= 2 and parts[-2:] == ("docs", "brand_positioning.md")


def iter_candidate_files(root: Path):
    """Yield text-file candidates, pruning caches and generated directories."""
    for directory, dirnames, filenames in os.walk(root):
        dirnames[:] = sorted(name for name in dirnames if name not in SKIP_DIRS)
        base = Path(directory)
        for filename in sorted(filenames):
            path = base / filename
            if path.is_symlink() or path.suffix.lower() in BINARY_SUFFIXES:
                continue
            try:
                if path.stat().st_size > MAX_TEXT_FILE_SIZE:
                    continue
            except OSError:
                continue
            yield path


def read_utf8_text(path: Path) -> str | None:
    """Read a UTF-8 text file without normalizing line endings; skip binaries."""
    try:
        data = path.read_bytes()
    except OSError as error:
        print(f"warning: cannot read {path}: {error}", file=sys.stderr)
        return None
    if b"\0" in data:
        return None
    try:
        return data.decode("utf-8")
    except UnicodeDecodeError:
        return None


def run_self_test() -> bool:
    sample = """package com.ntoma.studio

import com.ntoma.studio.ui.Theme.Ntoma.Splash
class NtomaApp
class NtomaDatabase
class NtomaNotifier
class NtomaNavHost
val appName = \"Ntoma Studio\"
val home = \"NtomaStudio/app/build.gradle.kts\"
val colour = R.color.ntoma_primary
val roomDb = \"ntoma.db\"
val icon = \"brand/ntoma_icon_512.png\"
val support = \"support@ntoma.app\"
val legacyRepo = \"ntoma-studio\"
val twi = \"Ankara ntoma ne trɔso\"
val credits = \"The word ntoma means cloth.\"
val alias = \"ntoma\"
val storePassword = \"ntomastudio\"
val keyPassword = \"ntomastudio\"
"""
    rewritten, _ = rewrite_text(sample)

    required = (
        "package com.fabricfit.app",
        "Theme.FabricFit.Splash",
        "class FabricFitApp",
        "class FabricFitDatabase",
        "class FabricFitNotifier",
        "class FabricFitNavHost",
        'val appName = "FabricFit"',
        '"FabricFit/app/build.gradle.kts"',
        "R.color.fabricfit_primary",
        '"fabricfit.db"',
        '"brand/fabricfit_icon_512.png"',
        '"support@fabricfit.app"',
        '"fabricfit-app"',
        '"Ankara ntoma ne trɔso"',
        '"The word ntoma means cloth."',
        'val alias = "ntoma"',
        'val storePassword = "ntomastudio"',
        'val keyPassword = "ntomastudio"',
    )
    missing = [expected for expected in required if expected not in rewritten]
    if missing:
        print("self-test failed; missing expected output:", file=sys.stderr)
        for item in missing:
            print(f"  {item}", file=sys.stderr)
        return False

    protected_docs = (
        Path("BUILD.md"),
        Path("FabricFit/docs/brand_positioning.md"),
        Path("README.md"),
        Path("HANDOVER.md"),
    )
    unprotected = [str(path) for path in protected_docs if not is_preserved_document(path)]
    if unprotected:
        print("self-test failed; rename-history docs are not protected:", file=sys.stderr)
        for item in unprotected:
            print(f"  {item}", file=sys.stderr)
        return False

    print("self-test passed: identifiers are ported while Twi ntoma and keystore credentials survive.")
    return True


def format_counts(counts: list[tuple[str, int]]) -> str:
    return ", ".join(f"{old!r} × {count}" for old, count in counts)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Rewrite legacy Ntoma Studio identifiers in text files. Dry-run by default; "
            "filesystem paths are never renamed."
        )
    )
    parser.add_argument(
        "worktree",
        nargs="?",
        type=Path,
        default=Path.cwd(),
        help="worktree to scan (defaults to the current directory)",
    )
    parser.add_argument("--apply", action="store_true", help="write the proposed content changes")
    parser.add_argument("--self-test", action="store_true", help="check rename and preservation guard rails")
    args = parser.parse_args(argv)

    if args.self_test:
        if args.apply:
            parser.error("--self-test and --apply cannot be used together")
        return 0 if run_self_test() else 1

    root = args.worktree.expanduser().resolve()
    if not root.is_dir():
        print(f"error: worktree is not a directory: {root}", file=sys.stderr)
        return 2

    changes = 0
    skipped_docs = 0
    for path in iter_candidate_files(root):
        relative = path.relative_to(root)
        if relative.as_posix() == "tools/port_legacy_rename.py":
            continue
        if is_preserved_document(relative):
            skipped_docs += 1
            continue
        original = read_utf8_text(path)
        if original is None:
            continue
        rewritten, counts = rewrite_text(original)
        if not counts:
            continue

        changes += 1
        action = "updated" if args.apply else "would update"
        print(f"{action}: {relative} ({format_counts(counts)})")
        if args.apply:
            try:
                path.write_bytes(rewritten.encode("utf-8"))
            except OSError as error:
                print(f"error: cannot write {path}: {error}", file=sys.stderr)
                return 2
        else:
            diff = difflib.unified_diff(
                original.splitlines(keepends=True),
                rewritten.splitlines(keepends=True),
                fromfile=f"a/{relative.as_posix()}",
                tofile=f"b/{relative.as_posix()}",
            )
            sys.stdout.writelines(diff)

    if not changes:
        print("No eligible legacy identifiers found; no files changed.")
    elif args.apply:
        print(f"Applied content changes to {changes} file(s). File paths were not renamed.")
    else:
        print(f"Dry run: {changes} file(s) would change. Re-run with --apply to write them.")
    if skipped_docs:
        print(f"Preserved {skipped_docs} rename-history document(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
