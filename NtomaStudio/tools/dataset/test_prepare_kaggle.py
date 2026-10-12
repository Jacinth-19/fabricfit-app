"""The Kaggle export must read release classes, merge aliases and skip attribute folders."""
import json
import tempfile
import unittest
from pathlib import Path

import prepare_kaggle
import taxonomy
from scrape import common  # noqa: F401  (ensures scrape/ is importable like prepare_kaggle)

HERE = Path(__file__).resolve().parent


class IterPoolTest(unittest.TestCase):
    def setUp(self):
        self.schema = json.loads((HERE / 'label_schema.json').read_text())
        self.tmp = Path(tempfile.mkdtemp())
        self.raw, self.scraped = self.tmp / 'raw', self.tmp / 'scraped'
        for folder, names in {'WAX': ['w.jpg'], 'ANKARA': ['a.jpg'], 'KENTE': ['k.jpg'],
                              'COTTON_PLAIN': ['c.jpg'], 'FUGU': ['f.jpg']}.items():
            (self.raw / folder).mkdir(parents=True)
            for n in names:
                (self.raw / folder / n).write_bytes(b'x')
        (self.scraped / 'src1' / 'ANKARA').mkdir(parents=True)
        (self.scraped / 'src1' / 'ANKARA' / 's.jpg').write_bytes(b'x')
        (self.scraped / '_hidden' / 'KENTE').mkdir(parents=True)
        (self.scraped / '_hidden' / 'KENTE' / 'h.jpg').write_bytes(b'x')

    def test_aliases_map_to_release_class_and_attributes_are_skipped(self):
        classes = taxonomy.release_classes(self.schema)
        got = [(cls, path.name, source) for cls, path, source in
               prepare_kaggle.iter_pool(self.raw, self.scraped, classes, self.schema)]
        wax_ankara = sorted(name for cls, name, _ in got if cls == 'WAX_ANKARA')
        self.assertEqual(wax_ankara, ['a.jpg', 's.jpg', 'w.jpg'])
        self.assertNotIn('COTTON_PLAIN', {cls for cls, _, _ in got})
        self.assertNotIn('FUGU', {cls for cls, _, _ in got})
        self.assertNotIn('h.jpg', {name for _, name, _ in got}, 'underscore folders must be ignored')
        self.assertIn(('KENTE', 'k.jpg', 'app'), got)


if __name__ == '__main__':
    unittest.main()


H = lambda c: c * 64  # noqa: E731  (fake but well-formed sha256 values)


def _row(**kw):
    base = dict(decision='approved', source_id='src', group_id='g', reviewer='R1',
                rights_evidence='licence URL saved', cloth_region_review='cloth only',
                sha256=H('a'), rights_verified=True, labels_verified=True,
                held_out_for_app_qa=False, split='train', reviewed_family=['KENTE'],
                reviewed_pattern=[], view='worn_outfit', input_origin='camera', target_domain=True)
    base.update(kw)
    return base


class ManifestGateTest(unittest.TestCase):
    def setUp(self):
        self.policy = json.loads((HERE / 'release_targets.json').read_text())

    def test_only_single_label_approved_rows_are_indexed(self):
        rows = [
            _row(),
            _row(source_id='s2', group_id='g2', sha256=H('b'), reviewed_family=['KENTE', 'LACE']),
            _row(source_id='s3', group_id='g3', sha256=H('c'), rights_verified=False),
            _row(source_id='s4', group_id='g4', sha256=H('d'), decision='rejected'),
            _row(source_id='s5', group_id='g5', sha256=H('e'), split='ood_test', reviewed_family=[],
                 ood_verified=True),
        ]
        index = prepare_kaggle.build_manifest_index(rows, self.policy, 'family')
        self.assertEqual(set(index), {H('a')})
        self.assertEqual(index[H('a')]['_label'], 'KENTE')
        self.assertEqual(index[H('a')]['group_id'], 'g')

    def test_duplicate_hash_stops_export(self):
        rows = [_row(), _row(source_id='s2', group_id='g2')]  # same sha256
        with self.assertRaises(SystemExit):
            prepare_kaggle.build_manifest_index(rows, self.policy, 'family')

    def test_export_refuses_without_manifest(self):
        import subprocess, sys
        result = subprocess.run([sys.executable, str(HERE / 'prepare_kaggle.py'), '--out', str(self.tmp_out())],
                                capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn('--manifest', result.stderr)

    def tmp_out(self):
        return Path(tempfile.mkdtemp()) / 'export'


class EndToEndExportTest(unittest.TestCase):
    """Runs the real export CLI on generated images: splits and labels come from the manifest."""

    def test_manifest_drives_splits_and_labels(self):
        import hashlib, io, random, subprocess, sys
        from PIL import Image
        tmp = Path(tempfile.mkdtemp())
        raw, scraped = tmp / 'raw', tmp / 'scraped'
        scraped.mkdir()
        def textured(seed):
            rnd = random.Random(seed)
            img = Image.new('RGB', (300, 300))
            img.putdata([(rnd.randint(0, 255), rnd.randint(0, 255), rnd.randint(0, 255)) for _ in range(90000)])
            buf = io.BytesIO(); img.save(buf, 'JPEG'); return buf.getvalue()
        plan = [('WAX', 1, 'WAX_ANKARA', 'train'), ('KENTE', 2, 'KENTE', 'validation'),
                ('KENTE', 3, 'KENTE', 'test'), ('COTTON_PLAIN', 4, None, None)]
        rows = []
        for i, (folder, seed, label, split) in enumerate(plan):
            (raw / folder).mkdir(parents=True, exist_ok=True)
            data = textured(seed)
            (raw / folder / f'img{i}.jpg').write_bytes(data)
            if label:
                rows.append(_row(source_id=f'src{i}', group_id=f'grp{i}', sha256=hashlib.sha256(data).hexdigest(),
                                 split=split, reviewed_family=[label]))
        manifest = tmp / 'manifest.jsonl'
        manifest.write_text('\n'.join(json.dumps(r) for r in rows) + '\n')
        out = tmp / 'export'
        result = subprocess.run([sys.executable, str(HERE / 'prepare_kaggle.py'), '--raw-dir', str(raw),
                                 '--scraped-dir', str(scraped), '--out', str(out), '--manifest', str(manifest),
                                 '--min-side', '100'], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        exported = sorted(p.relative_to(out).as_posix() for p in out.rglob('*.jpg'))
        self.assertEqual(exported, ['test/KENTE/app_img2.jpg', 'train/WAX_ANKARA/app_img0.jpg',
                                    'validation/KENTE/app_img1.jpg'])
        self.assertFalse(any('COTTON' in p for p in exported), 'attribute folder must not be exported')
