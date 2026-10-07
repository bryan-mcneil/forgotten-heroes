# docs — guides, journal, decisions, reports

Hand-written documents *about* the project as it is built. Compare `knowledge/` (the research and design
the plan came from, fixed after planning) and `plan/` (the step-by-step instructions).

## What lives here

| File or folder | What it is | Lands in |
|---|---|---|
| [setup.md](setup.md) | Set up a machine, then prove it with `scripts/check-env.sh` | Step 01 |
| [journal.md](journal.md) | Learning journal: one paragraph per session and every step's "What I learned" | Step 01 |
| [project_goal.md](project_goal.md) | The original brief. Superseded by `PLAN.md` and `knowledge/`; kept for history | Step 03 |
| `aws-account-setup.md` | Console walk-through of the account hardening (MFA, Identity Center, budgets) | Step 06 |
| `reviews/phase-N.md` | The phase-end review, one file per phase | end of each phase |
| `balance/` | Simulator balance reports worth keeping | Step 70 |
| `adr/` | Architecture decision records, one short file each, copied from `knowledge/architecture.md` §13 | Step 69 |
| `runbook.md`, `release.md`, `ci-cd.md`, `architecture.md` | How to operate, release and deploy; diagrams in Mermaid | Phase 5 |
| `security.md`, `licences/`, `costs/` | Audit results, licence verification, cost receipts | Steps 74 and 77 |

## What must NOT live here

- Rules, research or design still being decided. Those stay in `knowledge/`; a confirmed decision gets an
  ADR row in `knowledge/architecture.md` §13 first and a file in `adr/` later.
- Step instructions or status. `plan/` holds the instructions and `PLAN.md` holds the checkboxes.
- Generated output such as coverage reports, build logs or the exported `openapi.json`. The build makes
  those; git does not keep them.

## The knowledge base

Every file in [`knowledge/`](../knowledge/), and the question it answers:

- [super_auto_pets_knowledge.md](../knowledge/super_auto_pets_knowledge.md) — how Super Auto Pets works:
  rules, roster, battle algorithm, internal architecture
- [tech_stack_research.md](../knowledge/tech_stack_research.md) — does AWS serverless + Spring Boot + React
  fit this game? Versions, choices, risks
- [cost_estimator.md](../knowledge/cost_estimator.md) — what it costs (≈ $0/month), the Free plan, the
  guardrails that keep it there
- [game_design_document.md](../knowledge/game_design_document.md) — the rules of Forgotten Heroes, 36 heroes,
  16 relics, the ability DSL (authoritative)
- [architecture.md](../knowledge/architecture.md) — system design, API, DynamoDB table, CI/CD, security,
  15 ADRs
- [asset_inventory_and_pipeline.md](../knowledge/asset_inventory_and_pipeline.md) — what art and audio
  exists, what ships, palette conversion, atlases, licensing
- [ux_visual_sound_design.md](../knowledge/ux_visual_sound_design.md) — screens, inputs, visual language,
  motion, sound, accessibility, budgets
- [quality_and_testing_strategy.md](../knowledge/quality_and_testing_strategy.md) — test pyramid, scenario
  and property tests, the simulator as QA, definition of done
- [project_management.md](../knowledge/project_management.md) — board, issues, the PR ritual, commit
  conventions, cadence, teaching format
- [environment_audit.md](../knowledge/environment_audit.md) — what is installed on the dev machine
- [learning_path.md](../knowledge/learning_path.md) — what each phase teaches and the interview answers it
  produces
- [glossary_for_beginners.md](../knowledge/glossary_for_beginners.md) — every term, in plain words
- [open_questions.md](../knowledge/open_questions.md) — what was asked and what was answered (all 30 logged)
