# Forgotten Heroes

An auto-battler in the spirit of *Super Auto Pets*, set in a warm fantasy tavern where forgotten heroes —
including six old friends from *The Forgotten Kanji* — sign up for one more run. Built to demonstrate
**Java/Spring Boot on AWS serverless** and **React**, and to teach the author the whole stack from
scratch, one small reviewable step at a time. A Corgi Space Cadet production.

> Status: **Phase 0 (Foundations) in progress.** The plan is [`PLAN.md`](PLAN.md) (77 steps, 7 phases);
> its checkbox list is the source of truth for what is done. The research and design behind it are in
> [`knowledge/`](knowledge/). Bryan's answers to the planning questions are logged in
> [`knowledge/open_questions.md`](knowledge/open_questions.md) and already folded into every file.

## Start here
1. [`PLAN.md`](PLAN.md) — the map: every step, its status, and a link to its instructions.
2. [`plan/`](plan/) — one file per phase with the full instructions for every step.
3. [`docs/setup.md`](docs/setup.md) — set up a machine, then prove it with `./scripts/check-env.sh`.
4. [`knowledge/open_questions.md`](knowledge/open_questions.md) — what was asked and what was answered (all 30 logged).

## Decisions at a glance
| Topic | Decision |
|---|---|
| Name · repo · package | Forgotten Heroes · `bryan-mcneil/forgotten-heroes` (public) · `com.forgottenheroes` |
| URL | `https://heroes.bryanmcneil.pro` (CloudFront, US-only — VPN exits abroad are blocked too and see a "US only" page); studio page later at `corgi.bryanmcneil.pro` |
| Stack | Java 25 + Spring Boot 4.1 on Lambda (SnapStart) · HTTP API + Cognito · DynamoDB · React 19 + Vite 8 · SAM + GitHub Actions |
| Cost | ≈ $0.03/month; Free-plan credits until ≈ Feb/Mar 2027, then budgets + a cost kill switch |
| Art | Time Fantasy / Elements by FinalBossBlues, converted to the Elements palette; 36 heroes, 16 relics |
| Shell | Git Bash; every script is `.sh` |

## The knowledge base
| File | What it answers |
|---|---|
| [super_auto_pets_knowledge.md](knowledge/super_auto_pets_knowledge.md) | How SAP works — rules, roster, battle algorithm, and its internal architecture (from the game's own symbol table) |
| [tech_stack_research.md](knowledge/tech_stack_research.md) | Does AWS serverless + Spring Boot + React fit this game? Versions, choices, risks |
| [cost_estimator.md](knowledge/cost_estimator.md) | What it costs (≈ $0/month), the Free plan, and the guardrails that keep it there |
| [game_design_document.md](knowledge/game_design_document.md) | Forgotten Heroes rules, 36 heroes (six cameos), 16 relics, ability DSL |
| [architecture.md](knowledge/architecture.md) | System design, API, DynamoDB table, CI/CD, security, 15 ADRs |
| [asset_inventory_and_pipeline.md](knowledge/asset_inventory_and_pipeline.md) | What art/audio exists, what ships, palette conversion, atlases, licensing |
| [ux_visual_sound_design.md](knowledge/ux_visual_sound_design.md) | Screens, inputs (mouse + keyboard), visual language, motion, sound, accessibility, budgets |
| [quality_and_testing_strategy.md](knowledge/quality_and_testing_strategy.md) | Test pyramid, scenario/property tests, simulator as QA, definition of done |
| [project_management.md](knowledge/project_management.md) | Board, issues, PR ritual, commit conventions, cadence, teaching calibration |
| [environment_audit.md](knowledge/environment_audit.md) | What is installed on the dev machine (everything, as of 7 Oct 2026) |
| [learning_path.md](knowledge/learning_path.md) | What you learn in each phase and the interview answers it produces |
| [glossary_for_beginners.md](knowledge/glossary_for_beginners.md) | Every term, in plain words |

## Licence
Code is MIT. Art and audio are proprietary (Time Fantasy by FinalBossBlues and the author's own) and
are not included. See [`LICENSE`](LICENSE) for the code licence. Art and audio live in a private bucket and are
published from there at build time; the audio comes from royalty-free sources (DRAGON-STUDIO, Helton Yan,
Freesound_Community, OxidVideos and HorrorSFXFree). Every creator is named in [`CREDITS.md`](CREDITS.md).
