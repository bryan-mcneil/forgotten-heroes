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

```
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
| 1 Game Engine | 07–24 | a complete, tested, deterministic rules engine + simulator you can run from the terminal | 55 | [plan/phase_1_engine.md](plan/phase_1_engine.md) |
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
- [ ] **05** CI skeleton (`ci.yml` with docs lint), Dependabot, branch protection on `main` — est 1h
- [ ] **06** AWS account hardening: root MFA, Identity Center CLI user, `aws configure sso`, budgets template ($5/$20), cost tag — est 1.5h

## Phase 1 — Game Engine (pure Java)
- [ ] **07** Maven parent + wrapper (script-only zip, no global Maven) + `engine` module; JUnit, AssertJ, JaCoCo gate; first test — est 2h
- [ ] **08** Core value types: `Stats`, `Rank`, `Level/Xp`, `Side`, `Position`, `UnitId`, `Perk` (records, sealed types) — est 2h
- [ ] **09** Deterministic RNG: `GameRandom`, seed derivation per turn/battle, reproducibility tests — est 2h
- [ ] **10** Content model + JSON loader + schema validation; rank I heroes & relics in `heroes.json` — est 3h
- [ ] **11** Board model: `Unit`, `Band` (5 slots), `Tavern`, `RunState`; invariants — est 3h
- [ ] **12** Event model: sealed `GameEvent` family, `EventLog`, JSON serialization, golden test — est 2h
- [ ] **13** Tavern actions I: `StartTurn` (gold, rank schedule, fill), `Roll`, `Freeze`/`Unfreeze` + `TavernResolver` — est 3h
- [ ] **14** Tavern actions II: `Buy`, `Sell`, `Reorder`, `Merge` + leveling/XP + level-up bonus slot — est 4h
- [ ] **15** Relics: instant effects, perks, XP, tavern-wide buffs, `EAT_RELIC` — est 3h
- [ ] **16** Trigger system: `TriggerDispatcher`, ordering rule, limits, `AbilityInterpreter` for tavern triggers — est 4h
- [ ] **17** Battle resolver I: setup, start-of-battle, clash loop, hurt/knockout/fall, shifting, result, clash cap — est 4h
- [ ] **18** Battle resolver II: summons in battle, before/after attack, ally triggers, all 9 perks — est 4h
- [ ] **19** Full MVP content: 36 heroes, 7 tokens, 16 relics + one scenario test per hero/relic — est 5h
- [ ] **20** `GameSession` facade: start run, apply action, end turn vs ghost, run end; full-run test — est 3h
- [ ] **21** Bots: `RandomBot`, `GreedyBot`; a bot can finish a run — est 2h
- [ ] **22** `sim` module: CLI (`run`, `fuzz`, `replay`, `check`), Markdown/JSON balance report — est 4h
- [ ] **23** Property tests (jqwik), ArchUnit rules, coverage ≥ 90 % gate, PIT `[opt]` — est 3h
- [ ] **24** Engine README (rules as implemented, event list, DSL reference) + **Phase 1 review** — est 2h

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

## Where the thinking lives
`knowledge/` — research & design: SAP deep dive, tech stack, costs, GDD, architecture, assets, UX/sound, testing, PM, environment, glossary, learning path, open questions.
