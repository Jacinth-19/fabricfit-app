#!/usr/bin/env python3
"""Show real installed-app captures in a browser; not an Android/web app emulator.

Run from NtomaStudio:
  python tools/qa/serve-updated-app.py --captures qa-artifacts/updated-app --port 8080

Retrieve captures from the successful Actions run first. Media remains ignored by Git.
"""
import argparse
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

TEMPLATE = Path(__file__).with_name("updated-app-preview.html")
REQUIRED = [
    "01-create.png", "02-whole-dress.png", "03-selected-fabric.png", "04-analysis.png",
    "05-template.png", "06-another-outfit.png", "07-texture-scale.png", "ntoma-studio-updated.mp4",
]


class PreviewHandler(SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path.split("?", 1)[0] in ("/", "/index.html"):
            data = TEMPLATE.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
            return
        super().do_GET()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--captures", type=Path, required=True)
    parser.add_argument("--port", type=int, default=8080)
    args = parser.parse_args()
    folder = args.captures.resolve()
    missing = [name for name in REQUIRED if not (folder / name).is_file()]
    if missing:
        parser.error("Missing verified captures: " + ", ".join(missing))
    print(f"Updated Android app capture viewer on port {args.port}. This is recorded footage, not a live emulator.", flush=True)
    ThreadingHTTPServer(("0.0.0.0", args.port), partial(PreviewHandler, directory=str(folder))).serve_forever()


if __name__ == "__main__":
    main()
