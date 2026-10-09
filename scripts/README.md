# scripts — repo-wide shell scripts (Git Bash)

## What lives here

- `check-env.sh` (Step 01): prints the toolchain table — Java, Node, Docker, AWS CLI, SAM, ffmpeg,
  Python + Pillow, gh.
- Coming later, one per step that needs it: `create-labels.sh` and `create-issues.sh` (Step 04),
  `sim.sh` (Step 22), `create-local-table.sh` (Step 28), `deploy-backend.sh` (Step 34),
  `deploy-frontend.sh` (Step 61), `unkill.sh` (Step 67), `teardown.sh` and `export-table.sh` (Step 68),
  `release.sh` (Step 69).

## What must NOT live here

- Build logic that belongs to a module. Maven owns the Java build, npm owns the frontend, and the asset
  pipeline lives in `tools/`.
- Credentials of any kind. Scripts read the AWS profile and environment variables; nothing is baked in.
- Windows-only scripts. Everything is `.sh` for Git Bash (Q18); a `.cmd` or `.ps1` file exists only where
  a tool ships it (the Maven wrapper in Step 07).

## How do I run it

From the repo root: `./scripts/check-env.sh`. Every later script is run the same way, with its
arguments shown in the step that adds it.
