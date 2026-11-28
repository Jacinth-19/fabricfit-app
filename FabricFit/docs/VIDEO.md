# Video: the store promos, and what happens to a clip you post

Two unrelated things in this repo are called "video". They share a document because both
were, until recently, quietly not doing what they claimed.

## 1. Store promos — `store/video/`

| File | Spec | Notes |
| --- | --- | --- |
| `fabricfit_play_store_promo_16x9.mp4` | 1280×720, 24 fps, 15 s, H.264, silent | Play Store listing promo |
| `fabricfit_shorts_reel_9x16.mp4` | 720×1280, 24 fps, 12 s, H.264, silent | Shorts / Reels cut |
| `fabricfit_promo_preview.gif` | 640 px wide, 8 fps | Inline preview in the README and listing |

**Rebuild them:**

```bash
python3 -m venv /tmp/fvenv && /tmp/fvenv/bin/pip install -q imageio-ffmpeg pillow numpy
/tmp/fvenv/bin/python tools/store_video/render_promo.py          # renders both
/tmp/fvenv/bin/python tools/store_video/render_promo.py --check  # assets only, no render
```

An ffmpeg binary is never on `PATH` in a clean checkout. The script uses `$FFMPEG` if it
is set, otherwise whatever `imageio_ffmpeg` vendored into the virtualenv.

**Why this exists.** The promos that shipped before were made by hand and had drifted
badly: they still showed the retired "HERITAGE LUXE — MADE IN ACCRA" wordmark, the old
adinkra rosette, and the old splash, months after all three were replaced. Nothing caught
it, because nothing regenerated them. The renderer reads `store/screenshots/`, the brand
icon and the theme palette, so a rebuild after a redesign is one command.

`--check` fails if a screenshot is missing, the wrong size, or suspiciously small. Screens
must be 1080×1920; the six it uses are welcome, home, fitscan, scan results, discover and
wardrobe, in that order.

**Both videos are silent, deliberately.** They previously carried a synthesized marimba
score. Adding placeholder music to an auto-generated build would mean licensing noise, and
Play accepts a silent promo. If a track is wanted later, mux it onto the rendered file
rather than teaching the renderer about audio.

The opening beat (mark → settling ring → wordmark → tagline) is echoed by the in-app
splash in `ui/AppShell.kt`, so the first thing a user sees in the listing matches the first
thing they see after install.

## 2. Posting a video — `media/VideoProcessor.kt`

`prepareForUpload` re-encodes the picked clip to H.264/MP4 through **Media3 Transformer**
before it is attached to a post. Clips are capped at 3 minutes.

It previously did not transcode at all. The implementation depended on FFmpegKit, and when
ffmpeg-kit stopped being published to Maven Central the dependency was removed — but the
function carried on copying source bytes while its `quality` argument did nothing.

Notes for anyone touching this:

- **Media3 is pinned to 1.4.1.** 1.5.x requires `compileSdk 35`; this project is on 34.
  Bumping Media3 means bumping the SDK level first.
- **Only the stable 1.4.x call is used:** `transformer.start(editedItem, path)`. The
  per-encoder settings API moved between 1.3 and 1.5, so `quality` is documented as having
  no effect rather than being wired to something that breaks on a minor upgrade. Passing
  `quality <= 0` skips re-encoding entirely.
- **Failure falls back to a byte copy**, because a post that cannot be re-encoded is still
  worth posting. The result carries `wasTranscoded`, and the composer labels the attachment
  "Optimised for upload" or "Original file — could not be re-encoded". Do not show a
  compression claim the result does not support.
