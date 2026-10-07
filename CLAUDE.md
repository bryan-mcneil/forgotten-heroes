# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

Forgotten Heroes: a Super Auto Pets-style auto-battler built to teach Bryan (a beginner) Java/Spring Boot on
AWS serverless plus React, one small reviewable step at a time. It is also a recruiter portfolio piece, so the
public repo and the ≈ $0/month cost matter as much as the game.

**State:** planning is complete; the build runs through 77 steps in `PLAN.md`. There is no code, no `pom.xml`
and no `package.json` until the steps that create them are merged. `project_goal.md` is the original brief and
is superseded by `PLAN.md` + `knowledge/`.

## Start every session the same way

1. Open `PLAN.md`, find the **first unchecked `[ ]` step**. That step's phase is "the phase we are on".
2. Open the matching `plan/phase_N_*.md` and read **only that step** (Goal → You will have → Sensei notes →
   Do → Verify → Commit → Check your understanding). Do not pre-read later steps.
3. Open only the knowledge sections listed for that phase in the table below. Section numbers (`§`) are the
   `##` headings inside each file; read the section, not the file.
4. Before asking Bryan a question, check `knowledge/open_questions.md` — 30 questions are already answered
   there (log at the bottom). Never re-ask one. A genuinely new question is appended to its §F with a log row.

One step at a time: never start the next step before the current PR is merged.

## Which knowledge to read, by phase

Always open, every step: `PLAN.md` · the current step in its phase file · `knowledge/project_management.md`
§2–§3 (ritual, commit format) and §7 (teaching format) · `knowledge/glossary_for_beginners.md` when a term is
new to Bryan (add it there if missing).

At the start of a phase, also read that phase's section of `knowledge/learning_path.md` and the
"Phase-end review checklist" in `knowledge/quality_and_testing_strategy.md` §10.

| Phase (steps) | Read these sections | Consult only when the step points there | Do **not** open |
|---|---|---|---|
| **0 Foundations** (01–06) | `environment_audit.md` (all) · `architecture.md` §2 (repo layout, Step 03) and §10 (CI, Step 05) · `cost_estimator.md` §1 and §5 (Step 06) · `project_management.md` §1, §4, §5 (board/labels, Step 04) | `tech_stack_research.md` §8–§9 if a SAM/OIDC term needs explaining | GDD, SAP knowledge, UX, assets, architecture §3–§9 |
| **1 Game Engine** (07–24) | `game_design_document.md` §2–§7 (the rulebook — **authoritative**) and §8–§9 (bots, balance targets, Steps 21–22) · `architecture.md` §3 (packages, action flow, determinism, events, content) · `quality_and_testing_strategy.md` §1–§3 and §6 | `super_auto_pets_knowledge.md` §2 and §4 **only** to resolve a rule the GDD leaves ambiguous — the GDD wins when they differ | tech stack, cost, UX, assets, architecture §4–§12 |
| **2 Backend** (25–36) | `architecture.md` §4–§7 (API contract, table, end-turn sequence, auth) · `tech_stack_research.md` §2–§5 and §10 (why Spring on Lambda, DynamoDB, Cognito, HTTP API, local loop) · `quality_and_testing_strategy.md` §4 · `cost_estimator.md` §2–§5 (Steps 33–36) | `architecture.md` §9 and §11 for Steps 33–35 (template v1, observability) · `game_design_document.md` §4 only via `GameSession` questions | GDD rules in detail (the engine owns them), SAP knowledge, UX, assets |
| **3 Frontend** (37–56) | `ux_visual_sound_design.md` (all — the spec) · `architecture.md` §8 (folder map) and §3.4 (events the playback engine consumes) · `asset_inventory_and_pipeline.md` §1–§4 (Steps 39–41, 50) · `tech_stack_research.md` §6 · `quality_and_testing_strategy.md` §5 | `game_design_document.md` §5 art column (Step 40 gallery) and §2 (player-facing words) · the exported `openapi.json` and engine event catalogue from Steps 26/24 | infra sections, cost, SAP knowledge, backend internals beyond the OpenAPI file |
| **4 Studio** (57–60) | `game_design_document.md` §11 · `asset_inventory_and_pipeline.md` §6 · `ux_visual_sound_design.md` §1 | — | Everything else. **Deferred until Bryan supplies the corgi art (Q14)**; may ship after launch |
| **5 Infra & CI/CD** (61–69) | `architecture.md` §9–§12 and ADR-15 · `cost_estimator.md` §5 (guardrails — every resource must be free-tier) · `tech_stack_research.md` §7–§9 · `quality_and_testing_strategy.md` §7 | `ux_visual_sound_design.md` §11 (blocked-page copy, Step 61) · `cost_estimator.md` §1 (Free plan → Paid, Step 67 kill switch) | GDD, SAP knowledge, assets, UX beyond §11 |
| **6 Balance & Launch** (70–77) | `game_design_document.md` §9–§10 (Step 70) · `quality_and_testing_strategy.md` §6 and §10 · `asset_inventory_and_pipeline.md` §5 (licences, Step 74) · `cost_estimator.md` §1 and §4 (Step 77) · `learning_path.md` "Questions recruiters ask" (Step 76) | `ux_visual_sound_design.md` §8–§9 (Step 75 audit) · `architecture.md` §11–§12 (Steps 73–74) | SAP knowledge, tech stack research |

Never read into code: the `Super Auto Pets/` folder is a reference copy of the real game (gitignored in
Step 02, deleted in Step 77). `knowledge/super_auto_pets_knowledge.md` already holds everything we take from
it; use that file for rule lookups, not the folder.

## Commands

Nothing is runnable yet. Each command below exists only once the step that introduces it is merged; the
step's **Verify** block is the source of truth for the exact invocation.

| Lands at | Command | What it does |
|---|---|---|
| Step 01 | `scripts/check-env.sh` | prints the toolchain table (Java, Node, Docker, AWS CLI, SAM, ffmpeg, Python + Pillow, gh) |
| Step 05 | `npx markdownlint-cli2 "**/*.md" "#node_modules"` | docs lint, the only CI check before code exists |
| Step 07 | `./mvnw -B -ntp verify` · `./mvnw -pl engine -B -ntp verify` | all Java modules / engine only, with JaCoCo gate (≥ 90 % by Step 23) |
| Step 07 | `./mvnw -pl engine -Dtest=BandTest test` | one test class (standard Maven Surefire) |
| Step 22 | `./mvnw -pl sim exec:java -Dexec.args="run --games 1000"` · `scripts/sim.sh` | balance simulator (`run`, `fuzz`, `replay`, `check`) |
| Step 25 | `./mvnw -pl backend spring-boot:run -Dspring-boot.run.profiles=local` | API on :8080; `local` profile accepts `X-Dev-User` instead of a Cognito JWT |
| Step 28 | `docker compose up -d && ./mvnw -pl backend -am verify` | DynamoDB Local + Testcontainers-backed backend tests |
| Step 33–34 | `sam validate --lint` · `sam local invoke ApiFunction -e infra/events/health.json` · `scripts/deploy-backend.sh dev` | template lint, local Lambda, manual dev deploy |
| Step 37 | `cd frontend && npm run dev` · `npm test` · `npx vitest run src/<path>.test.ts` | Vite on :5173 (proxies `/api` → :8080), Vitest all / one file |
| Step 39 | `python tools/assets/build.py --extract` | asset pipeline (Pillow); assets are pulled from a private S3 bucket, never committed |
| Step 42 | `npm run api:types && git diff --exit-code src/api/schema.d.ts` | regenerate the typed client from `openapi.json`; CI fails on drift |
| Step 56 | `npm run e2e` · `npm run lighthouse` | Playwright E2E, Lighthouse CI budgets |
| Step 61+ | `scripts/deploy-frontend.sh dev` · `scripts/teardown.sh dev\|prod\|all` · `scripts/unkill.sh <env>` | site deploy, delete a stack, re-enable after the cost kill switch |

Shell is **Git Bash**; scripts are `.sh`; show commands once, in bash.

## Architecture in one screen

Full detail: `knowledge/architecture.md` (15 ADRs in §13). The parts that span modules:

- **Engine owns the rules, and only the engine.** `engine/` is pure Java (`com.forgottenheroes.engine`), no
  Spring, no I/O. Flow: `Action → Resolver.validate → Resolver.apply → Mutators → GameEvents`, with
  `TriggerDispatcher`/`AbilityInterpreter` firing abilities from JSON content. **Mutators are the only code
  that changes `RunState`.** The backend calls it only through the `GameSession` facade; the frontend never
  recomputes a rule.
- **Determinism is a hard rule (ADR-4).** Seeds derive from the run seed per turn/battle; `Math.random()`,
  `new Random()` and `System.currentTimeMillis()` are forbidden in the engine and ArchUnit enforces it. Same
  seed + same actions must give a byte-identical event log.
- **Events are the contract.** ~30 sealed `GameEvent` types (architecture §3.4) carry before/after values. The
  backend stores and returns them; the frontend's playback engine (Step 49) turns them into a timeline. Content
  (`heroes.json`, `relics.json`, `tokens.json`) is served by `GET /api/content`, so the client has no rules data.
- **Backend = one Spring Boot 4.1 Lambda (Lambdalith, SnapStart, Java 25)** behind an HTTP API with a Cognito
  JWT authorizer. Server-authoritative per-action API; optimistic locking via `version` → 409 and the client
  reloads. Errors are RFC 9457 `ProblemDetail` with a `code`; the API uses 400/401/404/409/422 and **never 403**
  (403 is reserved for CloudFront's geo-block, see below).
- **DynamoDB single table** `forgotten-heroes-{env}` with TTL; matchmaking is asynchronous "ghost" bands
  (architecture §4.4, §5, §6).
- **One front door.** CloudFront serves the React app from a private S3 bucket (OAC) and proxies `/api/*` to
  the HTTP API with an `x-origin-verify` header; the raw API URL answers 403. US-only geo-restriction stays on
  even for VPN users (Q27): CloudFront 403 → static `blocked.html`, SPA fallback on 404 only.
- **Infra is SAM** (`infra/template.yaml`, two stacks `dev`/`prod`), deployed by GitHub Actions via OIDC. Push
  to `main` deploys dev; a `v*` tag deploys prod after manual approval. After Step 65 nothing in prod is ever
  changed by hand.

## Constraints that are decisions, not derivable from code

- **$0/month.** No fixed-cost AWS resources: no WAF, no NAT/VPC, no Route 53 hosted zone, no Lambda Provisioned
  Concurrency (the "avoid" list is `cost_estimator.md` §2; the guardrails in §5 are part of the plan). The AWS account is on the credit-based Free plan; it must be
  upgraded to Paid two weeks before the credits expire (≈ Feb/Mar 2027, Q26).
- **Art and audio never enter git (ADR-13).** Time Fantasy/Elements sprites and royalty-free tracks live in a
  private S3 bucket; the repo is public. Palette is Elements everywhere (ADR-14); conversions are authored in
  the Forgotten Kanji toolkit, not here.
- **Scope is frozen for v1.0 (Q7):** 36 heroes (six Forgotten Kanji cameos), 16 relics, Arena mode only.
  Private rooms later as a same-seed challenge link (Q29); no Versus. GDD §10 lists what is out.
- **Plan changes are logged, never silent.** Split/re-order/mark-optional is allowed inside a phase; record it
  in `PLAN.md` → "Changelog of the plan". Decisions that change the design get an ADR row in architecture §13.

## How to work with Bryan

Bryan's goal is to learn as much as to ship (Java/Spring/React/AWS self-rated 1, Git 3, SQL/NoSQL 4). Every
step is taught in the Sensei format (`project_management.md` §7): **What / Why / How / Where**, then 1–3
"Check your understanding" questions. Explain Java, Spring, React and AWS from zero; be terse on Git; teach
DynamoDB as differences from SQL. Walk through a new tool by hand with him the first time before handing him
a script. **AWS is console-first (Q31):** every step that creates AWS resources (06, 34, 61, 62, 66, 67, 68)
starts with a numbered click-by-click walk-through of the AWS web console on `dev`, which you write in full
when the step arrives, never earlier (the console UI drifts); the SAM/CloudFormation code then recreates the
resource and the hand-made copy is deleted. Prod is never built by hand. Keep each step's diff to one concept. A step is done only when its Verify passes locally and in CI
**and** Bryan has written the one-sentence "What I learned" in the PR — that sentence is the sign-off.

Branch `step-NN-slug`, PR titled `Step NN — Title`, squash-merge to exactly one commit `step-NN: title (#issue)`
with Why/What/Learned lines (format in `project_management.md` §3). Flip the `PLAN.md` checkbox in the same PR.
