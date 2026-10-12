"""Release taxonomy helpers shared by training tools. No TensorFlow dependency.

The release classes are `release_visual_families` in label_schema.json. Legacy raw
folders map onto them through `family_aliases` (WAX and ANKARA -> WAX_ANKARA).
Folders that are not release classes (attributes, candidates, unsupported) are never
staged for training.
"""
import shutil
from pathlib import Path


def load_schema(path):
    import json
    return json.loads(Path(path).read_text())


def release_classes(schema):
    return list(schema["release_visual_families"])


def source_folders(schema, release_class):
    aliases = sorted(a for a, target in schema["family_aliases"].items() if target == release_class)
    return [release_class] + aliases


def stage_release_folders(aug_root, staged_root, schema):
    """Copy augmented images into one folder per release class.

    Returns (counts, missing): images staged per release class, and the classes with none.
    Filenames are prefixed with their source folder so aliases cannot collide.
    """
    aug_root, staged_root = Path(aug_root), Path(staged_root)
    if staged_root.exists():
        shutil.rmtree(staged_root)
    counts, missing = {}, []
    for cls in release_classes(schema):
        n = 0
        for src_name in source_folders(schema, cls):
            src = aug_root / src_name
            if not src.is_dir():
                continue
            for f in sorted(src.iterdir()):
                if f.is_file():
                    dest = staged_root / cls
                    dest.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(f, dest / f"{src_name}__{f.name}")
                    n += 1
        counts[cls] = n
        if n == 0:
            missing.append(cls)
    return counts, missing
