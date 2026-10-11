#!/usr/bin/env python3
"""Fail-closed design-group budget audit; never declares recognition release-ready.

Input is separately reviewed annotation JSONL, NOT collector query manifests.
An empty/missing annotation file reports zero approved groups. No automatic labels,
photo duplication, or augmentation can fill a group budget. Evaluation is a separate
mandatory gate even when this budget is met.
"""
from __future__ import annotations

import argparse
from collections import defaultdict
import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent


def audit(rows, policy, task="pattern"):
    labels = policy[f"{task}_labels"]
    known_labels = set(labels)
    allocations = policy["per_supported_class"]
    accepted = []
    held = defaultdict(int)
    for row in rows:
        if row.get("decision") != "approved":
            held["not_approved"] += 1
            continue
        required = ("source_id", "group_id", "reviewer", "rights_evidence", "cloth_region_review")
        if any(not isinstance(row.get(k), str) or not row[k].strip() for k in required):
            held["missing_review_fields"] += 1
            continue
        if not isinstance(row.get("sha256"), str) or not re.fullmatch(r"[a-f0-9]{64}", row["sha256"]):
            held["missing_valid_image_hash"] += 1
            continue
        if row.get("rights_verified") is not True or row.get("labels_verified") is not True:
            held["unverified_rights_or_labels"] += 1
            continue
        if row.get("held_out_for_app_qa") is not False:
            held["app_qa_not_cleared"] += 1
            continue
        if row.get("split") not in {"train", "validation", "test", "ood_test"}:
            held["invalid_split"] += 1
            continue
        approved_labels = row.get(f"reviewed_{task}")
        if not isinstance(approved_labels, list) or any(x not in known_labels for x in approved_labels):
            held["invalid_reviewed_labels"] += 1
            continue
        if row["split"] == "ood_test":
            if approved_labels or row.get("ood_verified") is not True or row.get("target_domain") is not True:
                held["unverified_ood"] += 1
                continue
        elif not approved_labels:
            held["missing_reviewed_labels"] += 1
            continue
        accepted.append(row)

    # Group, source and byte identity crossing splits is a hard failure. Flag
    # conflicting group IDs too: a repeated photo must not masquerade as new designs.
    identities = defaultdict(list)
    for row in accepted:
        for key in ("group_id", "source_id", "sha256"):
            if row.get(key):
                identities[(key, row[key])].append(row)
    excluded_groups = set()
    integrity_errors = []
    for (kind, identity), records in identities.items():
        groups = {r["group_id"] for r in records}
        splits = {r["split"] for r in records}
        if len(splits) > 1 or (kind != "group_id" and len(groups) > 1):
            excluded_groups.update(groups)
            integrity_errors.append({"type": "identity_conflict", "field": kind,
                                     "identity": identity, "groups": sorted(groups), "splits": sorted(splits)})
    usable = [r for r in accepted if r["group_id"] not in excluded_groups]
    counts = {}
    for label in labels:
        matches = [r for r in usable if label in r[f"reviewed_{task}"]]
        class_counts = {}
        for split in ("train", "validation", "test"):
            class_counts[f"{split}_design_groups"] = len({r["group_id"] for r in matches if r["split"] == split})
        for split in ("validation", "test"):
            class_counts[f"target_domain_{split}_design_groups"] = len({r["group_id"] for r in matches
                if r["split"] == split and r.get("target_domain") is True})
        camera_groups = {r["group_id"] for r in matches if r["split"] == "test"
            and r.get("target_domain") is True and r.get("input_origin") == "camera"
            and r.get("view") == "worn_outfit"}
        gallery_groups = {r["group_id"] for r in matches if r["split"] == "test"
            and r.get("target_domain") is True and r.get("input_origin") == "gallery"} - camera_groups
        # Alternate camera/gallery views of a design cannot fill both quotas.
        class_counts["camera_test_design_groups"] = len(camera_groups)
        class_counts["gallery_test_design_groups"] = len(gallery_groups)
        counts[label] = dict(counts=class_counts,
            gaps={k: max(0, target-class_counts[k]) for k, target in allocations.items()})
    ood_count = len({r["group_id"] for r in usable if r["split"] == "ood_test"})
    budget_met = bool(labels) and not integrity_errors and ood_count >= policy["ood_test_groups"] and all(
        not any(v["gaps"].values()) for v in counts.values())
    return dict(task=task, policy_status=policy["status"], candidate_rows_seen=len(rows),
                approved_usable_rows=len(usable), held_rows=dict(held), integrity_errors=integrity_errors,
                class_budgets=counts, ood_test_groups=ood_count,
                ood_test_gap=max(0, policy["ood_test_groups"]-ood_count),
                collection_budget_met=budget_met, release_ready=False,
                reason="Independent model evaluation and device tests still required; counts never authorize release.")


def markdown(report):
    lines = ["# Recognition data budget audit", "", "**Release ready: NO.** " + report["reason"], "",
             "Only explicitly approved, verified design groups count. Query matches do not.", "",
             "| Class | Train groups | Validation groups | Test groups | Base allocation gap |",
             "|---|---:|---:|---:|---:|"]
    for label, budget in report["class_budgets"].items():
        counts, gaps = budget["counts"], budget["gaps"]
        base = [f"{split}_design_groups" for split in ("train", "validation", "test")]
        lines.append(f"| {label} | {counts[base[0]]} | {counts[base[1]]} | {counts[base[2]]} | {sum(gaps[k] for k in base)} |")
    lines += ["", f"Unknown/non-fabric test group gap: **{report['ood_test_gap']}**.",
              f"Integrity conflicts: **{len(report['integrity_errors'])}**. Budget met: **{report['collection_budget_met']}**.",
              "Domain/slice requirements are additional subsets of these allocations; see the JSON report."]
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--annotations", type=Path, help="Reviewed JSONL; omit to audit zero approvals")
    parser.add_argument("--policy", type=Path, default=HERE / "release_targets.json")
    parser.add_argument("--task", choices=("pattern", "family"), default="pattern")
    parser.add_argument("--out", type=Path, default=Path(".datasets/readiness"))
    args = parser.parse_args()
    rows = [json.loads(line) for line in args.annotations.read_text().splitlines() if line.strip()] if args.annotations else []
    result = audit(rows, json.loads(args.policy.read_text()), args.task)
    args.out.mkdir(parents=True, exist_ok=True)
    (args.out / f"{args.task}-readiness.json").write_text(json.dumps(result, indent=2) + "\n")
    text = markdown(result)
    (args.out / f"{args.task}-readiness.md").write_text(text)
    print(text)
    # Successful audit does not mean passing a release gate; inspect release_ready.
    return 1 if result["integrity_errors"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
