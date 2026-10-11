"""Expose CI captures through GitHub git-blob API (no branch commits, no credentials stored)."""
import base64
import json
import os
from pathlib import Path
import subprocess

repo, head = os.environ['GITHUB_REPOSITORY'], os.environ['GITHUB_SHA']
manifest = []
for path in sorted(Path('qa-artifacts').rglob('*')):
    if not path.is_file() or path.suffix not in {'.png', '.mp4', '.md', '.txt', '.xml'}:
        continue
    if path.name in {'logcat.txt', 'device-diagnostics.txt'} or path.stat().st_size > 40_000_000:
        continue
    payload = json.dumps({'content': base64.b64encode(path.read_bytes()).decode(), 'encoding': 'base64'})
    result = subprocess.run(['gh', 'api', f'repos/{repo}/git/blobs', '--input', '-'], input=payload, text=True, capture_output=True, check=True)
    blob = json.loads(result.stdout)
    manifest.append({'file': str(path.relative_to('qa-artifacts')), 'sha': blob['sha'], 'bytes': path.stat().st_size})
summary = json.dumps(manifest, indent=2)
check = {'name':'Captured app files', 'head_sha':head, 'status':'completed', 'conclusion':'neutral', 'output':{'title':'Real emulator captures', 'summary':'Original synthetic dress fixture. These are captures, not evidence that the entire journey passed; check validation.txt and the app-journey result.', 'text':summary}}
subprocess.run(['gh', 'api', f'repos/{repo}/check-runs', '--input', '-'], input=json.dumps(check), text=True, check=True)
