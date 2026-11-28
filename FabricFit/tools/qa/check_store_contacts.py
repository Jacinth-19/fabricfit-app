#!/usr/bin/env python3
"""Fail the build if store or in-app copy advertises a contact address that cannot work.

Play requires a privacy policy contact that a user can actually reach, and the in-app
Help screen tells people where to write. Both previously pointed at
`support@`/`privacy@fabricfit.app`, a domain that is not registered, so every message
sent to them bounced. That is the kind of thing that stays broken for a year because
nothing in the build notices, which is what this script is for.

Two rules:
  1. No address may sit on a domain we do not own (an allowlist, not a blocklist, so a
     new invented domain is caught instead of grandfathered in).
  2. The privacy policy must still contain at least one approved address - deleting the
     contact section is as much a Play violation as advertising a dead mailbox.

Run:  python3 FabricFit/tools/qa/check_store_contacts.py [--root <repo>]
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

# Domains that are ours (or are deliberately reserved for documentation). Anything else
# is treated as a placeholder that will bounce. `example.com` is RFC 2606 reserved; it is
# allowed only in docs that are explicitly about placeholders.
OWNED_DOMAINS = {"gmail.com", "googlemail.com"}
APPROVED_CONTACTS = {"fabricfit.app@gmail.com"}

# Scanned because a user can read them: shipped strings, store assets, public docs.
SCAN_GLOBS = [
    "FabricFit/app/src/main/res/values*/strings.xml",
    "FabricFit/store/privacy_policy.html",
    "FabricFit/store/listing.md",
    "FabricFit/docs/USER_GUIDE.md",
    "FabricFit/docs/PLAY_DATA_SAFETY.md",
    "FabricFit/docs/marketing/*.md",
    "FabricFit/tools/i18n/*.tsv",
    "FabricFit/strings_dump.txt",
]
POLICY = "FabricFit/store/privacy_policy.html"

EMAIL_RE = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".", help="repository root (default: cwd)")
    root = Path(ap.parse_args(argv).root)
    problems: list[str] = []
    seen: dict[str, list[str]] = {}

    for pattern in SCAN_GLOBS:
        for path in sorted(root.glob(pattern)):
            text = path.read_text(encoding="utf-8", errors="replace")
            for lineno, line in enumerate(text.splitlines(), 1):
                for address in EMAIL_RE.findall(line):
                    if address.lower() in APPROVED_CONTACTS:
                        continue
                    domain = address.rsplit("@", 1)[1].lower()
                    if domain not in OWNED_DOMAINS:
                        seen.setdefault(address, []).append(
                            f"{path.relative_to(root)}:{lineno}"
                        )

    for address, places in sorted(seen.items()):
        where = ", ".join(places[:4]) + (f" (+{len(places) - 4} more)" if len(places) > 4 else "")
        problems.append(
            f"{address} is on a domain this project does not own and will bounce - {where}"
        )

    policy = root / POLICY
    if not policy.exists():
        problems.append(f"{POLICY} is missing; Play requires a hosted privacy policy")
    else:
        body = policy.read_text(encoding="utf-8", errors="replace")
        if not any(c in body for c in APPROVED_CONTACTS):
            problems.append(
                f"{POLICY} no longer contains an approved contact address "
                f"({', '.join(sorted(APPROVED_CONTACTS))}); Play requires one"
            )

    if problems:
        print("Store contact check failed:\n", file=sys.stderr)
        for item in problems:
            print(f"  - {item}", file=sys.stderr)
        print(
            "\nUse an address that exists. Repository owners can enable GitHub private email, "
            "or register a domain and add MX records, then update OWNED_DOMAINS/APPROVED_CONTACTS "
            "here.",
            file=sys.stderr,
        )
        return 1

    print(f"Store contact check passed ({', '.join(sorted(APPROVED_CONTACTS))}).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
