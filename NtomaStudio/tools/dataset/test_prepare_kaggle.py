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
