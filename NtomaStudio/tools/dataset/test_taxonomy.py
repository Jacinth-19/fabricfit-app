"""Consistency checks for the owner-approved taxonomy (2026-10-12)."""
import json
import tempfile
import unittest
from pathlib import Path

from taxonomy import release_classes, source_folders, stage_release_folders

HERE = Path(__file__).resolve().parent


class TaxonomyTest(unittest.TestCase):
    def setUp(self):
        self.targets = json.loads((HERE / 'release_targets.json').read_text())
        self.schema = json.loads((HERE / 'label_schema.json').read_text())

    def test_policy_matches_schema(self):
        self.assertEqual(self.targets['pattern_labels'], self.schema['pattern_labels'])
        self.assertEqual(self.targets['family_labels'], self.schema['release_visual_families'])

    def test_every_legacy_class_has_exactly_one_role(self):
        s = self.schema
        roles = []
        roles += s['release_visual_families']
        roles += s['family_aliases'].keys()
        roles += s['candidate_visual_families_pending_examples']
        roles += s['unsupported_pending_provenance']
        roles += s['attributes_not_visual_families']
        legacy = set(s['classes'])
        self.assertEqual(legacy - set(roles), set(), 'legacy class without a role')
        self.assertEqual(set(roles) - legacy - set(s['release_visual_families']), set(),
                         'role not in legacy list')
        self.assertEqual(len(roles), len(set(roles)), 'a class has two roles')

    def test_alias_targets_are_release_families(self):
        for alias, target in self.schema['family_aliases'].items():
            self.assertIn(target, self.schema['release_visual_families'], alias)

    def test_retired_pattern_labels_not_in_release_set(self):
        for label in self.schema['retired_pattern_labels']:
            self.assertNotIn(label, self.targets['pattern_labels'])

    def test_five_pattern_classes_and_six_families(self):
        self.assertEqual(len(self.targets['pattern_labels']), 5)
        self.assertEqual(len(self.targets['family_labels']), 6)


class StagingTest(unittest.TestCase):
    def setUp(self):
        self.schema = json.loads((HERE / 'label_schema.json').read_text())
        self.tmp = Path(tempfile.mkdtemp())
        self.aug = self.tmp / 'augmented'
        for folder, names in {'WAX': ['a.jpg', 'b.jpg'], 'ANKARA': ['a.jpg'], 'KENTE': ['k.jpg'],
                              'COTTON_PLAIN': ['c.jpg'], 'FUGU': ['f.jpg']}.items():
            (self.aug / folder).mkdir(parents=True)
            for n in names:
                (self.aug / folder / n).write_bytes(b'x')

    def test_aliases_merge_into_one_release_class(self):
        self.assertEqual(source_folders(self.schema, 'WAX_ANKARA'), ['WAX_ANKARA', 'ANKARA', 'WAX'])

    def test_stage_merges_aliases_and_skips_attributes_and_candidates(self):
        staged = self.tmp / 'staged_release'
        counts, missing = stage_release_folders(self.aug, staged, self.schema)
        self.assertEqual(counts['WAX_ANKARA'], 3)
        self.assertEqual(counts['KENTE'], 1)
        self.assertEqual(sorted(p.name for p in (staged / 'WAX_ANKARA').iterdir()),
                         ['ANKARA__a.jpg', 'WAX__a.jpg', 'WAX__b.jpg'])
        self.assertFalse((staged / 'COTTON_PLAIN').exists())
        self.assertFalse((staged / 'FUGU').exists())
        self.assertIn('ADINKRA', missing)
        self.assertEqual(set(missing), {c for c, n in counts.items() if n == 0})

    def test_restage_replaces_old_contents(self):
        staged = self.tmp / 'staged_release'
        stage_release_folders(self.aug, staged, self.schema)
        (self.aug / 'KENTE' / 'k.jpg').unlink()
        stage_release_folders(self.aug, staged, self.schema)
        self.assertFalse((staged / 'KENTE').exists())


if __name__ == '__main__':
    unittest.main()
