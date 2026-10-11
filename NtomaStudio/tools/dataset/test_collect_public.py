import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from PIL import Image
import collect_public as c


class Response:
    def __init__(self, data):
        self.data = data
    def __enter__(self): return self
    def __exit__(self, *args): pass
    def iter_content(self, size): yield self.data


class CollectorTests(unittest.TestCase):
    def test_rights_fail_closed(self):
        for value in ('', 'unknown', 'https://evil.test/licenses/by/4.0/',
                      'https://creativecommons.org/licenses/by-nc/4.0/',
                      'https://creativecommons.org/licenses/by-nd/4.0/'):
            self.assertIsNone(c.rights_id(value))
        self.assertEqual(c.rights_id('https://creativecommons.org/licenses/by/4.0/'), 'cc-by-4.0')
        self.assertEqual(c.rights_id('http://creativecommons.org/publicdomain/zero/1.0/'), 'cc0')

    def test_no_auto_labels_and_deduplicated_matches(self):
        with tempfile.TemporaryDirectory() as temp:
            collector = c.Collector(Path(temp)/'review')
            image = io.BytesIO()
            Image.new('RGB', (300,400), 'red').save(image, 'JPEG')
            item = dict(key='commons:123', title='cloth', source='Commons', creator='Test Artist',
                        landing_page='https://commons.wikimedia.org/wiki/File:cloth.jpg',
                        image_url='https://upload.wikimedia.org/test.jpg',
                        license_url='https://creativecommons.org/licenses/by/4.0/')
            with patch.object(collector, 'get', return_value=Response(image.getvalue())) as fetch:
                collector.add('family', 'WAX', 'wax cloth', item)
                collector.add('family', 'ANKARA', 'ankara cloth', item)
                collector.add('pattern', 'STRIPED', 'stripe cloth', dict(item, key='commons:124'))
                self.assertEqual(fetch.call_count, 2)
            first = collector.rows['commons:123']
            self.assertEqual(first['reviewed_labels'], [])
            self.assertEqual(len(first['matches']), 2)
            self.assertEqual(collector.rows['commons:124']['duplicate_of'], 'commons:123')
            report = collector.save({'family': {'WAX': [], 'ANKARA': []}, 'pattern': {'STRIPED': []}}, 'test')
            self.assertEqual(report['unique_images'], 1)
            self.assertEqual(report['training_approved_images'], 0)
            self.assertEqual(len(list((collector.root/'images').glob('*.jpg'))), 1)
            self.assertTrue((collector.root/first['evidence']).exists())

    def test_missing_creator_never_downloaded(self):
        with tempfile.TemporaryDirectory() as temp:
            collector = c.Collector(Path(temp)/'review')
            item = dict(key='x', license_url='https://creativecommons.org/licenses/by/4.0/',
                        landing_page='https://example.test', creator='')
            with patch.object(collector, 'get') as fetch:
                collector.add('family', 'SILK', 'silk', item)
                fetch.assert_not_called()
            self.assertEqual(len(collector.rows), 0)


if __name__ == '__main__':
    unittest.main()
