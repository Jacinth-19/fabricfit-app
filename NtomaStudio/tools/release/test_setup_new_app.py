import tempfile
import json
import shutil
import subprocess
import os
import sys
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


    @unittest.skipUnless(shutil.which('keytool'), 'JDK keytool required (runs in Actions)')
    def test_owner_setup_creates_recoverable_key_and_feeds_secrets_via_stdin(self):
        original_run = setup.run
        remote_calls = []
        def command(args, **kwargs):
            if args[0] == 'keytool':
                return original_run(args, **kwargs)
            remote_calls.append((args, kwargs.get('data')))
            return b''
        with tempfile.TemporaryDirectory() as folder:
            backup = Path(folder) / 'private-backup'
            argv = ['setup_new_app.py', '--new-app', '--backup-dir', str(backup), '--submit']
            with patch.object(sys, 'argv', argv), patch.object(setup, 'preflight'), patch.object(setup, 'run', side_effect=command):
                setup.main()
            details = json.loads((backup / 'credentials.json').read_text())
            self.assertEqual(0o600, (backup / 'credentials.json').stat().st_mode & 0o777)
            self.assertEqual(0o600, (backup / 'ntoma-upload.keystore').stat().st_mode & 0o777)
            self.assertEqual(0o700, backup.stat().st_mode & 0o777)
            secret_calls = [c for c in remote_calls if c[0][1:3] == ['secret', 'set']]
            self.assertEqual(4, len(secret_calls))
            for args, data in secret_calls:
                self.assertTrue(data)
                self.assertNotIn(details['store_password'], args)
            fingerprint = next(data.decode() for args, data in remote_calls if args[1:3] == ['variable', 'set'])
            env = dict(os.environ, NTOMA_UPLOAD_KEYSTORE=str(backup / 'ntoma-upload.keystore'),
                       NTOMA_UPLOAD_STORE_PASSWORD=details['store_password'],
                       NTOMA_UPLOAD_KEY_PASSWORD=details['key_password'],
                       NTOMA_UPLOAD_KEY_ALIAS=details['alias'], NTOMA_UPLOAD_CERT_SHA256=fingerprint)
            result = subprocess.run([sys.executable, str(Path(setup.__file__).with_name('verify_upload_key.py'))], env=env, capture_output=True)
            self.assertEqual(0, result.returncode, result.stderr.decode())
            self.assertEqual(1, len([args for args, _ in remote_calls if args[1:3] == ['workflow', 'run']]))
