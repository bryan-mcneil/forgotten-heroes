# Learning journal

One paragraph per session, in Bryan's words (grammar tidied, meaning untouched). Each step's
"What I learned" sign-off sentence lives here too, so it survives outside the pull request.

## 2026-10-07 — Step 01, Toolchain

Verified the toolchain and wrote `scripts/check-env.sh` and `docs/setup.md`. What I learned: be aware
of version differences between local and production environments, especially when using services like
AWS. Also be aware that tools can find a JDK through different paths: `java` on the PATH and
`JAVA_HOME` can point to different things and cause confusion when debugging. If local Java were 26 and
Lambda ran 25, everything would work locally and break on Lambda, and we would wonder why.

## 2026-10-07 — Step 02, Create the repository

Turned the folder into a Git repository on `main`, with the `Super Auto Pets/` reference copy ignored
before the first `git add`, and pushed it to GitHub as `bryan-mcneil/forgotten-heroes`. What I learned:
always add ignore rules before the first commit; a file that is committed and untracked later is in the
history forever. `.gitattributes` can force LF for every file, which keeps the files consistent across
machines, but some Windows-only files such as `.cmd` and `.ps1` need CRLF, so know which files those are.

## 2026-10-07 — Step 03, Monorepo skeleton

Created the monorepo skeleton on the first step branch and pull request: twelve folders, each with a
README that says what lives there, what must not, and how to run it; the brief moved to
`docs/project_goal.md`. What I learned: some of the folder structure we will be using — rules data lives
in the engine, and the tools pipeline handles the assets, which live in a private S3 bucket. A new hero's
JSON goes in `engine/src/main/resources/content/`; its sprite frames are never committed, the pipeline
builds them into the gitignored `frontend/public/assets/`.

## 2026-10-08 — Step 04, Project management on GitHub

Put the whole plan on GitHub: labels, one milestone per phase, issue and PR templates, the "Forgotten
Heroes" board, and 77 issues made by a script from `PLAN.md`. Did the first label, milestone and issue by
hand with `gh`, then ran the scripts. A short dash typed by hand instead of the long dash in the plan made one
duplicate issue and one duplicate milestone; the script matches titles letter for letter. What I learned: a
little about GitHub issues and tracking their status on the project board, and some `gh` terminal commands.

## 2026-10-14 — Step 05, CI skeleton & branch protection

Added the first GitHub Actions workflow: a `docs` job that lints every Markdown file and checks the links
in `README.md` and `PLAN.md` on every PR and every push to `main`. Locked `main` with branch protection
(the `docs` check is required, admins included, squash merges only, no force pushes) and proved it by
pushing an empty commit that was rejected. What I learned: the three parts of GitHub Actions in detail. A
workflow says when and what to run, in YAML. A job is one unit of work on its own virtual machine, the
runner. Steps run one after the other on that machine, choosing which commands to run and in which order.

## 2026-10-14 — Step 06, AWS account hardening and budgets

Locked the AWS account: MFA on the root user, an everyday IAM user `bryan` with MFA and no access keys, and the
terminal signed in through the browser with `aws login`. The plan's IAM Identity Center had to wait: enabling it
creates an AWS Organization, which ends the Free plan and its credits (Q32). Built the $5 and $20 budgets by hand
in the console, then as `infra/budgets.yaml`, and deleted the hand-made ones. Along the way Cost Explorer showed
two forgotten tutorial servers in Oregon eating about $23 of credits a month; terminated the same day. What I
learned: how to create a custom budget with e-mail alerts.
