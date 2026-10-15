# .github — GitHub automation

## What lives here

- `ISSUE_TEMPLATE/` (Step 04): three issue forms, `step.yml`, `bug.yml`, `idea.yml`, plus `config.yml`, which
  turns blank issues off and links to `PLAN.md`. GitHub shows them as a chooser when you click *New issue*.
- `pull_request_template.md` (Step 04): pre-fills every PR body with the checklist and the *What I learned* line.
- `workflows/`: `ci.yml` (Step 05; `java` job Step 07; completed in Step 63), `deploy-dev.yml` (Step 64), `deploy-prod.yml`
  (Step 65) and a weekly `security.yml`.
- `dependabot.yml` (Step 05).

## What must NOT live here

- AWS keys or any secret in a file. Deploys authenticate with OIDC (Step 62); the few values that must
  stay private live in GitHub's encrypted settings, not in the repo.
- A workflow that deploys prod from a branch. Prod deploys only from a `v*` tag after a manual approval.

## How do I run it

Templates need no running: GitHub reads them from this folder on `main`. The labels, milestones, board and
issues they refer to are created by `scripts/create-labels.sh`, `scripts/create-milestones.sh` and
`scripts/create-issues.sh` (Step 04).

`workflows/ci.yml` runs by itself on every pull request and every push to `main`. From the terminal,
`gh pr checks` shows the result on the current PR, and `gh run list` / `gh run watch` follow a run.
It has two jobs, `docs` and `java`, and both are required checks on `main`. The same commands run locally:

```bash
npx markdownlint-cli2 "**/*.md" "#node_modules"
npx markdown-link-check --config .markdown-link-check.json README.md PLAN.md
./mvnw -B -ntp verify
```

Branch protection on `main` (Step 05) is applied from `infra/github/branch-protection.json`:

```bash
gh api -X PUT repos/:owner/:repo/branches/main/protection --input infra/github/branch-protection.json
```
