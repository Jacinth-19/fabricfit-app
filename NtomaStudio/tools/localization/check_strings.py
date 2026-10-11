#!/usr/bin/env python3
"""Validate shipping string IDs/formats and export missing copy for HUMAN review."""
import argparse
import csv
import re
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
LOCALES = ("tw", "gaa", "ee")
# Java Formatter conversion syntax; escaped %% is not an argument.
FORMAT = re.compile(r"%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([a-zA-Z%])")


def placeholders(text):
    result = []
    next_index = 1
    for index, kind in FORMAT.findall(text):
        if kind in ("%", "n"):
            continue
        result.append((int(index) if index else next_index, kind))
        if not index:
            next_index += 1
    return Counter(result)


def strings(path):
    result = {}
    for node in ET.parse(path).getroot():
        if node.tag != "string":
            continue
        key = node.attrib["name"]
        if key in result:
            raise ValueError(f"{path}: duplicate ID {key}")
        result[key] = "".join(node.itertext())
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--export-review", action="store_true")
    args = parser.parse_args()
    base = strings(RES / "values/strings.xml")
    errors = []
    for locale in LOCALES:
        translated = strings(RES / f"values-{locale}/strings.xml")
        for key, text in translated.items():
            if key not in base:
                errors.append(f"{locale}: unknown ID {key}")
            elif placeholders(text) != placeholders(base[key]):
                errors.append(f"{locale}: placeholder mismatch {key}")
        missing = sorted(base.keys() - translated.keys())
        print(f"{locale}: {len(translated)}/{len(base)} strings; {len(missing)} English fallbacks. Native review NOT certified.")
        if args.export_review:
            out = ROOT / "docs/localization" / f"{locale}-review.csv"
            out.parent.mkdir(parents=True, exist_ok=True)
            with out.open("w", newline="", encoding="utf-8") as stream:
                writer = csv.writer(stream)
                writer.writerow(["string_id", "english", "existing_translation_unverified", "reviewed_translation", "reviewer", "review_date", "status"])
                for key in sorted(base):
                    writer.writerow([key, base[key], translated.get(key, ""), "", "", "", "english-fallback" if key in missing else "needs-native-review"])
    if errors:
        raise SystemExit("\n".join(errors))


if __name__ == "__main__":
    main()
