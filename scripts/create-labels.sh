#!/usr/bin/env bash
# Creates every label the project uses (knowledge/project_management.md §5).
#
# Safe to re-run: `gh label create --force` creates a label that is missing and
# updates the colour/description of one that exists, so the end state is always
# the same list. Run it from anywhere: ./scripts/create-labels.sh
set -euo pipefail

# name|colour|description   (colour = 6 hex digits, no leading #)
LABELS='
phase:0|0E8A16|Phase 0 — Foundations
phase:1|1D76DB|Phase 1 — Game Engine
phase:2|5319E7|Phase 2 — Backend
phase:3|D93F0B|Phase 3 — Frontend
phase:4|FBCA04|Phase 4 — Studio
phase:5|006B75|Phase 5 — Infra & CI/CD
phase:6|B60205|Phase 6 — Balance & Launch
area:engine|C5DEF5|Pure-Java rules engine (engine/)
area:backend|C5DEF5|Spring Boot API on Lambda (backend/)
area:frontend|C5DEF5|React app (frontend/)
area:infra|C5DEF5|SAM templates, CI/CD, AWS (infra/, .github/)
area:assets|C5DEF5|Art, audio and the asset pipeline (tools/)
area:docs|C5DEF5|README, knowledge/, plan/, docs/
area:sim|C5DEF5|Balance simulator (sim/)
type:step|BFDADC|One step of PLAN.md
type:bug|D73A4A|Behaves differently from the rulebook or from what you expected
type:idea|A2EEEF|For later; promoted only at a phase review
type:chore|EDEDED|Maintenance: dependency bumps, tool fixes
good-first-step|7057FF|Mostly configuration; good for a low-energy day
blocked|000000|Cannot move until something else happens
'

while IFS='|' read -r name colour description; do
  [[ -z "$name" ]] && continue        # skip the blank first/last lines
  gh label create "$name" --color "$colour" --description "$description" --force
done <<< "$LABELS"
