#!/usr/bin/env python3
"""Offline self-test for the two-task recognition dataset pipeline.

No TensorFlow, no network — runs anywhere (Pillow only for the functional
augment step). Verifies the contracts that keep training side and app side
in lockstep (docs/ml_roadmap.md):

  1. both label schemas parse, have unique classes and required keys
  2. schema classes map to the app enums parsed out of Fabric.kt
     (fabric task -> FabricCategory, pattern task -> PatternType)
  3. the asset names the app loads (FabricFamilyClassifier.MODEL/LABELS,
     PatternClassifier.MODEL/LABELS) match the names train_finetune.py
     tells humans to ship
  4. train_finetune.py declares both tasks with the right schema/dir/prefix
  5. FUNCTIONAL (Pillow): synthetic pattern photos through augment.py
     --task pattern, into scratch dirs only — never touches a pre-existing
     raw_pattern/ pool

Exit code 0 = all pass. Usage:
  python3 tools/dataset/selftest_pipeline.py
"""
import ast
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent                 # tools/dataset
REPO = HERE.parent.parent                              # NtomaStudio/
APP_ML = REPO / "app/src/main/java/com/ntoma/studio/data/ml"
FABRIC_KT = REPO / "app/src/main/java/com/ntoma/studio/domain/model/Fabric.kt"

FAILURES = []


def check(name: str, ok: bool, detail: str = ""):
    print(f"  [{'PASS' if ok else 'FAIL'}] {name}" + (f" — {detail}" if detail else ""))
    if not ok:
        FAILURES.append(name)


# ------------------------------------------------------------- 1. schemas

def load_schema(name: str) -> dict:
    path = HERE / name
    check(f"{name} exists", path.is_file())
    if not path.is_file():
        return {}
    try:
        schema = json.loads(path.read_text())
        check(f"{name} parses as JSON", True)
    except json.JSONDecodeError as e:
        check(f"{name} parses as JSON", False, str(e))
        return {}
    classes = schema.get("classes", [])
    check(f"{name} has classes", len(classes) > 0)
    check(f"{name} classes unique", len(classes) == len(set(classes)))
    return schema


# ------------------------------------------------- 2. app enum cross-check

def enum_values(kt_source: str, enum_name: str) -> set[str]:
    m = re.search(rf"enum class {enum_name}[^{{]*\{{(.*?)\n\}}", kt_source, re.S)
    if not m:
        return set()
    return set(re.findall(r"^\s*([A-Z][A-Z0-9_]*)\s*\(", m.group(1), re.M))


def check_classes_in_enum(schema: dict, schema_name: str, enum_values_: set[str], enum_name: str):
    if not schema or not enum_values_:
        check(f"{schema_name} vs {enum_name} cross-check", False, "missing inputs")
        return
    unmapped = [c for c in schema.get("classes", []) if c not in enum_values_]
    check(f"all {schema_name} classes map to {enum_name}", not unmapped,
          f"unmapped: {unmapped}" if unmapped else f"{len(schema.get('classes', []))} classes OK")


# ------------------------------------------------ 3. asset-name cross-check

def kotlin_const(path: Path, const: str) -> str | None:
    if not path.is_file():
        return None
    m = re.search(rf'const val {const} = "([^"]+)"', path.read_text())
    return m.group(1) if m else None


def check_assets():
    pairs = [
        ("FabricFamilyClassifier.kt", "fabric_family_classifier.tflite", "fabric_family_labels.txt"),
        ("PatternClassifier.kt", "pattern_classifier.tflite", "pattern_labels.txt"),
    ]
    for kt_name, want_model, want_labels in pairs:
        model = kotlin_const(APP_ML / kt_name, "MODEL")
        labels = kotlin_const(APP_ML / kt_name, "LABELS")
        check(f"{kt_name} MODEL asset", model == want_model, f"found {model!r}")
        check(f"{kt_name} LABELS asset", labels == want_labels, f"found {labels!r}")
    # train_finetune.py must tell humans to ship those exact asset names
    tf_src = (HERE / "train_finetune.py").read_text()
    for want in ("fabric_family_classifier.tflite", "pattern_classifier.tflite"):
        check(f"train_finetune.py documents shipping {want}", want in tf_src)


# --------------------------------------------- 4. train_finetune task table

def check_train_tasks():
    tree = ast.parse((HERE / "train_finetune.py").read_text())
    tasks = None
    for node in ast.walk(tree):
        if isinstance(node, ast.Assign):
            for t in node.targets:
                if isinstance(t, ast.Name) and t.id == "TASKS":
                    tasks = ast.literal_eval(node.value)
    check("train_finetune.py TASKS table found", tasks is not None)
    if not tasks:
        return
    expected = {
        "fabric": ("label_schema.json", "augmented", "fabric"),
        "pattern": ("label_schema_pattern.json", "augmented_pattern", "pattern"),
    }
    check("TASKS declares fabric + pattern", set(tasks) == set(expected),
          f"found {sorted(tasks)}")
    for task, want in expected.items():
        if task in tasks:
            check(f"TASKS[{task}] wiring", tuple(tasks[task]) == want,
                  f"found {tasks[task]}")


# ------------------------------------------- 5. functional augment dry-run

def synth_pattern_images(raw: Path) -> None:
    """Tiny, obviously-different synthetic images per pattern class."""
    from PIL import Image, ImageDraw
    rng_specs = {
        "STRIPED": lambda d, s: [d.rectangle([x, 0, x + s // 10, s], fill=(20, 20, 20) if (x // (s // 5)) % 2 else (200, 160, 40)) for x in range(0, s, s // 5)],
        "CHECKED": lambda d, s: [d.rectangle([x, y, x + s // 4, y + s // 4], fill=(140, 47, 57) if ((x + y) // (s // 4)) % 2 else (250, 245, 235)) for x in range(0, s, s // 4) for y in range(0, s, s // 4)],
        "PLAID": lambda d, s: [d.rectangle([x, 0, x + 2, s], fill=(20, 60, 120)) or d.rectangle([0, x, s, x + 2], fill=(180, 30, 30)) for x in range(0, s, s // 8)],
        "DOTTED": lambda d, s: [d.ellipse([x, y, x + s // 8, y + s // 8], fill=(240, 240, 240)) for x in range(4, s, s // 4) for y in range(4, s, s // 4)],
        "FLORAL": lambda d, s: [d.ellipse([x, y, x + s // 5, y + s // 5], fill=(220, 90, 140)) or d.ellipse([x + s // 12, y + s // 12, x + s // 9, y + s // 9], fill=(250, 210, 90)) for x in range(0, s, s // 3) for y in range(0, s, s // 3)],
        "GEOMETRIC": lambda d, s: [d.polygon([(x, y + s // 5), (x + s // 10, y), (x + s // 5, y + s // 5)], fill=(30, 120, 90)) for x in range(0, s, s // 3) for y in range(0, s, s // 3)],
        "SOLID": lambda d, s: [d.rectangle([0, 0, s, s], fill=(90, 60, 130))],
    }
    for cls, spec in rng_specs.items():
        folder = raw / cls
        folder.mkdir(parents=True, exist_ok=True)
        for i in range(2):
            img = Image.new("RGB", (64, 64), (128, 128, 128))
            spec(ImageDraw.Draw(img), 64)
            img.save(folder / f"synth_{i}.jpg", "JPEG", quality=90)


def functional_augment():
    try:
        from PIL import Image  # noqa: F401
    except ImportError:
        print("  [SKIP] functional augment step (Pillow not installed)")
        return
    raw = HERE / "raw_pattern"
    aug = HERE / "augmented_pattern"
    if raw.exists() and any(raw.iterdir()):
        print("  [SKIP] functional augment step (raw_pattern/ already has real data — never touching it)")
        return
    created_aug = not aug.exists()
    try:
        synth_pattern_images(raw)
        res = subprocess.run(
            [sys.executable, str(HERE / "augment.py"), "--task", "pattern",
             "--per-image", "2", "--size", "64", "--seed", "3"],
            capture_output=True, text=True, timeout=300,
        )
        check("augment.py --task pattern runs clean", res.returncode == 0,
              (res.stderr or res.stdout).strip().splitlines()[-1] if (res.stderr or res.stdout) else "")
        classes = {p.name for p in aug.iterdir() if p.is_dir()} if aug.is_dir() else set()
        check("augmented_pattern/ covers all 7 classes", len(classes) == 7, f"found {sorted(classes)}")
        total = sum(1 for _ in aug.rglob("*.jpg")) if aug.is_dir() else 0
        check("augmented_pattern/ has images", total >= 28, f"{total} images")
    finally:
        # only ever clean up what this test created (guard above guarantees raw was empty)
        shutil.rmtree(raw, ignore_errors=True)
        if created_aug:
            shutil.rmtree(aug, ignore_errors=True)


def main() -> int:
    print("1) label schemas")
    fabric_schema = load_schema("label_schema.json")
    pattern_schema = load_schema("label_schema_pattern.json")

    print("2) schema -> app enum cross-check")
    kt = FABRIC_KT.read_text()
    check_classes_in_enum(fabric_schema, "label_schema.json", enum_values(kt, "FabricCategory"), "FabricCategory")
    check_classes_in_enum(pattern_schema, "label_schema_pattern.json", enum_values(kt, "PatternType"), "PatternType")

    print("3) asset-name cross-check (Kotlin <-> training docs)")
    check_assets()

    print("4) train_finetune.py task table")
    check_train_tasks()

    print("5) functional augment dry-run (synthetic data, scratch dirs)")
    functional_augment()

    failed = len(FAILURES)
    print(f"\n{'ALL CHECKS PASSED' if failed == 0 else f'{failed} CHECK(S) FAILED'}")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
