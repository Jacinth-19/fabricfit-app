#!/usr/bin/env python3
"""Verify a provided upload key without logging key material or passwords."""
import hashlib
import os
import re
import subprocess

DEMO_SHA256 = "dae4dafd1e9aef9abf51cb9178f40a9b8f3dc317a36fbe9191d11b3680a5c785"


def main():
    required = ["NTOMA_UPLOAD_KEYSTORE", "NTOMA_UPLOAD_STORE_PASSWORD", "NTOMA_UPLOAD_KEY_ALIAS", "NTOMA_UPLOAD_KEY_PASSWORD", "NTOMA_UPLOAD_CERT_SHA256"]
    if any(not os.environ.get(name) for name in required):
        raise SystemExit("Missing upload signing configuration; no release was built.")
    # keytool reads passwords directly from the environment, never process arguments.
    result = subprocess.run([
        "keytool", "-exportcert", "-keystore", os.environ["NTOMA_UPLOAD_KEYSTORE"],
        "-alias", os.environ["NTOMA_UPLOAD_KEY_ALIAS"],
        "-storepass:env", "NTOMA_UPLOAD_STORE_PASSWORD",
    ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        raise SystemExit("Cannot read upload certificate; check keystore, alias and password privately.")
    fingerprint = hashlib.sha256(result.stdout).hexdigest()
    expected = re.sub(r"[:\s]", "", os.environ["NTOMA_UPLOAD_CERT_SHA256"]).lower()
    if not re.fullmatch(r"[0-9a-f]{64}", expected):
        raise SystemExit("Upload certificate SHA-256 must contain 64 hex digits.")
    if fingerprint == DEMO_SHA256:
        raise SystemExit("Public demo certificate rejected, even if the key was renamed.")
    if fingerprint != expected:
        raise SystemExit("Upload certificate mismatch: use the key registered in Play Console.")
    print("Upload certificate matches configured identity; public demo key rejected.")


if __name__ == "__main__":
    main()
