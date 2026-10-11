import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import setup_new_app as setup


class NewAppSetupTest(unittest.TestCase):
    def test_existing_upload_secret_prevents_mutation(self):
        with patch.object(setup, 'listing', return_value=[{'name': setup.SECRET_NAMES[0]}]), patch.object(setup, 'run') as command:
            with self.assertRaises(RuntimeError):
                setup.preflight()
            command.assert_not_called()

    def test_denied_access_does_not_generate_or_write_key(self):
        with patch.object(setup, 'listing', side_effect=RuntimeError('403')), patch.object(setup, 'run') as command:
            with self.assertRaises(RuntimeError):
                setup.preflight()
            command.assert_not_called()

    def test_backup_cannot_be_inside_repository(self):
        with self.assertRaises(ValueError):
            setup.validate_backup(Path(__file__).resolve().parent / 'private-signing')

    def test_existing_backup_is_never_overwritten(self):
        with tempfile.TemporaryDirectory() as path:
            with self.assertRaises(ValueError):
                setup.validate_backup(Path(path))

    def test_listing_handles_paginated_json_without_new_cli_flags(self):
        with patch.object(setup, 'run', return_value=b'{"name":"first"}\n{\n"name":"second"\n}') as command:
            self.assertEqual([{'name': 'first'}, {'name': 'second'}], setup.listing('actions/secrets', 'secrets'))
            self.assertIn('--paginate', command.call_args.args[0])
            self.assertNotIn('--slurp', command.call_args.args[0])
