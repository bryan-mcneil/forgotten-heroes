# .github — GitHub automation

## What lives here

- `ISSUE_TEMPLATE/` and `pull_request_template.md` (Step 04).
- `workflows/`: `ci.yml` (Step 05, completed in Step 63), `deploy-dev.yml` (Step 64), `deploy-prod.yml`
  (Step 65) and a weekly `security.yml`.
- `dependabot.yml` (Step 05).

## What must NOT live here

- AWS keys or any secret in a file. Deploys authenticate with OIDC (Step 62); the few values that must
  stay private live in GitHub's encrypted settings, not in the repo.
- A workflow that deploys prod from a branch. Prod deploys only from a `v*` tag after a manual approval.

## How do I run it

TODO Step 05: workflows run on every pull request; `gh run list` and `gh run watch` follow them from
the terminal.
