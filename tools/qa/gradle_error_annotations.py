#!/usr/bin/env python3
"""Turn a Gradle build log into GitHub Actions error annotations.

CI job logs on a public repository are only reachable through the Actions UI,
which costs an extra click and a login for reviewers. This script re-publishes
the useful part of a failed Gradle run as job annotations, so the reason for a
red build is visible straight from the run page (and from `gh run view`).

Usage:  python3 tools/qa/gradle_error_annotations.py <gradle-log> [max-annotations]
"""
from __future__ import annotations

import re
import sys

# Lines that point at the actual cause of a failure. Order matters: the first
# match wins so a line is not reported twice.
PATTERNS = [
    (re.compile(r"^e: "), "kotlin"),
    (re.compile(r"error: ", re.I), "error"),
    (re.compile(r"^w: .*\[[A-Za-z]+Error\]"), "kotlin"),
    (re.compile(r"Could not (find|resolve|determine|download|get)", re.I), "dependency"),
    (re.compile(r"(FAILED|Execution failed for task)", re.I), "task"),
    (re.compile(r"^(\* )?What went wrong:", re.I), "gradle"),
    (re.compile(r"^(\* )?Exception is:", re.I), "gradle"),
    (re.compile(r"^\s*> ", re.M), "gradle"),
]

# Gradle's own summary blocks carry the real message; keep them verbatim.
SECTION_STARTS = ("* What went wrong:", "* Exception is:")
SECTION_LIMIT = 25


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
        for pattern, _ in PATTERNS:
            if pattern.search(line):
                interesting.append(line)
                break

    # Collapse duplicates while keeping order (Gradle repeats errors per task).
    seen: set[str] = set()
    unique: list[str] = []
    for line in interesting:
        key = line.strip()
        if key and key not in seen:
            seen.add(key)
            unique.append(key)
    return unique


def chunk(lines: list[str], max_chunks: int) -> list[str]:
    if not lines:
        return ["Gradle failed without a recognisable error line. Check the job log."]
    size = max(1, -(-len(lines) // max_chunks))  # ceil division
    groups = [lines[i : i + size] for i in range(0, len(lines), size)]
    if len(groups) > max_chunks:  # rounding guard
        groups = groups[: max_chunks - 1] + [lines[size * (max_chunks - 1) :]]
    return ["\n".join(group) for group in groups]


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    max_annotations = int(argv[2]) if len(argv) > 2 else 8
    messages = chunk(collect(argv[1]), max_annotations)
    print(f"::group::Gradle failure summary ({len(messages)} annotation(s))")
    for index, message in enumerate(messages, start=1):
        label = f"[{index}/{len(messages)}] " if len(messages) > 1 else ""
        print(f"::error::{escape(label + message)}")
    print("::endgroup::")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
