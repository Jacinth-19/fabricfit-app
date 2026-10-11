import unittest
from collections import Counter
from check_strings import placeholders, strings, RES, LOCALES, DRAFTS


class StringsTest(unittest.TestCase):
    def test_formatting_contract(self):
        self.assertEqual(Counter({(1, 'd'): 1, (2, 'd'): 1}), placeholders('%1$d of %2$d'))
        self.assertEqual(placeholders('%2$d and %1$d'), placeholders('%1$d of %2$d'))
        self.assertNotEqual(placeholders('%1$d'), placeholders('%1$d of %2$d'))
        self.assertFalse(placeholders('100%%'))

    def test_unreviewed_workflow_copy_is_not_shipped(self):
        base = strings(RES / 'values/strings.xml')
        for locale in LOCALES:
            self.assertFalse((RES / f'values-{locale}').exists())
            localized = strings(DRAFTS / f'{locale}-strings.xml')
            for key in ('template_disclosure', 'privacy_device_body', 'edit_hint', 'tryon_quota_note'):
                self.assertNotIn(key, localized)
                self.assertIn(key, base)
