#!/usr/bin/env bash
# Creates one GitHub issue per step row in PLAN.md and puts it on the project board.
#
#   ./scripts/create-issues.sh            create what is missing
#   ./scripts/create-issues.sh --dry-run  only print what would happen (no writes)
#
# Idempotent: a step whose issue already exists (same title) is not created again, but
# its labels, milestone and board card are still checked. Running the script twice, or
# after adding a step 78 to PLAN.md, always ends in the same state: one issue per row.
#
# Needs: gh logged in with the board scopes (gh auth refresh -s project,read:project),
# the labels (create-labels.sh), the milestones (create-milestones.sh) and the board
# (gh project create --owner <you> --title "Forgotten Heroes").
set -euo pipefail

PLAN="PLAN.md"
PROJECT_TITLE="Forgotten Heroes"
DRY_RUN=0
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=1

cd "$(dirname "$0")/.."          # paths below are relative to the repo root

# run <cmd...>: execute, or only print the command in a dry run.
run() {
  if (( DRY_RUN )); then echo "      would run: $*"; else "$@" > /dev/null; fi
}

# slugify: the rule GitHub uses to turn a heading into a link anchor.
# Lowercase; drop everything except letters, digits, spaces and hyphens; spaces become hyphens.
# "Step 04 — Project management on GitHub" -> "step-04--project-management-on-github"
slugify() { tr '[:upper:]' '[:lower:]' | sed -e 's/[^a-z0-9 -]//g' -e 's/ /-/g'; }

# ---- 1. Facts fetched once (each is one API call) -------------------------------------
owner=$(gh repo view --json owner --jq '.owner.login')
repo_url=$(gh repo view --json url --jq '.url')
milestones=$(gh api 'repos/:owner/:repo/milestones?state=all&per_page=100' --jq '.[].title')

project_number=$(gh project list --owner "$owner" --format json \
  --jq ".projects[] | select(.title == \"$PROJECT_TITLE\") | .number" 2>/dev/null || true)
if [[ -z "$project_number" ]]; then
  if (( DRY_RUN )); then
    echo "note: board '$PROJECT_TITLE' not found (or no project scope); dry run continues without it" >&2
  else
    echo "error: no board titled '$PROJECT_TITLE'. Create it: gh project create --owner $owner --title \"$PROJECT_TITLE\"" >&2
    exit 1
  fi
fi

# Every issue, open or closed, as "number<TAB>title" lines; and the issue numbers already on the board.
existing_issues=$(gh issue list --state all --limit 200 --json number,title \
  --jq '.[] | "\(.number)\t\(.title)"')
on_board=""
if [[ -n "$project_number" ]]; then
  on_board=$(gh project item-list "$project_number" --owner "$owner" --limit 200 --format json \
    --jq '.items[] | select(.content.type == "Issue") | .content.number')
fi

# ---- 2. One issue per "- [ ] **NN** ... — est Nh" row --------------------------------
phase=""
created=0
skipped=0
# The plan is read on file descriptor 3 so that gh (which may read stdin) cannot eat the lines.
while IFS= read -r -u 3 line; do
  if [[ "$line" =~ ^##\ Phase\ ([0-6]) ]]; then phase="${BASH_REMATCH[1]}"; continue; fi
  [[ "$line" =~ ^-\ \[[\ x~]\]\ \*\*([0-9]{2})\*\*\ (.*)\ —\ est\ (.*)$ ]] || continue
  num="${BASH_REMATCH[1]}"; summary="${BASH_REMATCH[2]}"; est="${BASH_REMATCH[3]}"

  phase_file=$(ls plan/phase_"$phase"_*.md)
  heading=$(grep -m1 "^## Step $num — " "$phase_file" | sed 's/^## //')
  if [[ -z "$heading" ]]; then
    echo "error: no '## Step $num — ...' heading in $phase_file" >&2; exit 1
  fi
  title=$(tr -d '`' <<< "$heading")           # backticks do not render in titles
  anchor=$(slugify <<< "$heading")
  milestone=$(grep -m1 "^Phase $phase — " <<< "$milestones" || true)
  if [[ -z "$milestone" ]]; then
    if (( DRY_RUN )); then
      milestone="(missing: run scripts/create-milestones.sh)"
    else
      echo "error: no milestone for phase $phase; run scripts/create-milestones.sh first" >&2; exit 1
    fi
  fi
  labels="type:step,phase:$phase"
  body="**Plan row:** $summary (est $est)
**Instructions:** [$phase_file → $heading]($repo_url/blob/main/$phase_file#$anchor)

Done when the step's **Verify** passes locally and in CI, the PR carries a one-sentence *What I learned*, and the squash commit on \`main\` reads \`step-$num: title (#this issue)\`."

  number=$(awk -F '\t' -v t="$title" '$2 == t { print $1; exit }' <<< "$existing_issues")
  if [[ -n "$number" ]]; then
    echo "= #$number exists: $title"
    run gh issue edit "$number" --add-label "$labels" --milestone "$milestone"
    skipped=$((skipped + 1))
  else
    echo "+ creating: $title  [$labels · $milestone]"
    if (( DRY_RUN )); then
      echo "      link: $repo_url/blob/main/$phase_file#$anchor"
      number="NEW"
    else
      url=$(gh issue create --title "$title" --body "$body" --label "$labels" --milestone "$milestone")
      number="${url##*/}"                      # ".../issues/12" -> "12"
      sleep 1                                  # stay well under GitHub's write rate limit
    fi
    created=$((created + 1))
  fi

  if [[ -z "$project_number" ]]; then
    echo "      (no board)"
  elif grep -qx -- "$number" <<< "$on_board"; then
    echo "      = already on the board"
  else
    run gh project item-add "$project_number" --owner "$owner" --url "$repo_url/issues/$number"
    echo "      + added to the board"
  fi
done 3< "$PLAN"

echo "Done: $created created, $skipped already existed."
