import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock

import monitor_kaggle as m


class Timer:
    def __init__(self): self.value = 0
    def now(self): return self.value
    def sleep(self, seconds): self.value += seconds


class MonitorTests(unittest.TestCase):
    def run_observer(self, effects, seconds=300):
        api = Mock()
        api.kernels_status.side_effect = effects
        timer = Timer()
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder)/'report.json'
            code, state = m.observe(api, 'owner/notebook/7', path, seconds, 60, timer.now, timer.sleep)
            self.assertEqual(json.loads(path.read_text()), state)
        api.kernels_push.assert_not_called()
        api.kernels_cancel.assert_not_called()
        self.assertTrue(all(c.args[0] == 'owner/notebook/7' for c in api.kernels_status.call_args_list))
        return code, state

    def test_queued_running_complete(self):
        code, state = self.run_observer([SimpleNamespace(status=x) for x in ('QUEUED','RUNNING','COMPLETE')])
        self.assertEqual(code, 0)
        self.assertEqual(state['elapsed_seconds'], 120)

    def test_remote_failure_is_not_success(self):
        code, state = self.run_observer([SimpleNamespace(status='ERROR')])
        self.assertEqual(code, 1)
        self.assertEqual(state['status'], 'ERROR')

    def test_retry_and_timeout_never_resubmit(self):
        code, state = self.run_observer([RuntimeError('secret-must-not-leak'), SimpleNamespace(status='RUNNING')], 120)
        self.assertEqual(code, 2)
        self.assertNotIn('secret-must-not-leak', json.dumps(state))
        self.assertEqual(state['status'], 'MONITOR_TIMEOUT')

    def test_invalid_ref(self):
        for ref in ('owner/notebook', 'owner/notebook/0', 'owner/notebook/1;echo bad', '../etc/1'):
            with self.assertRaises(ValueError): m.validate_ref(ref)


if __name__ == '__main__': unittest.main()
