#!/usr/bin/env python3
"""Turn a Gradle build log into GitHub Actions annotations or a PR comment.

CI job logs on a public repository are only reachable through the Actions UI,
which costs an extra click (and a login) for reviewers. This script re-publishes
the useful part of a failed Gradle run as job annotations, so the reason for a
red build is visible straight from the run page, and can render the same
information as Markdown for a pull-request comment.

Usage:
  python3 tools/qa/gradle_error_annotations.py <gradle-log> [max-annotations]
  python3 tools/qa/gradle_error_annotations.py <gradle-log> --markdown [max-chars]
"""
from __future__ import annotations

import re
import sys

# Lines that point at the actual cause of a failure. Order matters: the first
# match wins so a line is not reported twice.
PATTERNS = [
    re.compile(r"^e: "),                                             # Kotlin compiler
    re.compile(r"error: ", re.I),                                    # AAPT / resource merger
    re.compile(r"Could not (find|resolve|determine|download|get)", re.I),
    re.compile(r"^> Task .*FAILED$", re.M),
    re.compile(r"^FAILURE: ", re.M),
]

# Gradle's own summary blocks carry the real message; keep them verbatim.
SECTION_STARTS = ("* What went wrong:", "* Exception is:")
SECTION_LIMIT = 25

# GitHub truncates a single annotation message at ~4 KB.
ANNOTATION_BUDGET = 1400
# A PR comment may be up to 64 KB; stay just under it.
MARKDOWN_BUDGET = 60000


def escape(text: str) -> str:
    """Escape a value for a GitHub Actions workflow command."""
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def collect(path: str) -> list[str]:
    with open(path, encoding="utf-8", errors="replace") as handle:
        lines = [line.rstrip("\n") for line in handle]

    interesting: list[str] = []
    in_section = False
    section_left = 0

    for line in lines:
        stripped = line.strip()
        if any(stripped.startswith(start) for start in SECTION_STARTS):
            in_section = True
            section_left = SECTION_LIMIT
            interesting.append(line)
            continue
        if in_section:
            interesting.append(line)
            section_left -= 1
            # A blank line followed by a new "* " bullet ends the section.
            if section_left <= 0 or (not stripped and section_left < SECTION_LIMIT):
                in_section = False
            continue
        if any(pattern.search(line) for pattern in PATTERNS):
            interesting.append(line)

    # Collapse duplicates while keeping order (Gradle repeats errors per task).
    seen: set[str] = set()
    unique: list[str] = []
    for line in interesting:
        key = line.strip()
        if key and key not in seen:
            seen.add(key)
            unique.append(key)
    return unique


def by_budget(lines: list[str], budget: int, max_chunks: int, dropped_note: str) -> list[str]:
    """Group lines into chunks that fit `budget` characters."""
    if not lines:
        return ["Gradle failed without a recognisable error line. Check the job log."]

    chunks: list[str] = []
    current: list[str] = []
    size = 0
    for line in lines:
        if current and size + len(line) + 1 > budget:
            chunks.append("\n".join(current))
            current, size = [], 0
            if len(chunks) >= max_chunks:
                break
        current.append(line)
        size += len(line) + 1
    if current and len(chunks) < max_chunks:
        chunks.append("\n".join(current))

    emitted = sum(len(chunk.split("\n")) for chunk in chunks)
    if emitted < len(lines):
        chunks.append(dropped_note.format(dropped=len(lines) - emitted))
    return chunks


def markdown(path: str, max_chars: int) -> str:
    lines = collect(path)
    body = "\n".join(lines)
    note = ""
    if len(body) > max_chars:
        body = body[:max_chars]
        note = f"\n\n… truncated after {max_chars} characters."
    return "```\n" + body + "\n```" + note


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2

    if "--markdown" in argv:
        limit = MARKDOWN_BUDGET
        rest = [a for a in argv[2:] if a != "--markdown"]
        if rest and rest[0].isdigit():
            limit = int(rest[0])
        print(markdown(argv[1], limit))
        return 0

    max_annotations = int(argv[2]) if len(argv) > 2 and argv[2].isdigit() else 10
    messages = by_budget(
        collect(argv[1]),
        ANNOTATION_BUDGET,
        max_annotations,
        dropped_note="… {dropped} further error lines omitted.",
    )
    print(f"::group::Gradle failure summary ({len(messages)} annotation(s))")
    for index, message in enumerate(messages, start=1):
        label = f"[{index}/{len(messages)}] " if len(messages) > 1 else ""
        print(f"::error::{escape(label + message)}")
    print("::endgroup::")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
