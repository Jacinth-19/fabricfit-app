import copy
import json
from pathlib import Path
import unittest

import audit_readiness as a


class ReadinessTests(unittest.TestCase):
    def setUp(self):
        self.policy = json.loads((Path(__file__).parent/'release_targets.json').read_text())
        self.row = dict(source_id='test:1', group_id='design:1', reviewer='human',
                        decision='approved', rights_evidence='evidence/1.json', cloth_region_review='crop approved',
                        rights_verified=True, labels_verified=True, held_out_for_app_qa=False,
                        reviewed_pattern=['DOTTED'], split='train', sha256='a'*64)

    def test_empty_means_zero_never_ready(self):
        report = a.audit([], self.policy)
        self.assertFalse(report['release_ready'])
        self.assertFalse(report['collection_budget_met'])
        self.assertEqual(report['class_budgets']['DOTTED']['gaps']['train_design_groups'],300)

    def test_query_label_is_not_approval(self):
        report = a.audit([dict(source_id='cma:1', matches=[{'suggested_label':'DOTTED'}],
                               reviewed_labels=[], status='REVIEW_REQUIRED_NOT_TRAINING_DATA')], self.policy)
        self.assertEqual(report['approved_usable_rows'],0)

    def test_crops_are_one_group(self):
        report = a.audit([self.row, dict(self.row, source_id='crop:2')], self.policy)
        self.assertEqual(report['class_budgets']['DOTTED']['counts']['train_design_groups'],1)
        self.assertFalse(report['release_ready'])

    def test_group_split_leak_fails_closed(self):
        report = a.audit([self.row, dict(self.row, source_id='test:2', split='test', sha256='b'*64)], self.policy)
        self.assertTrue(report['integrity_errors'])
        self.assertEqual(report['approved_usable_rows'],0)

    def test_same_source_new_group_fails_closed(self):
        report = a.audit([self.row, dict(self.row, group_id='fake:2')], self.policy)
        self.assertTrue(report['integrity_errors'])
        self.assertEqual(report['approved_usable_rows'],0)

    def test_app_holdout_or_missing_verification_excluded(self):
        for updates in ({'held_out_for_app_qa':True}, {'rights_verified':False},
                        {'labels_verified':False}, {'reviewer':''}):
            report = a.audit([dict(self.row, **updates)], self.policy)
            self.assertEqual(report['approved_usable_rows'],0)

    def test_target_domain_required_for_slice(self):
        report = a.audit([dict(self.row, split='test', input_origin='camera', view='worn_outfit')], self.policy)
        counts = report['class_budgets']['DOTTED']['counts']
        self.assertEqual(counts['test_design_groups'],1)
        self.assertEqual(counts['camera_test_design_groups'],0)
        report = a.audit([dict(self.row, split='test', input_origin='camera', view='worn_outfit', target_domain=True)], self.policy)
        self.assertEqual(report['class_budgets']['DOTTED']['counts']['camera_test_design_groups'],1)

    def test_ood_requires_review(self):
        row = dict(self.row, reviewed_pattern=[], split='ood_test', target_domain=True)
        self.assertEqual(a.audit([row], self.policy)['ood_test_groups'],0)
        self.assertEqual(a.audit([dict(row, ood_verified=True)], self.policy)['ood_test_groups'],1)

    def test_hash_required_and_conflicting_hash_fails_closed(self):
        row = dict(self.row)
        row.pop('sha256')
        self.assertEqual(a.audit([row], self.policy)['approved_usable_rows'],0)
        row = dict(self.row, source_id='different-source', group_id='false-new-group')
        report = a.audit([self.row, row], self.policy)
        self.assertEqual(report['approved_usable_rows'],0)
        self.assertTrue(any(x['field'] == 'sha256' for x in report['integrity_errors']))

    def test_same_design_does_not_fill_both_origin_quotas(self):
        base = dict(self.row, split='test', target_domain=True, view='worn_outfit')
        rows = [dict(base, input_origin='camera'),
                dict(base, source_id='crop:2', sha256='b'*64, input_origin='gallery')]
        counts = a.audit(rows, self.policy)['class_budgets']['DOTTED']['counts']
        self.assertEqual(counts['camera_test_design_groups'],1)
        self.assertEqual(counts['gallery_test_design_groups'],0)

    def test_full_budget_is_not_release_permission(self):
        policy = copy.deepcopy(self.policy)
        policy['pattern_labels'] = ['DOTTED']
        for key in policy['per_supported_class']:
            policy['per_supported_class'][key] = 1
        policy['ood_test_groups'] = 1
        rows = [dict(self.row, source_id=str(i), group_id=str(i), sha256=str(i)*64, split=split,
                     target_domain=True, input_origin=origin, view='worn_outfit')
                for i,(split,origin) in enumerate([('train','camera'),('validation','camera'),
                                                 ('test','camera'),('test','gallery')])]
        rows.append(dict(self.row, source_id='ood', group_id='ood', sha256='f'*64, split='ood_test',
                         target_domain=True, reviewed_pattern=[], ood_verified=True))
        report = a.audit(rows, policy)
        self.assertTrue(report['collection_budget_met'])
        self.assertFalse(report['release_ready'])

    def test_family_taxonomy_approved_zero_groups(self):
        # Owner-approved six visual families (2026-10-12): budgets exist, but no reviewed groups.
        report = a.audit([], self.policy, 'family')
        self.assertEqual(sorted(report['class_budgets']), sorted(self.policy['family_labels']))
        self.assertFalse(report['collection_budget_met'])
        self.assertFalse(report['release_ready'])


if __name__ == '__main__':
    unittest.main()
