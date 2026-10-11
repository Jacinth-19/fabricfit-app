import hashlib
import os
import subprocess
import unittest
from unittest.mock import patch
import verify_upload_key as verifier


class UploadKeyTest(unittest.TestCase):
    def env(self, fingerprint):
        return {name: value for name, value in zip(
            ['NTOMA_UPLOAD_KEYSTORE', 'NTOMA_UPLOAD_STORE_PASSWORD', 'NTOMA_UPLOAD_KEY_ALIAS', 'NTOMA_UPLOAD_KEY_PASSWORD', 'NTOMA_UPLOAD_CERT_SHA256'],
            ['key.jks', 'private-value', 'upload', 'private-value', fingerprint])}

    def test_missing_secrets_fail_closed(self):
        with patch.dict(os.environ, {}, clear=True), self.assertRaises(SystemExit):
            verifier.main()

    def test_correct_certificate_passes_without_password_arguments(self):
        certificate = b'test-certificate'
        with patch.dict(os.environ, self.env(hashlib.sha256(certificate).hexdigest()), clear=True), patch('subprocess.run', return_value=subprocess.CompletedProcess([], 0, certificate, b'')) as call:
            verifier.main()
        self.assertNotIn('private-value', call.call_args.args[0])
        self.assertIn('-storepass:env', call.call_args.args[0])

    def test_wrong_certificate_rejected(self):
        with patch.dict(os.environ, self.env('a' * 64), clear=True), patch('subprocess.run', return_value=subprocess.CompletedProcess([], 0, b'wrong', b'')), self.assertRaises(SystemExit):
            verifier.main()

    def test_demo_key_rejected_even_if_expected(self):
        with patch.dict(os.environ, self.env(verifier.DEMO_SHA256), clear=True), patch('subprocess.run', return_value=subprocess.CompletedProcess([], 0, b'demo', b'')), patch('hashlib.sha256') as digest, self.assertRaises(SystemExit):
            digest.return_value.hexdigest.return_value = verifier.DEMO_SHA256
            verifier.main()
