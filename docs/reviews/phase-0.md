# Phase 0 review — Foundations (Steps 01–06)

Reviewed on 2026-10-14. The checklist comes from `plan/phase_0_foundations.md` ("Phase 0 review") and
`knowledge/quality_and_testing_strategy.md` §10. Phase 0 promised: a verified machine, a public repo with CI and a
board holding all 77 steps, and an AWS account that cannot surprise you with a bill.

## 1. What the phase promised, and the proof

| Promise | Proof on 2026-10-14 |
|---|---|
| Machine verified: `scripts/check-env.sh` green | every row ✔ with Docker Desktop running (Docker itself is first needed in Step 28) |
| Public repo, one squash commit per step | `bryan-mcneil/forgotten-heroes` is public; `main` holds six commits, `step-01` to `step-06`, each with Why / What / Learned lines |
| Board holding all 77 steps | project "Forgotten Heroes": 77 cards, 6 Done, 71 Backlog; issues 6 closed, 71 open |
| CI on every PR, required on `main` | workflow `ci`, job `docs` (Markdown lint + link check); branch protection: required check `docs`, up-to-date branches, admins included, linear history, no force pushes |
| AWS account safe and cheap | root MFA on; IAM user `bryan` with MFA and no access keys; `aws sts get-caller-identity` → `user/bryan`; stack `forgotten-heroes-budgets` with the $5 and $20 budgets; Free plan ends 27 Feb 2027, reminder set for 13 Feb 2027; two tutorial servers that were spending credits are gone |
| Clean-clone demo | `git clone` → `./scripts/check-env.sh` runs → `npx markdownlint-cli2 "**/*.md" "#node_modules"` → 0 issues in 41 files |

Test counts and coverage: not applicable yet. The first tests arrive in Step 07.

The sentence the learning path says you can now say: *"I run a public monorepo with branch protection, CI on
every PR, squash-merged steps, and an AWS account hardened with MFA, a least-privilege CLI identity and budget
alarms."*

## 2. Retrospective

**Went well**

1. *to fill in*
2. *to fill in*
3. *to fill in*

**Change for Phase 1**

1. *to fill in*
2. *to fill in*
3. *to fill in*

## 3. Open questions and risks

- One question was added and answered during the phase: Q32, IAM Identity Center waits for the Paid-plan upgrade
  because enabling it would end the Free plan. Nothing is pending in `knowledge/open_questions.md`.
- The risk register (`knowledge/project_management.md` §9) gained three rows: credits hide spending, console drift,
  and Free-plan limits.
- Plan changes made during the phase are all in the `PLAN.md` changelog (Steps 04, 05 and 06).

## 4. Sign-off

Phase 0 complete. Signed: *to fill in*, 2026-10-14. Tag: `phase-0-complete`.
