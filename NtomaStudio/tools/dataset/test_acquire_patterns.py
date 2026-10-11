"""Offline tests: python3 -m unittest discover -s NtomaStudio/tools/dataset -p 'test_acquire_patterns.py'."""
import io
import json
import tarfile
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import acquire_patterns as pipeline


class AcquisitionTests(unittest.TestCase):
    def test_rights_provenance_labels_and_missing_images_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            licenses = [("pd", "http://creativecommons.org/publicdomain/mark/1.0/"),
                        ("zero", "http://creativecommons.org/publicdomain/zero/1.0/"),
                        ("by", "http://creativecommons.org/licenses/by/4.0/"),
                        ("nc", "http://creativecommons.org/licenses/by-nc-nd/4.0/")]
            images = [dict(id=str(i), file_name=f"{i}.jpg", license=lic,
                           landing_page=f"https://example.test/{i}", source="test")
                      for i, lic in enumerate(["pd", "zero", "by", "nc", None, "pd", "pd", "pd", "pd"])]
            images[5]["landing_page"] = None
            annotations = [dict(image_id=str(i), category_id="stripe") for i in range(8)]
            annotations.append(dict(image_id="6", category_id="stripe"))
            metadata = dict(images=images, annotations=annotations,
                            licenses=[dict(id=k, name=v) for k, v in licenses],
                            categories=[dict(id="stripe", name="striped pattern")])
            (root / "metadata.json").write_text(json.dumps(metadata))
            for part, ids in [(1, range(7)), (2, [])]:
                with tarfile.open(root / f"pt{part}.tar.gz", "w:gz") as archive:
                    for i in ids:
                        # Traversal-like members cannot control extraction destination.
                        entry = tarfile.TarInfo(f"../../{i}.jpg")
                        payload = b"same-bytes"
                        entry.size = len(payload)
                        archive.addfile(entry, io.BytesIO(payload))
                    link = tarfile.TarInfo("ignored.jpg")
                    link.type = tarfile.SYMTYPE
                    link.linkname = "/etc/passwd"
                    archive.addfile(link)
            report = pipeline.audit(root)
            self.assertEqual(report["candidate_total"], 1)
            self.assertEqual(report["excluded_counts"], dict(rights_review=3, missing_provenance=1,
                                                           ambiguous_annotation=2, missing_image=1, exact_duplicate=1))
            self.assertEqual(report["exact_duplicate_groups"], [["0", "1"]])
            self.assertEqual(len(list((root / "candidate").rglob("*.jpg"))), 1)
            self.assertTrue((root / "manifest.csv").exists())
            self.assertEqual(pipeline.audit(root), report)  # Repeatable, no stale files.

    def test_cache_checksum_mismatch_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "data").write_bytes(b"corrupt")
            with patch.object(pipeline, "ARTIFACTS", {"data": ("remote", "0" * 64)}):
                with self.assertRaisesRegex(ValueError, "Checksum mismatch"):
                    pipeline.acquire(root, offline=True)

    def test_offline_missing_cache_does_not_download(self):
        with tempfile.TemporaryDirectory() as directory:
            with patch.object(pipeline.subprocess, "run") as run:
                with self.assertRaises(FileNotFoundError):
                    pipeline.acquire(Path(directory), offline=True)
                run.assert_not_called()


if __name__ == "__main__":
    unittest.main()
