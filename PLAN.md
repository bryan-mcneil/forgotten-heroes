# Forgotten Heroes — The Build Plan (77 steps, 7 phases)

> **How to use this file.** This is your map. Each row below is one step = one branch = one PR =
> one squash commit on `main`. Click the phase file for the full instructions of each step
> (goal, Sensei notes, exact commands, how to verify, review checklist). Flip the checkbox when
> the PR is merged. If you ever feel lost: come back here, find the first unchecked box, open its
> phase file, read only that step.

**Legend:** `[ ]` not started · `[~]` in progress · `[x]` done · `[opt]` optional (skipping still ships a complete game) · `est` = rough hours for a beginner working with Claude.

**Rules of the road** (details in `knowledge/project_management.md`):
1. One step at a time. Never start the next before the current PR is merged.
2. Every step ends with **Verify** commands that must pass locally and in CI.
3. You write one sentence in the PR about what you learned — that is the sign-off.
4. Steps may be split or re-ordered inside a phase; changes are logged at the bottom of this file.
5. Shell: **Git Bash** (Q18). Every command in the phase files is bash; scripts are `.sh`.
6. AWS is **console-first** (Q31): every AWS resource is built by hand in the web console on `dev`, following a
   click-by-click walk-through written when the step arrives; then the template recreates it as code. Prod is code-only.

---

## The map

```text
Phase 0  Foundations ──► Phase 1  Game Engine (Java) ──► Phase 2  Backend (Spring Boot on Lambda)
  tools, repo, CI,         rules, battles, bots,           API, DynamoDB, auth, Lambda, first deploy
  AWS account, budgets     simulator, 90 % tests
                                                                     │
Phase 6  Balance & Launch ◄── Phase 5  Infra & CI/CD ◄── Phase 4  Studio ◄── Phase 3  Frontend (React)
  tuning, perf, security,       CloudFront, OIDC,            Corgi Space       tavern, battle playback,
  a11y, README, v1.0.0          dev/prod, alarms             Cadet intro       sound, keyboard, E2E
```

| Phase | Steps | What exists at the end | Est. hours | Detail file |
|---|---|---|---|---|
| 0 Foundations | 01–06 | tooling verified, public repo with CI, board with all 77 issues, hardened AWS account with budgets | 8 | [plan/phase_0_foundations.md](plan/phase_0_foundations.md) |
| 1 Game Engine | 07–24 | a complete, tested, deterministic rules engine + simulator you can run from the terminal — **done 2026-10-14**, review in [docs/reviews/phase-1.md](docs/reviews/phase-1.md) | 55 | [plan/phase_1_engine.md](plan/phase_1_engine.md) |
| 2 Backend | 25–36 | the API running on Lambda in `dev`, saving runs to DynamoDB, protected by Cognito | 40 | [plan/phase_2_backend.md](plan/phase_2_backend.md) |
| 3 Frontend | 37–56 | the playable game in the browser against the dev API, with sound, keyboard, tests | 75 | [plan/phase_3_frontend.md](plan/phase_3_frontend.md) |
| 4 Studio | 57–60 | Corgi Space Cadet intro, landing page at `corgi.bryanmcneil.pro`, PWA/meta — **deferred until the corgi art arrives (Q14); may follow launch** | 10 | [plan/phase_4_studio.md](plan/phase_4_studio.md) |
| 5 Infra & CI/CD | 61–69 | push-to-deploy dev, tag-to-deploy prod with approval, `heroes.bryanmcneil.pro`, US-only edge, cost kill switch, alarms, teardown, runbook | 26 | [plan/phase_5_infra_cicd.md](plan/phase_5_infra_cicd.md) |
| 6 Balance & Launch | 70–77 | tuned content, perf/security/a11y audits, recruiter README, **v1.0.0 live** | 25 | [plan/phase_6_launch.md](plan/phase_6_launch.md) |
| | | **Total** | **≈ 240 h** (≈ 210 h without `[opt]` steps) — at your 6–7 h/week ≈ 9 months; fastest path ≈ 7–8 months | |

---

**Fastest path to live** (Q7 — keep it simple, ship fast): Phases 0–3, 5 and 6 without the ten `[opt]`
steps; Phase 4 waits for the corgi art (Q14) and can ship as 1.1 after launch. ≈ 210 h ≈ 7–8 months at
your 6–7 h/week (Q17). Every decision from your 7 Oct 2026 answers is logged in `knowledge/open_questions.md`.

## Phase 0 — Foundations
- [x] **01** Toolchain: verify Corretto 25, AWS CLI, SAM CLI, ffmpeg, VS Code extensions (installed 7 Oct 2026); `scripts/check-env.sh`, `docs/setup.md` — est 1h
- [x] **02** Create the public GitHub repo `forgotten-heroes`; root files (README, LICENSE, .gitignore, .editorconfig, .gitattributes); gitignore the `Super Auto Pets/` reference copy — est 1h
- [x] **03** Monorepo skeleton: `engine/ backend/ sim/ frontend/ infra/ tools/ studio/ docs/ knowledge/ plan/` with READMEs — est 1h
- [x] **04** Project management: labels, milestones, issue/PR templates, Project board, script that creates all 77 issues — est 1.5h
- [x] **05** CI skeleton (`ci.yml` with docs lint), Dependabot, branch protection on `main` — est 1h
- [x] **06** AWS account hardening: root MFA, IAM user `bryan` + MFA (no access keys), `aws login` profile `fh`, budgets template ($5/$20), cost tag; Identity Center waits for the Paid-plan upgrade (Q32) — est 1.5h

## Phase 1 — Game Engine (pure Java)
- [x] **07** Maven parent + wrapper (script-only zip, no global Maven) + `engine` module; JUnit, AssertJ, JaCoCo gate; first test — est 2h
- [x] **08** Core value types: `Stats`, `Rank`, `Level/Xp`, `Side`, `Position`, `UnitId`, `Perk` (records, sealed types) — est 2h
- [x] **09** Deterministic RNG: `GameRandom`, seed derivation per turn/battle, reproducibility tests — est 2h
- [x] **10** Content model + JSON loader + schema validation; rank I heroes & relics in `heroes.json` — est 3h
- [x] **11** Board model: `Unit`, `Band` (5 slots), `Tavern`, `RunState`; invariants — est 3h
- [x] **12** Event model: sealed `GameEvent` family, `EventLog`, JSON serialization, golden test — est 2h
- [x] **13** Tavern actions I: `StartTurn` (gold, rank schedule, fill), `Roll`, `Freeze`/`Unfreeze` + `TavernResolver` — est 3h
- [x] **14** Tavern actions II: `Buy`, `Sell`, `Reorder`, `Merge` + leveling/XP + level-up bonus slot — est 4h
- [x] **15** Relics: instant effects, perks, XP, tavern-wide buffs, `EAT_RELIC` — est 3h
- [x] **16** Trigger system: `TriggerDispatcher`, ordering rule, limits, `AbilityInterpreter` for tavern triggers — est 4h
- [x] **17** Battle resolver I: setup, start-of-battle, clash loop, hurt/knockout/fall, shifting, result, clash cap — est 4h
- [x] **18** Battle resolver II: summons in battle, before/after attack, ally triggers, all 9 perks — est 4h
- [x] **19** Full MVP content: 36 heroes, 7 tokens, 16 relics + one scenario test per hero/relic — est 5h
- [x] **20** `GameSession` facade: start run, apply action, end turn vs ghost, run end; full-run test — est 3h
- [x] **21** Bots: `RandomBot`, `GreedyBot`; a bot can finish a run — est 2h
- [x] **22** `sim` module: CLI (`run`, `fuzz`, `replay`, `check`), Markdown/JSON balance report — est 4h
- [x] **23** Property tests (jqwik), ArchUnit rules, coverage ≥ 90 % gate, PIT `[opt]` — est 3h
- [x] **24** Engine README (rules as implemented, event list, DSL reference) + **Phase 1 review** — est 2h

## Phase 2 — Backend (Spring Boot 4.1, Java 25, Lambda)
- [ ] **25** `backend` module from Spring Initializr; `/api/health`; runs locally; MockMvc test — est 2h
- [ ] **26** API design: DTO records, validation, ProblemDetail errors, springdoc OpenAPI, exported `openapi.json` — est 3h
- [ ] **27** Service layer + in-memory repositories; `RunService` drives the engine; service tests — est 3h
- [ ] **28** DynamoDB: table definition, Enhanced Client repositories, Testcontainers tests, `docker-compose.yml` + `local` profile — est 4h
- [ ] **29** Optimistic locking (`version`) + conditional writes + 409 handling; concurrency test — est 2h
- [ ] **30** Matchmaking: ghost writes with TTL, similar-crowns query, bot & mirror fallbacks — est 3h
- [ ] **31** Auth: Spring Security resource server (Cognito JWT), `local` dev-user header, ArchUnit guard — est 3h
- [ ] **32** Leaderboard + profile (display name) endpoints — est 2h
- [ ] **33** Lambda handler (serverless-java-container), shaded jar, SnapStart priming, `infra/template.yaml` v1 — est 4h
- [ ] **34** First manual deploy to `dev` with SAM; smoke test with curl; `samconfig.toml` — est 2h
- [ ] **35** Observability: JSON logs, request summary line, EMF metrics, log retention, 4 alarms — est 3h
- [ ] **36** Hardening: CORS, payload limits, stage throttling, security headers, Dependabot for Maven + **Phase 2 review** — est 2h

## Phase 3 — Frontend (React 19, Vite 8, TypeScript)
- [ ] **37** Vite + React + TS scaffold; ESLint/Prettier; Vitest + RTL; folder structure; `/api` proxy — est 2h
- [ ] **38** Theme: Tailwind v4 tokens, fonts, pixel utilities, app shell, dark tavern look — est 3h
- [ ] **39** Asset pipeline I (Python): inventory + extract SV heroes, monsters, icons, FX → normalized frames — est 4h
- [ ] **40** `Sprite` component + `/dev/gallery`; confirm every hero's art pick; update GDD art column — est 3h
- [ ] **41** Asset pipeline II: atlas packing, manifest with hashes, private S3 bucket publish/pull scripts — est 3h
- [ ] **42** Typed API client: `openapi-typescript`, fetch wrapper (auth, 409 retry), MSW mocks — est 3h
- [ ] **43** `RunStore` (Zustand) + content loading hooks + tests — est 3h
- [ ] **44** Auth UI: Cognito Managed Login with PKCE, protected routes, dev-auth mode for local — est 3h
- [ ] **45** Title/Home screens, React Router routes, settings drawer skeleton — est 3h
- [ ] **46** Tavern screen layout: `HeroCard`, `RelicCard`, `BandRow`, `TavernRow`, HUD (mock data) — est 5h
- [ ] **47** Tavern interactions: dnd-kit drag/drop + click-select; wired to the API; pending/409 handling — est 5h
- [ ] **48** Keyboard controls + accessibility pass (focus, ARIA names, live region, reduced motion) — est 4h
- [ ] **49** Battle playback engine (pure TS): events → timeline → view-model snapshots; unit tests — est 4h
- [ ] **50** Sound: Howler manager, audio sprite build script (ffmpeg), SFX/BGM mapping, volume settings — est 4h
- [ ] **51** Battle screen rendering: sprites, damage numbers, FX, callouts, speed/skip; `/dev` replay & inspector — est 6h
- [ ] **52** Results & progression: win/loss/draw banner, hearts/crowns animation, run-over screen, continue — est 3h
- [ ] **53** `[opt]` Juice & polish: Motion transitions, coin flyouts, toasts, skeletons, error recovery — est 4h
- [ ] **54** `[opt]` Leaderboard + profile screens; display-name picker — est 3h
- [ ] **55** `[opt]` Onboarding callouts, How-to-play page, responsive rules (2× scale, rotate prompt) — est 3h
- [ ] **56** Frontend QA: Playwright E2E (dev-seed fixtures), axe, visual snapshots, Lighthouse CI budgets + **Phase 3 review** — est 5h

## Phase 4 — Studio: Corgi Space Cadet `[opt]` — deferred until the corgi art arrives (Q14)
- [ ] **57** `[opt]` Brand kit: placeholder corgi frames, logo type, palette; `studio/brand.md` — est 2h
- [ ] **58** `[opt]` Intro animation component (skippable, once per session, sound) — est 3h
- [ ] **59** `[opt]` Studio landing page in `studio/site/`, deployed to Hostinger via Git deploy — est 3h
- [ ] **60** `[opt]` Favicons, Open Graph image, PWA manifest, titles — est 2h

## Phase 5 — Infrastructure & CI/CD
- [ ] **61** Complete SAM template: site bucket + CloudFront (OAC, SPA fallback, headers, `/api/*` → HTTP API, US-only geo-restriction with a `blocked.html` page, origin-verify header), Cognito callback URLs, params/outputs; `deploy-frontend` script — est 4h
- [ ] **62** GitHub OIDC: IAM roles for dev & prod (`infra/github-oidc.yaml`), trust limited to repo/branch/tag — est 3h
- [ ] **63** `ci.yml` complete: backend (Docker), frontend, lint, coverage, contract, optional e2e; caches — est 3h
- [ ] **64** `deploy-dev.yml`: on push to main → SAM deploy → build site → S3 sync → invalidation → smoke — est 3h
- [ ] **65** `deploy-prod.yml`: on tag `v*` → GitHub Environment `production` with required reviewer → same steps — est 2h
- [ ] **66** Custom domain `heroes.bryanmcneil.pro`: ACM cert (DNS validation via Hostinger), CloudFront alias, Cognito URLs — est 2h
- [ ] **67** Monitoring & cost guardrails: dashboard, alarms → e-mail, reserved concurrency 10, **cost kill switch** (SNS → Lambda), sign-up alarm, budgets verified, retention — est 4h
- [ ] **68** Data lifecycle: TTL verification, backup/export script, account-delete endpoint, `scripts/teardown` — est 2h
- [ ] **69** Release process: CHANGELOG, semver, `docs/runbook.md`, `docs/architecture.md` (Mermaid), ADR folder + **Phase 5 review** — est 3h

## Phase 6 — Balance, Hardening & Launch
- [ ] **70** Balance pass: 10,000-run simulations, tune stats/abilities to GDD §9 targets, nightly `sim check` — est 4h
- [ ] **71** `[opt]` Content expansion: +18 stretch heroes, 2 new effect types (repeat, swallow), scenarios — est 5h
- [ ] **72** Ghost pool seeding for prod (bot bands for every turn) + matchmaking metrics — est 2h
- [ ] **73** `[opt]` Performance pass: Lambda power tuning, SnapStart priming check, bundle/caching audit, keep-warm decision — est 3h
- [ ] **74** Security & licence audit: gitleaks, dependency check, headers, JWT edge tests, `CREDITS.md`, audio licence verification — est 3h
- [ ] **75** `[opt]` Accessibility & UX audit: axe, keyboard-only playthrough, contrast, reduced motion fixes — est 3h
- [ ] **76** Recruiter docs: README hero section (demo link, GIF, diagram, badges, 5-minute local run), portfolio link — est 3h
- [ ] **77** Launch: tag `v1.0.0`, approve prod, smoke, alarms & budget check, cost receipt, sign-off, post-launch policy, delete the local SAP reference copy — est 2h

---

## Changelog of the plan
| Date | Change | Why |
|---|---|---|
| 2026-10-07 | Plan created (77 steps) | — |
| 2026-10-07 | Renamed to **Forgotten Heroes**: repo `forgotten-heroes`, package `com.forgottenheroes`, folder `forgotten_heroes` | Q5 — matches *The Forgotten Kanji*; same studio |
| 2026-10-07 | Step 01: tools installed during planning (Corretto 25.0.4, AWS CLI 2.37, SAM 1.167, ffmpeg 9.0, 9 VS Code extensions); Maven dropped — Step 07 uses the script-only wrapper zip | `Apache.Maven` is not in winget; the wrapper needs no global Maven |
| 2026-10-07 | Git Bash is the default shell; all scripts are `.sh` | Q18 |
| 2026-10-07 | Step 02: `Super Auto Pets/` is gitignored (not moved); Step 77 deletes it | it is a copy of the real install, reference only |
| 2026-10-07 | Step 06: record Free plan credits/expiry, reminder to upgrade; budget e-mail <your-email> | Q1, Q3 |
| 2026-10-07 | Step 61: CloudFront fronts the API, US-only geo-restriction, origin-verify header; Step 67: cost kill switch + sign-up alarm (ADR-15) | Q4 — US only, DDoS cost fear |
| 2026-10-07 | Step 66 no longer optional: `heroes.bryanmcneil.pro` (studio later at `corgi.bryanmcneil.pro`) | Q2 |
| 2026-10-07 | Phase 4 deferred until the corgi art arrives; may follow launch | Q14 |
| 2026-10-07 | Art: Elements palette everywhere; conversions authored in the Forgotten Kanji toolkit (ADR-14); Step 39 gets the conversion job | Q13 |
| 2026-10-07 | Six Forgotten Kanji cameo heroes: Aoi, Hotaru, Iwao, Kashi, Kaede, Nagi (GDD §5) | Q15 |
| 2026-10-07 | Step 04 walks through `gh` by hand before the script | Q19 |
| 2026-10-07 | Cadence: 6–7 h/week → ≈ 9 months full, ≈ 7–8 months fastest path | Q17 |
| 2026-10-07 | Folder renamed `fantasy_heroes` → `forgotten_heroes`; rule 6 retired | Q30 — done |
| 2026-10-07 | Step 06: the calendar reminder says *upgrade to the Paid plan*, not *decide* | Q26 — default accepted |
| 2026-10-07 | Step 61: SPA fallback on **404 only** (the OAC may `s3:ListBucket`, so S3 answers 404); CloudFront **403 → `blocked.html`** so geo-blocked and VPN visitors see a "US only" page instead of a broken app | Q27 — US-only stays, VPN users included |
| 2026-10-07 | Cameo slots confirmed (GDD §5); private rooms = same-seed challenge link first, live rooms stay a door (GDD §3, architecture §4.4) | Q28, Q29 |
| 2026-10-07 | Console-first for AWS: Steps 06, 34, 61, 62, 66, 67, 68 get a numbered web-console walk-through (written at step time) before the template/script; ≈ 1–2 h extra per step (≈ +10 h, not folded into the phase totals) | Q31 — Bryan wants to set AWS up manually in the GUI and learn it |
| 2026-10-07 | Step 01: the SAM CLI ships only as `sam.cmd`, which Git Bash cannot run by the bare name `sam`; `docs/setup.md` §5 adds a tiny `~/bin/sam` shim and `check-env.sh` shows ✘ until it exists. Step 01 lands before `git init` (Step 02), so its files become the **first** commit on `main` (`step-01: …`) when Step 02 initialises the repo, followed by `step-02: …` | Q18 (Git Bash) · the repo does not exist before Step 02 |
| 2026-10-08 | Step 04: issue titles are the phase-file headings (`Step 04 — Project management on GitHub`) and the `PLAN.md` row goes in the body; milestones get their own `create-milestones.sh`; `create-issues.sh` also re-checks labels, milestone and board card on an issue that already exists and has `--dry-run`; Verify counts with `gh issue list --state all` because the step itself closes 01–03 | board cards must stay readable (the longest row is 240 characters); idempotent means "same end state", not only "skip" |
| 2026-10-14 | Step 05: lint rules live in `.markdownlint-cli2.yaml` (one file; also honours `.gitignore` and skips the PR template snippet); cosmetic rules are off (line length, blank lines around blocks, table style, bold-as-heading, list numbering); the 68 real findings in the planning docs were fixed (bare URLs wrapped, fence languages, two tables, trailing spaces); Verify tests the push rejection with an empty commit | the docs predate the linter; structural rules are the ones worth keeping, and `git push origin main` with nothing to push is not rejected |
| 2026-10-14 | **Steps 10–24 are built by Claude** at Bryan's request (one branch, PR, CI run and squash commit per step, as before), so the AWS phases arrive sooner; the Phase 1 rule "Bryan types every file" is suspended for these steps only and the "What I learned" lines are Claude's teaching summaries, marked as such in the journal | Bryan's main goal is AWS experience (Phases 2 and 5); the engine is the road to it, not the destination |
| 2026-10-14 | Step 24: `engine/README.md` rewritten (rules as implemented with the decisions made while building, package map, generated event catalogue, DSL reference, add-a-hero recipe, reproduce-a-bug recipe); `tools/docs/event_catalog.py` (`--write` / `--check`) generates the catalogue from the Javadoc one-liners in `GameEvent.java`; `docs/reviews/phase-1.md` with the fresh-clone proof (engine 342 tests, 97.8 % lines; sim 9 tests, 93.7 %), the add-a-hero recipe timed on the fresh clone (about 5 minutes; the pinned hero count in `ContentLoaderTest` is the one extra guard, by design), the retro and two new risk rows; the review is prepared by Claude and countersigned by Bryan after reading the Steps 10–24 journal entries; tag `phase-1-complete` on the squash commit. | the README is the hand-over document from the delegated steps back to Bryan |
| 2026-10-14 | Step 23: the five properties run with 150–200 tries each so the suite stays under ~10 s; `RunState` JSON needed `RunStateJson` (DTO records in `engine.json`) and `restore(...)` factories on `RunState`, `Unit`, `Tavern`, `Counters` (the backend's storage format from Step 28); the sim's ArchUnit rule allows `engine.action`, `engine.event`, `engine.json` and `engine.rng` besides the plan's four packages, plus the single class `RuleViolation` (fuzz and replay need actions, events and the dice), and forbids `engine.resolver`/`engine.mutator`; extra engine rules beyond the plan: no `java.net`/`java.nio.file`/`java.sql`, only `GameRandom` touches `SplittableRandom`, the model knows no JSON, gold/hearts/crowns are set only by mutators; the gate went to 90 % with coverage already at 97.9 %; PIT is a `pit` profile (pitest 1.30.1 + junit5 plugin 1.2.3) and not part of `verify`; `ReportMarkdown` writes LF on every platform; **the properties found a bug on their first run**: `Buy` drew the next unit id before the band-full and max-level checks, so a refused purchase still moved the id counter (fixed: validate first, and a merged copy borrows the survivor's id) | properties replay from a printed seed, like everything else in the engine; the restore factories are the one door storage gets into the mutable model |
| 2026-10-14 | Step 22: module `sim` with picocli 4.7.7 and `exec-maven-plugin` 3.6.4 (skipped in the root pom, enabled in `sim`); `run` seeds a ghost pool with `max(10, games/10)` random-bot games, then plays the measured bot against the growing pool, and for the greedy bot adds a 200-game tournament against random-only ghosts for the GDD §9 "greedy vs random" number; `replay --file bug.json` reads `{seed, actions}` (the exact file `fuzz` prints) instead of `--seed`/`--actions`; `check` only judges a hero's win rate after `minBattlesForHeroRates` battles; reports default to `target/sim/` (gitignored) and `scripts/sim.sh` compiles engine + sim quietly before running; the sim's JaCoCo gate is 70 %; first report committed as `docs/balance/2026-10-report.md` | the pool protocol is what makes hero win rates mean "against bands of the same age"; one replay file format shared by fuzz, replay and later the frontend inspector |
| 2026-10-14 | Step 21: `Bot.next(state, content, rng)` returns `EndTurn` to end the turn (no separate signal); the runner counts refusals and asks again, with three safety valves (10 refusals in a row, 60 actions per turn, 60 turns) so a run always finishes; `GhostProvider` is a functional interface (`mirror()`, `nobody()`, `GhostPool`); `RunSummary` keeps one `TurnRecord` per battle (outcome, clashes, the band sent in) plus gold earned per hero, which is what Step 22 aggregates; a mirror ghost almost always draws (identical bands), so the "finishes 50 runs" tests fight a growing pool of bot bands instead and a separate test pins the mirror behaviour; GreedyBot wins ≈ 97 % of battles against random-bot ghosts (the plan asked ≥ 70 %) | a bot run has to be deterministic and finite before it can be a measurement |
| 2026-10-14 | Step 20: `start(runId, runSeed)` returns an `ActionResult` (state **and** the first turn's events), not a bare `RunState`, so the backend can hand the client its opening story; `apply` refuses `StartTurn` and `EndTurn` with `WRONG_PHASE` (the engine owns the turn cycle); `endTurn` returns `TurnResult(state, events, battle, outcome, ghost)` where `ghost` is the player's own snapshot for the backend to store; a ghost's units get ids `g1, g2 …` and keep their fought-with stats (temporary bonuses folded in), XP and perk; an empty ghost is legal and loses at once; the full-run test plays 12 turns against a mirror and proves determinism and contiguous numbering rather than a forced end (a mirror cannot lose); `RunState.toSnapshot()` | the facade's return values are the backend's API shape, so they carry events, not just state |
| 2026-10-14 | Step 19: the DSL needed **no new vocabulary** for the 36 heroes (every GDD §5 row maps onto the ten effects and seventeen selectors; Demon's and Necromancer's limits scale with `Amount`); `ScenarioRunner` (test code) reads the §2 format with tavern scenarios added (`band`, `tavern`, `actions` incl. the synthetic `EndOfTurn`, `expectState`) and matches `expectEvents` as an ordered subsequence by position, hero/token id, `before→after` strings and `stats`; one scenario per hero, relic **and token** (59 files), enforced by a meta-test on file names; the scenarios caught one engine bug (a stat change on the enemy side reported no position: Stone Golem) and two slips in the hand calculations (positions shift after a removal; the Assassin fight was a draw) | content before interpreter would have been wrong on day one; a file per id is the cheapest contract that scales to 36 heroes |
| 2026-10-14 | Step 18: `Damage.attack`/`incoming` carry all nine perks and `Damage.spend` reports the once-only ones *before* the damage event; ability damage in battle now goes through the defender's perks and records `HURT`/`KNOCKOUT` moments (Archer's opener vs Aegis is 0 and spends it); Venom = the hit amount becomes the victim's whole health (the event stays honest); Flame Oil's splash is `DamageKind.SPLASH` from the attacker and can knock out; **a fallen unit still answers `HURT`** (Kaede strikes back as she falls; `KNOCKOUT` still needs a living attacker); summons needed no new code (the Step 16 dispatcher already places them, fails them when full, and puts enemy summons at the enemy front); rank III heroes and the Wolf token added early for the Druid and Assassin tests; three hand-written expectations were wrong on the first run (Fairy Charm's enemy health, Phoenix's clash count, where the Berserker falls) and the engine was right | one damage path for attacks, splashes and abilities is the only way nine perks stay consistent; writing the numbers down first is still worth it even when the hand is the one that is wrong |
| 2026-10-14 | Step 17: `BattleResolver.resolve(home, away, seed, turn, playerSide)` (plus a 3-argument form for tests) reuses the Step 16 `TriggerDispatcher` for every moment and for the settle pass, so the battle has no fall logic of its own; `BattleState` is the battle's `Board` (fresh copies, ids `s1…` for summons); `Damage.attack` / `Damage.incoming` return a `Hit` that already says which once-only perk it spends (Step 18 fills them in); `Knockout` is emitted only when the hit dealt damage; `AFTER_ATTACK` / `ALLY_AHEAD_ATTACKS` fire only for attackers still standing after the falls (GDD §4.5 order); `LineShifted` is emitted per side whose order changed after a dispatch; the plan's 1v1 example ("2/2 vs 3/1 → home survives at 2/1") does not add up and became 2/2 vs 1/2; a battle can end before any clash (Archer's opener on a 4/1 Merchant) | one dispatcher for tavern and battle means a Fall resolves identically in both; the hand-calculated sequences matched on the first run, which is the point of writing them first |
| 2026-10-14 | Step 16: the dispatcher runs **inside** `TavernResolver.apply` (one `dispatch` per action, dice forked `"abilities"`), not as a separate call, because a Fall must fire while the unit still holds its place (Standard Bearer's "allies behind", Necromancer's "its slot"); `PendingTrigger` carries the `Unit` and its `Side`, not an id (a sold Merchant is already off the band when `SELL` fires); `Board` interface with `TavernBoard` (and the battle's state in Step 17) so one dispatcher serves both; `settle()` after every moment processes units at zero health in the §4.6 order (`UnitFell` → own Fall with summons deferred → Fairy Charm / Phoenix Feather → `ALLY_FALL` / `ALLY_AHEAD_FALLS` → removal → deferred summons), so Poison Vial and Alchemist cascades resolve like battle falls; `Kill` just drops a unit to zero; **ability damage in the tavern is permanent** (`Unit.reduceMaxHealth`, as SAP's shop damage); `SUMMON` works in the tavern (`SummonFailed` for enemy-side summons there), `STOCK_RELIC` uses the first empty shelf or an extra one, `SET_HEALTH_PERCENT` is a stat change, not damage; `TavernResolver.fire(state, trigger)` for the session's `END_OF_TURN`; rank II heroes and the Dirty Rat token added early (the tests needed them); guard at 1,000 activations | the GDD's "units that fall during resolution still get their Fall abilities" only works if removal comes after the ability, which means the dispatcher must own removal, which means it must run inside the action |
| 2026-10-14 | Step 15: `EffectApplier` + `EffectContext` + `Targeting` (v1: `SELF`, `CHOSEN`, `TRIGGERING_UNIT`, `ALL_ALLIES`, `RANDOM_ALLY`) live in `resolver`; a relic that picks its own targets refuses a band index (`TARGET_NOT_ALLOWED`) and needs at least one hero unless it only buffs the tavern (`TARGET_REQUIRED`); Royal Decree buffs the shelves now (slot bonus) and `Tavern.permanentBuff` for the future; Tome of Experience is +1 XP = +1/+1 (the Step 14 rule) and opens the bonus slot on a level-up; Poison Vial = `UnitFell` + removal + a recorded `FALL` (Step 16 fires it before removal); `DEAL_DAMAGE`/`GAIN_GOLD` are implemented in the applier already, `SUMMON`/`STOCK_RELIC`/`SET_HEALTH_PERCENT` wait for Steps 16/18; `UnitMutator` gains `givePerk`, `consumePerk`, `damage`, `fell`; 16 relics in `relics.json` | one interpreter over data means the 16 relics are 16 JSON rows and one shared test loop |
| 2026-10-14 | Step 14: merge XP = survivor + copy + 1 (two level-2s make a level 3) and **every XP point is +1/+1** (GDD §4.4 and SAP §2.2 agree on the parts; the combination is now written down), so a hero's XP bonus always equals its XP; a copy's perk is taken only when the survivor has none, the higher temporary bonus of each stat survives; `Buy` onto a copy fires `BUY` on the survivor but not `ALLY_SUMMONED` (merging is not a summon); `Sell` records `SELL` before removal; `Move` onto itself is `INVALID_INDEX`; the level-up bonus slot is drawn with `SeedDerivation.actionSeed(turnSeed, lastEventSeq)` forked `"bonus"`, falls back to "rank ≤ next" while the roster is incomplete, and is announced by a new `BonusSlotOpened` event (34 event types; golden file regenerated); `Unit.permanentStats()`; mutators `BandMutator` (`buy`, `sell`, `move`, `stack`, `canStack`) and `UnitMutator` (`modifyStats`, `gainXp`, `positionOf`) | the plan's "stats = max + 1/1" and "+1 XP" are the single-copy case of the general rule; a bare `LevelUp` event could not tell the client the shelves changed |
| 2026-10-14 | Step 13: `TavernResolver.apply(state, action)` takes no RNG parameter and derives the dice from the state (`rollSeed(turnSeed, rollsThisTurn)` forked `"tavern"`), so a roll is reproducible from the saved state alone; it returns `TavernResult(events, pendingTriggers)` (Step 16 fires them); `StartTurn` is engine-internal (the session applies it; a client sending it will get `WRONG_PHASE` in Step 20) and the automatic refill is roll 0, after which one manual roll is still free (plan wording kept); `EndTurn` in the resolver is a programming error (`IllegalArgumentException`), the session resolves it; `Freeze`/`Unfreeze` take an `ItemKind` (`HERO`/`RELIC`, moved to `model`) and the bonus slot is freezable at index `heroSlots().size()`, closing on the next unfrozen roll and at every start of turn; `RuleViolation.Code` enum + `require` helper; mutators are static utility classes (`GoldMutator`, `TavernMutator`, `RunMutator`), gold gains cap at `MAX_GOLD` | one dice derivation inside the resolver beats passing an RNG around; a bare `int slot` cannot say which shelf it means |
| 2026-10-14 | Step 12: 33 event records nested in `GameEvent` (the §3.4 list minus `UnitHurt`, which `DamageDealt` already says, plus `TurnEnded`, `TavernBuffed`, `RelicStocked`, `UnitRevived`); `RunStarted` carries no seed (a client that knows the seed can predict the shop); type ids are `SIMPLE_NAME`; lists are written through a typed writer so every element keeps its `type`; `EventLog(lastSeq)` continues numbering so a whole run is contiguous, `appendAll` renumbers; shared `engine.json.EngineJsonModule` (rank ⇄ number, unit id ⇄ text, position ⇄ `home:0`) used by both mappers; is-getter auto-detection off in both mappers instead of `@JsonIgnore` on records; pretty printer pinned to `\n`; `UnitView` for `BattleStarted` | the golden file is the frontend's contract and must not depend on packages, platform line ends or whether a method is named `isX` |
| 2026-10-14 | Step 11: `Unit.health()` may go below zero during a battle (`isAlive()` = health > 0; `stats()` shows 0), `setPermanentStats(total)` implements the merge rule, `resetStats` serves Phoenix Feather; `Band.insert` refuses a duplicate id, so `RunState.assertInvariants` only confirms it; `TavernSlot.EMPTY` sentinel instead of nulls; the level-up bonus slot is addressed as hero slot index `heroSlots().size()`; `Tavern.addRelicSlot` for a stocked relic when no relic slot is empty; `RunState` also carries `rollsThisTurn` (the roll-seed index), `lastEventSeq` (contiguous event numbers across a run) and `nextUnitId()` (the counter that replaces `UnitId.Sequence`); `Rules.MAX_GOLD = 50`; `Outcome`, `RunStatus`, `BattleSummary`, `Counters` added; `TestFixtures` lives in test package `engine.testing` | one mutable aggregate with hand-written `equals` lets a test prove "rejected action, state unchanged" with one `isEqualTo`; a sentinel keeps the slot list null-free for Jackson and for the UI |
| 2026-10-14 | Step 10: Jackson **3** (`tools.jackson.databind`, the one Spring Boot 4 uses) and `json-schema-validator` 3.0.8 (built on Jackson 3); every DSL number that may scale (`count`, `limit.perTurn`, `limit.perBattle`, summon stats) is an `Amount` object, not a bare integer, so the content stays uniform; `CHOSEN` target (the band member picked when buying a relic) and `KILL` effect (Poison Vial) added to the GDD §7 lists; ranks are written `1..6` and bridged to the `Rank` enum by a custom (de)serializer; one schema checks the three files as a single document and reports paths like `/heroes/0/ability` | Jackson 2 and 3 cannot share a mapper and the backend will use 3; Kashi's Ward count and Demon's per-battle limit scale with level, which a bare integer cannot say |
| 2026-10-14 | Step 09: `Hash64` = `mix` (splitmix64 finaliser, proven equal to the JDK's `SplittableRandom` by a known-answer test), `of(long...)` (ordered parts) and `ofString`; `SeedDerivation.battleSeed` = hash(runSeed, turn, "battle") and `rollSeed(turnSeed, index)` adds the label "roll"; `GameRandom.fork(label)` derives from the **stored seed** plus the label, never from the generator's position, and `seed()` is exposed for bug reports. `UnitId.Sequence` is **kept** (Step 08 said Step 09 would replace it with a seeded factory): random ids can collide, a counter cannot; it moves into `RunState` in Step 11 | one stored run seed replays everything; a fork that depended on draw order would reshuffle every battle after any new shop ability |
| 2026-10-14 | Step 08: `Position(Side, int index)` is in (the PLAN row names it, the step body did not; index 0 = front, `toString` prints `home:0` like the scenario files); `MAX_STAT` lives in `Rules`, not `Stats`; `Xp` gains `MAX` and `ZERO`; the counter factory is the nested `UnitId.Sequence` (one per run, replaced by a seeded one in Step 09). VS Code gotcha: the Java extension auto-imported `main.java.com.…` because the folder was opened before any pom existed — delete the import and *Developer: Reload Window* once | every rulebook number in one holder; a nested sequence keeps the factory on `UnitId` as the plan implies |
| 2026-10-14 | Step 07: the `java` CI job (`./mvnw -B -ntp verify` on Corretto 25 via `actions/setup-java@v6` with the Maven cache) is a **required** check beside `docs` from this step on (`infra/github/branch-protection.json`); versions pinned at the latest on Maven Central that day (Spring Boot BOM 4.1.1, compiler 3.16.0, surefire 3.6.0, JaCoCo 0.8.15, enforcer 3.6.3, wrapper 3.3.4, Maven 3.9.16); the wrapper was set up before the poms so `./mvnw -v` is the first moment | rule 2 says Verify must pass in CI, so a green engine build must gate every merge from the first test on; `setup-java@v4` in the plan is two majors behind |
| 2026-10-14 | Step 06: IAM Identity Center and `aws configure sso` replaced by an IAM user `bryan` (MFA, **no access keys**) and `aws login` (AWS CLI ≥ 2.32: browser sign-in, temporary credentials, 12 h) until the Paid-plan upgrade; Identity Center joins the Step 77 upgrade checklist; the template's budgets are named `forgotten-heroes-5` / `forgotten-heroes-20`; `check-env.sh` now requires AWS CLI ≥ 2.32 | Q32 — enabling Identity Center creates an AWS Organization, and AWS states that doing so on a Free-plan account upgrades it to Paid and expires the credits immediately; the smaller *account instance* cannot hand out CLI credentials |

## Where the thinking lives
`knowledge/` — research & design: SAP deep dive, tech stack, costs, GDD, architecture, assets, UX/sound, testing, PM, environment, glossary, learning path, open questions.
