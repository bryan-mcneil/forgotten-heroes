#!/usr/bin/env bash
# Creates the seven milestones, one per phase (knowledge/project_management.md §1).
#
# Safe to re-run: GitHub refuses a duplicate milestone title (HTTP 422), so the
# script lists what exists first and only creates the missing ones.
# Run it from anywhere: ./scripts/create-milestones.sh
set -euo pipefail

# title|description
MILESTONES='
Phase 0 — Foundations|plan/phase_0_foundations.md
Phase 1 — Game Engine|plan/phase_1_engine.md
Phase 2 — Backend|plan/phase_2_backend.md
Phase 3 — Frontend|plan/phase_3_frontend.md
Phase 4 — Studio|plan/phase_4_studio.md (deferred until the corgi art arrives)
Phase 5 — Infra & CI/CD|plan/phase_5_infra_cicd.md
Phase 6 — Balance & Launch|plan/phase_6_launch.md
'

# `gh api` is a raw call to GitHub's HTTP API. `:owner/:repo` is filled in from the
# current repository; `--jq` filters the JSON that comes back (gh bundles jq).
existing=$(gh api 'repos/:owner/:repo/milestones?state=all&per_page=100' --jq '.[].title')

while IFS='|' read -r title description; do
  [[ -z "$title" ]] && continue
  if grep -qxF -- "$title" <<< "$existing"; then
    echo "= exists:  $title"
  else
    gh api repos/:owner/:repo/milestones -f title="$title" -f description="$description" \
      --jq '"+ created: " + .title'
  fi
done <<< "$MILESTONES"
