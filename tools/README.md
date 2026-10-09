# tools — build-time helpers (Python and shell)

## What lives here

- `assets/`: the asset pipeline (Python 3.13 + Pillow, Steps 39–41) — inventory, frame extraction, atlas
  packing, the ffmpeg audio sprite (Step 50), a manifest with content hashes, and the scripts that
  publish to and pull from the private S3 bucket. `assets/config/assets.yaml` is the only hand-edited
  file in it.
- `seed-ghosts/`: fills the prod ghost pool with bot bands so the first players have opponents (Step 72).
- `docs/`: generators such as the engine event catalogue (Step 24).

## What must NOT live here

- Raw or processed art and audio (ADR-13). `assets/raw/` and `assets/build/` are gitignored; the files
  live in a private S3 bucket because the Time Fantasy licence forbids redistribution and this repo is
  public.
- Game code. Tools prepare inputs for the game; they never implement rules or serve requests.
- Palette conversion. The Time Fantasy to Elements recolouring is authored in the Forgotten Kanji toolkit
  (ADR-14); these scripts only read its output.

## How do I run it

TODO Step 39: `python tools/assets/build.py --extract`.
TODO Step 41: `python tools/assets/build.py --all`, then `tools/assets/publish.sh` to push to S3.
