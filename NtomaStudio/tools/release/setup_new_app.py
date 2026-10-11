#!/usr/bin/env python3
"""Owner-machine setup for a NEW Play app. No private material is printed or committed."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess

REPO = "Jacinth-19/ntoma-studio"
BRANCH = "arena/5b96a1cd-ntoma-studio"
ENVIRONMENT = "production"
SECRET_NAMES = (
    "NTOMA_UPLOAD_KEYSTORE_BASE64", "NTOMA_UPLOAD_STORE_PASSWORD",
    "NTOMA_UPLOAD_KEY_ALIAS", "NTOMA_UPLOAD_KEY_PASSWORD",
)
VARIABLE = "NTOMA_UPLOAD_CERT_SHA256"


def run(args, *, data=None, env=None, private=False):
    result = subprocess.run(args, input=data, env=env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        if private:
            raise RuntimeError("Private signing operation failed; inspect configuration locally, not in chat.")
        raise RuntimeError(result.stderr.decode(errors="replace").strip() or "Command failed")
    return result.stdout


def gh_json(*args):
    return json.loads(run(["gh", *args]))


def listing(endpoint, field):
    output = run(["gh", "api", "--paginate", f"repos/{REPO}/{endpoint}?per_page=100", "--jq", f".{field}[]"])
    # gh emits one JSON object at a time (older CLI versions do not have --slurp).
    decoder, items, text = json.JSONDecoder(), [], output.decode().strip()
    while text:
        item, end = decoder.raw_decode(text)
        items.append(item)
        text = text[end:].lstrip()
    return items


def preflight(*, configure_environment=True):
    # Test secret access BEFORE creating any key or changing environment settings.
    repo_secrets = listing("actions/secrets", "secrets")
    if any(row["name"] in SECRET_NAMES for row in repo_secrets):
        raise RuntimeError("Upload secrets already exist. Refusing to create/replace a key; use the existing identity.")
    repo_variables = listing("actions/variables", "variables")
    if any(row["name"] == VARIABLE for row in repo_variables):
        raise RuntimeError("An upload certificate is already configured. Refusing to replace it.")
    environments = listing("environments", "environments")
    production = next((e for e in environments if e["name"] == ENVIRONMENT), None)
    if production:
        names = listing("environments/production/secrets", "secrets")
        variables = listing("environments/production/variables", "variables")
        if any(n["name"] in SECRET_NAMES for n in names) or any(v["name"] == VARIABLE for v in variables):
            raise RuntimeError("Production signing is already configured or partially configured. Do not replace it; recover the original backup.")
        if not any(r["type"] == "required_reviewers" for r in production.get("protection_rules", [])):
            raise RuntimeError("Existing production environment needs required reviewers; configure it privately before continuing.")
        policy = production.get("deployment_branch_policy") or {}
        if not policy.get("custom_branch_policies"):
            raise RuntimeError("Existing production environment needs a custom deployment-branch restriction.")
        policies = gh_json("api", f"repos/{REPO}/environments/production/deployment-branch-policies")["branch_policies"]
        if not any(p["name"] == BRANCH for p in policies):
            raise RuntimeError("Production environment does not permit this session branch.")
    else:
        if not configure_environment:
            print("No production environment yet; full setup will create it if authorized.")
            return
        owner = gh_json("api", "user")
        payload = {
            "reviewers": [{"type": "User", "id": owner["id"]}],
            "prevent_self_review": False,
            "deployment_branch_policy": {"protected_branches": False, "custom_branch_policies": True},
        }
        run(["gh", "api", "--method", "PUT", f"repos/{REPO}/environments/production", "--input", "-"], data=json.dumps(payload).encode())
        run(["gh", "api", "--method", "POST", f"repos/{REPO}/environments/production/deployment-branch-policies", "--input", "-"], data=json.dumps({"name": BRANCH, "type": "branch"}).encode())
    # Environment secret endpoints have separate permissions.
    listing("environments/production/secrets", "secrets")


def validate_backup(path):
    path = path.expanduser().resolve()
    repo_root = Path(__file__).resolve().parents[3]
    if path == repo_root or repo_root in path.parents:
        raise ValueError("Backup must be OUTSIDE this Git repository.")
    if path.exists():
        raise ValueError("Backup directory already exists. Never overwrite an upload-key backup.")
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--new-app", action="store_true", help="Confirm no prior Play upload exists")
    parser.add_argument("--backup-dir", type=Path, help="New private directory outside Git on your trusted machine")
    parser.add_argument("--version-code", type=int, default=1)
    parser.add_argument("--submit", action="store_true", help="Dispatch signed bundle build, not Play publishing")
    parser.add_argument("--preflight-only", action="store_true")
    args = parser.parse_args()
    if not args.new_app:
        parser.error("--new-app is required; existing Play identities must not be replaced")
    if not 1 <= args.version_code <= 2100000000:
        parser.error("invalid version code")
    if not args.preflight_only and not args.backup_dir:
        parser.error("--backup-dir is required")
    try:
        backup = validate_backup(args.backup_dir) if args.backup_dir else None
        preflight(configure_environment=not args.preflight_only)
        if args.preflight_only:
            print("GitHub signing configuration access confirmed. No key generated.")
            return
        if not shutil.which("keytool"):
            raise RuntimeError("Install JDK 17 (keytool) on your trusted machine first.")
        backup.mkdir(mode=0o700, parents=True, exist_ok=False)
        os.chmod(backup, 0o700)
        password = secrets.token_urlsafe(48)
        alias = "ntoma-upload"
        env = dict(os.environ, NTOMA_SETUP_PASSWORD=password)
        keystore = backup / "ntoma-upload.keystore"
        # Retain a recoverable owner-only backup before configuring any remote secrets.
        details = {"alias": alias, "store_password": password, "key_password": password}
        fd = os.open(backup / "credentials.json", os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "w") as out:
            json.dump(details, out)
        run(["keytool", "-genkeypair", "-noprompt", "-storetype", "PKCS12", "-keystore", str(keystore),
             "-alias", alias, "-keyalg", "RSA", "-keysize", "3072", "-validity", "10000",
             "-dname", "CN=Ntoma Studio upload", "-storepass:env", "NTOMA_SETUP_PASSWORD",
             "-keypass:env", "NTOMA_SETUP_PASSWORD"], env=env, private=True)
        os.chmod(keystore, 0o600)
        cert = run(["keytool", "-exportcert", "-keystore", str(keystore), "-alias", alias,
                    "-storepass:env", "NTOMA_SETUP_PASSWORD"], env=env, private=True)
        (backup / "upload-certificate.der").write_bytes(cert)
        fingerprint = hashlib.sha256(cert).hexdigest()
        values = (base64.b64encode(keystore.read_bytes()), password.encode(), alias.encode(), password.encode())
        for name, value in zip(SECRET_NAMES, values):
            run(["gh", "secret", "set", name, "--repo", REPO, "--env", ENVIRONMENT], data=value, private=True)
        run(["gh", "variable", "set", VARIABLE, "--repo", REPO, "--env", ENVIRONMENT], data=fingerprint.encode())
        print("New upload key backed up privately and signing environment configured.")
        print(f"Keep an encrypted/offline recovery copy of {backup}; do not send its contents in chat.")
        if args.submit:
            run(["gh", "workflow", "run", "release.yml", "--repo", REPO, "--ref", BRANCH,
                 "-f", f"version_code={args.version_code}", "-f", "confirm_upload_key=true"])
            print("Signed-bundle workflow dispatched. Approve the production environment in GitHub Actions.")
            print("This builds an AAB only; it does NOT publish to Play.")
    except (RuntimeError, ValueError) as error:
        raise SystemExit(f"Setup stopped: {error}\nDo not regenerate after a partial setup; preserve any private backup and recover that same key.")


if __name__ == "__main__":
    main()
