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

    def test_known_ids_never_downloaded(self):
        with tempfile.TemporaryDirectory() as temp:
            collector = c.Collector(Path(temp)/'review', known_ids=['commons:123'])
            with patch.object(collector, 'get') as fetch:
                collector.add('family', 'KENTE', 'kente', {'key':'commons:123'})
                fetch.assert_not_called()
            report = collector.save({'family': {'KENTE':[]}}, 'test', final=False)
            self.assertEqual(report['skipped_known_source_ids'], 1)
            self.assertEqual(report['unique_images'], 0)

    def test_commons_pagination_and_limit(self):
        with tempfile.TemporaryDirectory() as temp:
            collector = c.Collector(Path(temp)/'review', per_query=3)
            payloads = [
                {'query':{'pages':[{'pageid':1},{'pageid':2}]},
                 'continue':{'continue':'gsroffset||','gsroffset':2}},
                {'query':{'pages':[{'pageid':3},{'pageid':4}]}},
            ]
            responses = [unittest.mock.Mock() for _ in payloads]
            for response, payload in zip(responses, payloads):
                response.json.return_value = payload
            with patch.object(collector, 'get', side_effect=responses) as fetch:
                self.assertEqual([p['pageid'] for p in collector.commons_pages('cloth')], [1,2,3])
            self.assertEqual(fetch.call_args_list[1].kwargs['params']['gsroffset'],2)
            self.assertEqual(fetch.call_args_list[1].kwargs['params']['gsrlimit'],1)

    def test_repeated_continuation_stops(self):
        with tempfile.TemporaryDirectory() as temp:
            collector = c.Collector(Path(temp)/'review', per_query=150)
            response = unittest.mock.Mock()
            response.json.return_value = {'query':{'pages':[{'pageid':1}]},
                                          'continue':{'gsroffset':1,'continue':'x'}}
            with patch.object(collector, 'get', return_value=response) as fetch:
                self.assertEqual(len(list(collector.commons_pages('cloth'))), 2)
                self.assertEqual(fetch.call_count, 2)

    def test_cma_pagination_textile_department(self):
        with tempfile.TemporaryDirectory() as temp:
            collector = c.Collector(Path(temp)/'review', per_query=60)
            first, second = unittest.mock.Mock(), unittest.mock.Mock()
            first.json.return_value = {'data':[{'id':x} for x in range(50)]}
            second.json.return_value = {'data':[{'id':50}]}
            with patch.object(collector, 'get', side_effect=[first,second]) as fetch:
                self.assertEqual(len(list(collector.cma_rows('floral'))),51)
            self.assertEqual(fetch.call_args_list[1].kwargs['params']['skip'],50)
            self.assertEqual(fetch.call_args_list[1].kwargs['params']['department'],'Textiles')

    def test_wall_limit_preserves_report(self):
        with tempfile.TemporaryDirectory() as temp:
            collector = c.Collector(Path(temp)/'review')
            collector.deadline = 0
            with patch.object(collector.session, 'get') as fetch:
                with self.assertRaises(c.CollectionLimit):
                    collector.get(c.COMMONS)
                fetch.assert_not_called()
            report = collector.save({'family': {'KENTE':[]}}, 'test', final=False)
            self.assertEqual(report['stop_reason'], 'wall_time_limit')
            self.assertTrue((collector.root/'manifest.jsonl').exists())


if __name__ == '__main__':
    unittest.main()
