#!/usr/bin/env python3
"""Build an 8-category provenance/coverage audit from reviewed FabricFit rows.

No class is guessed: only explicit, manually reviewed source labels in the mapping
below enter the candidate metadata; categories requiring review are excluded.
Image bytes are not in Git, so hashes/dimensions remain blank until images are
actually downloaded and passed through clean_dataset.py.
"""
from __future__ import annotations
import csv, json, hashlib
from urllib.parse import urlparse
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent
SOURCE = REPO / "FabricFit/tools/dataset/reviewed/accepted-manifest.csv"
OUT = ROOT / "dataset/metadata"
CLASSES = ["KENTE", "ANKARA_WAX_PRINT", "ADINKRA", "BATIK", "TIE_DYE_ADIRE", "LACE", "GHANAIAN_FUGU_SMOCK", "OTHER_UNKNOWN"]
# Deliberately conservative. KENTE_PRINT, GONJA and all ambiguous classes are
# excluded rather than silently conflated. A domain expert may revise decisions.
MAP = {
    "KENTE": "KENTE",
    "ANKARA": "ANKARA_WAX_PRINT",
    "WAX": "ANKARA_WAX_PRINT",
    "ADINKRA": "ADINKRA",
    "BATIK": "BATIK",
    "TIEDYE": "TIE_DYE_ADIRE",
    "LACE": "LACE",
    "FUGU": "GHANAIAN_FUGU_SMOCK",
}
KNOWN_OPEN_LICENSES={"CC0 1.0","Public domain","CC BY 2.0","CC BY 3.0","CC BY 4.0","CC BY-SA 2.0","CC BY-SA 3.0","CC BY-SA 4.0"}
FIELDS = ["id", "file", "category", "original_label", "source_url", "source_website", "source_page", "creator", "license", "date_collected", "width", "height", "sha256", "review_status", "label_note"]

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    with SOURCE.open(newline="", encoding="utf-8") as f:
        source_rows = list(csv.DictReader(f))
    rows=[]
    for i, r in enumerate(source_rows, 1):
        category = MAP.get(r["label"])
        if not category: continue
        rows.append({"id":"sha256:"+hashlib.sha256(r["url"].encode()).hexdigest()[:16],"file":"","category":category,"original_label":r["label"],"source_url":r["url"],"source_website":urlparse(r["url"]).hostname or "","source_page":r["source_page"],"creator":r["creator"],"license":r["license"],"date_collected":"","width":"","height":"","sha256":"","review_status":"visual-review-accepted; 8-class mapping requires domain confirmation","label_note":"Mapped from prior 17-class review; not a Ghanaian textile expert certification"})
    with (OUT/"images.csv").open("w", newline="", encoding="utf-8") as f:
        w=csv.DictWriter(f,fieldnames=FIELDS); w.writeheader(); w.writerows(rows)
    counts=Counter(r["category"] for r in rows)
    report={"candidateRows":len(rows),"downloaded":0,"classes":{c:counts[c] for c in CLASSES},"classTarget":1300,"perClassTarget":162,"sourceDataset":{"id":"mikuns/african-fabric","availability":"NOT VERIFIED (Kaggle host inaccessible in this run)","license":"NOT VERIFIED; excluded until Kaggle metadata/licence can be checked"},"requiredManualWork":["download eligible files and populate SHA-256/dimensions","confirm every mapped label with a Ghanaian textile specialist","confirm open licence and collection date per source","review all images; reject non-fabric, irrelevant, watermarks and uncertain labels","source additional independent photos; currently zero binaries available here"],"trainingAllowed":False,"reason":"No local downloaded image files and severe per-class data shortage; no model training or test result is justified."}
    (OUT/"class_distribution.json").write_text(json.dumps(report,indent=2)+"\n")
    with (OUT/"licenses.csv").open("w",newline="",encoding="utf-8") as f:
        w=csv.DictWriter(f,fieldnames=["id","source_url","source_page","creator","license","date_collected"]); w.writeheader(); w.writerows({k:r[k] for k in ["id","source_url","source_page","creator","license","date_collected"]} for r in rows)
    with (ROOT/"reports/licensing_report.csv").open("w",newline="",encoding="utf-8") as f:
        cols=["id","source_url","source_page","creator","license","date_collected","license_gate","redistribution_status"]
        w=csv.DictWriter(f,fieldnames=cols);w.writeheader()
        for r in rows:w.writerow({**{k:r.get(k,"") for k in cols[:6]},"license_gate":"recognized open-license label; source not re-fetched" if r["license"] in KNOWN_OPEN_LICENSES else "unverified","redistribution_status":"not individually re-fetched; image binary absent"})
    rejected=[]
    for round_name in ("round-1","round-2"):
        index=REPO/f"FabricFit/docs/qa/review-{round_name}/review-index.csv"
        decisions=REPO/f"FabricFit/tools/dataset/reviewed/{round_name}-decisions.csv"
        if not index.exists() or not decisions.exists(): continue
        with index.open(newline="",encoding="utf-8") as f: ix={r["id"]:r for r in csv.DictReader(f)}
        with decisions.open(newline="",encoding="utf-8") as f:
            for d in csv.DictReader(f):
                if d["decision"]!="reject": continue
                item=ix.get(d["id"],{})
                rejected.append({"id":d["id"],"round":round_name,"label":item.get("label",""),"title":item.get("title",""),"license":item.get("license",""),"source_page":item.get("landing_url",""),"reason":d["reason"]})
    with (OUT/"rejected_images.csv").open("w",newline="",encoding="utf-8") as f:
        cols=["id","round","label","title","license","source_page","reason"];w=csv.DictWriter(f,fieldnames=cols);w.writeheader();w.writerows(rejected)
    html=(ROOT/"reports/dataset_report.html")
    html.parent.mkdir(parents=True,exist_ok=True)
    table="".join(f"<tr><td>{c}</td><td>{counts[c]}</td><td>{(counts[c]/1300):.1%}</td></tr>" for c in CLASSES)
    html.write_text("<!doctype html><meta charset='utf-8'><title>FabricFit dataset audit</title><h1>FabricFit Ghana dataset audit</h1><p><strong>Not ready for training:</strong> zero downloaded binaries; 21 accepted legacy review rows only. Kaggle source licence not verified.</p><table><thead><tr><th>Class</th><th>Reviewed candidates</th><th>of 1,300 target</th></tr></thead><tbody>"+table+"</tbody></table><p>Counts are candidates, not locally validated image files. See metadata/images.csv and class_distribution.json.</p>",encoding="utf-8")
    print(json.dumps({**report,"rejectedRows":len(rejected)},indent=2))

if __name__=="__main__": main()
