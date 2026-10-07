# Architecture — Forgotten Heroes

> The blueprint the build plan follows. Read this once end-to-end, then come back to the section
> you are working in. Decisions are numbered **ADR-n** (Architecture Decision Record) so we can
> refer to them in commits and revisit them deliberately.

---

## 1. The big picture

```
                 ┌──────────────────────────── AWS (one account, two stacks: dev / prod) ───────────────────────────┐
                 │                                                                                                   │
  Browser        │   CloudFront (US only · Shield Std) ──► S3 (private, OAC)   Cognito User Pool (Managed Login)  │
  React 19 SPA ──┼──► static files: index.html, JS, atlases, audio sprite        ▲ login / tokens                   │
      │          │                                                               │                                   │
      │ fetch /api/* + Bearer JWT                                                │                                   │
      └──────────┼──► CloudFront /api/* ─(x-origin-verify)─► HTTP API ─(JWT)─► Lambda (Java 25, SnapStart)        │
                 │        throttled 20 rps                      Spring Boot 4.1 "Lambdalith"                       │
                 │                                                   │   uses  ┌──────────────────┐                 │
                 │                                                   ├────────►│  engine (pure Java)│ rules, battles │
                 │                                                   │         └──────────────────┘                 │
                 │                                                   └────────► DynamoDB single table (on-demand, TTL)
                 │                                                                                                   │
                 │   CloudWatch alarms + Budgets ($5/$20) ──► SNS ──► e-mail + KillSwitch λ (conc 0 · sign-ups off)  │
                 └───────────────────────────────────────────────────────────────────────────────────────────────────┘

  GitHub (public repo) ── Actions (OIDC → IAM role) ── ci.yml / deploy-dev.yml / deploy-prod.yml (manual approval)
  Hostinger ── DNS for bryanmcneil.pro (ACM validation, CNAME heroes → CloudFront) + studio page corgi.bryanmcneil.pro
```

Public entry points: `https://heroes.bryanmcneil.pro` (CloudFront, **US-only** by geo-restriction — Q4/Q27; blocked visitors, VPN exits abroad included, get a static `blocked.html`) and
the Cognito Managed Login domain. The HTTP API's own URL rejects any request that lacks CloudFront's secret
`x-origin-verify` header (403), so there is exactly one front door. See ADR-15.

**ADR-1 — Server-authoritative, client-replays.** The browser never computes rules. It sends
intents (buy/sell/roll/…); the server validates, mutates state, and returns the new state plus an
**event log**. Battles return an event log the client *animates*. (Same shape as SAP, §4.3 of the
SAP knowledge file.) Reason: one rules implementation (Java), no cheating, perfect replays, and
the frontend stays a pure view.

**ADR-2 — Monorepo**, one GitHub repository with clear top-level folders (below). Reason: one PR
per step touches whatever layers that step needs; recruiters see everything in one place.

**ADR-3 — Engine is framework-free Java.** `engine/` depends on nothing but the JDK and Jackson.
ArchUnit enforces it. Reason: testable in milliseconds, reusable by the simulator and the backend.

---

## 2. Repository layout

```
forgotten-heroes/
├── README.md                 ← recruiter-facing front page (demo link, GIF, architecture, how to run)
├── PLAN.md                   ← the step list with status; links into plan/
├── plan/                     ← one file per phase, one section per step
├── knowledge/                ← research & design docs (this folder)
├── docs/                     ← ADRs, runbook, setup guides, diagrams, balance reports, cost receipts
├── pom.xml                   ← Maven parent (modules: engine, backend, sim)
├── mvnw, mvnw.cmd            ← Maven wrapper (no global Maven install)
├── engine/                   ← pure Java game rules (+ content JSON in resources)
├── backend/                  ← Spring Boot API + Lambda handler
├── sim/                      ← balance simulator CLI (bots, reports)
├── frontend/                 ← React + Vite + TypeScript app
├── infra/                    ← template.yaml (SAM), samconfig.toml, IAM policies, budget template, scripts
├── tools/                    ← asset pipeline (Python), audio sprite builder, ghost seeding scripts
├── studio/                   ← Corgi Space Cadet static site (deployed to Hostinger)
├── .github/                  ← workflows, issue & PR templates
└── docker-compose.yml        ← DynamoDB Local + admin UI for local dev
```

---

## 3. The engine (`engine/`)

### 3.1 Packages
```
com.forgottenheroes.engine
├── model        Stats, Rank, Level, Side, Position, UnitId, Perk, Unit, Band, Tavern, RunState, Outcome
├── content      HeroDefinition, RelicDefinition, TokenDefinition, AbilityDefinition (DSL records), ContentLoader, ContentIndex
├── rng          GameRandom (seeded, forkable), SeedDerivation
├── action       sealed Action: StartTurn, Roll, Buy, BuyRelic, Sell, Reorder, Merge, Freeze, Unfreeze, EndTurn
├── event        sealed GameEvent (~25 types), EventLog, EventJson
├── mutator      ~30 small state changes (GainGold, SpendGold, SetStats, ModifyStats, SummonUnit, RemoveUnit, …)
├── resolver     TavernResolver (shop rules), BattleResolver (fight), TriggerDispatcher, AbilityInterpreter, Targeting, Ordering
├── session      GameSession facade: start(seed) · apply(action) · endTurn(ghost) · state()
└── bot          Bot interface, RandomBot, GreedyBot
```

### 3.2 Flow of one action
```
Action ──► Resolver.validate(state, action)   → throws RuleViolation("NOT_ENOUGH_GOLD") if illegal
       ──► Resolver.apply(state, action)      → calls Mutators; each Mutator appends 1..n GameEvents
       ──► TriggerDispatcher.fire(trigger)    → finds eligible abilities, orders them (GDD §4.6), runs
                                                 AbilityInterpreter → more Mutators → more events
       ──► returns EventLog (ordered, numbered) and the mutated RunState
```
Mutators are the **only** code allowed to change `RunState`. Everything above them is decision
logic. This is SAP's Action → Resolver → Mutator → Event shape.

### 3.3 Determinism (ADR-4)
* `GameRandom` wraps `SplittableRandom`. The run seed is created once (`SecureRandom`) at run
  start and stored. Each turn derives `turnSeed = hash(runSeed, turn)`; each battle derives
  `battleSeed = hash(runSeed, turn, "battle")`. Shop rolls use a counter so the *n*-th roll of a
  turn is reproducible.
* Rule: **no `Math.random()`, no `new Random()`, no `System.currentTimeMillis()` in the engine.**
  ArchUnit forbids them. A property test replays the same actions with the same seed and asserts
  byte-identical event logs.

### 3.4 Events (the contract with the frontend)
Each event: `{ "seq": 17, "type": "DamageDealt", ...fields }`. Types:
`RunStarted, TurnStarted, GoldChanged, TavernRolled, TavernItemFrozen, TavernItemUnfrozen,
HeroBought, RelicBought, HeroSold, HeroesReordered, HeroesMerged, LevelUp, XpGained, StatsChanged,
PerkGained, PerkConsumed, AbilityTriggered, BattleStarted, Clash, DamageDealt, UnitHurt, Knockout,
UnitFell, UnitSummoned, SummonFailed, LineShifted, BattleEnded, HeartsChanged, CrownsChanged,
RunEnded`. Events carry **before/after** values where useful (e.g. `StatsChanged{unitId, attack: 2→3}`)
so the client never has to recompute anything.

### 3.5 Content
`engine/src/main/resources/content/heroes.json`, `relics.json`, `tokens.json`, validated against
`content.schema.json` at load. Served to the client via `GET /api/content` (ETag-cached) so the
frontend never carries its own copy of rules data.

---

## 4. The backend (`backend/`)

### 4.1 Layers
```
web        @RestController + DTO records + exception → RFC 9457 ProblemDetail
service    RunService (uses engine.GameSession), MatchmakingService, LeaderboardService, ProfileService
repo       RunRepository, GhostRepository, BattleLogRepository, LeaderboardRepository (DynamoDB Enhanced Client)
security   JwtUserResolver (Cognito sub → userId); LocalDevAuth (profile "local" only)
lambda     StreamLambdaHandler (aws-serverless-java-container) + SnapStart priming
```

### 4.2 API (v1) — all under `/api`, JSON, Bearer JWT except `/content` and `/health`
| Method & path | Purpose | Notes |
|---|---|---|
| `GET /health` | liveness | no auth |
| `GET /content` | heroes, relics, tokens, rules constants | no auth, ETag |
| `GET /me` | profile + current run id | creates profile on first call |
| `POST /runs` | start a run | 409 if an active run exists |
| `GET /runs/{runId}` | full state (band, tavern, gold, hearts, crowns, turn, version) | |
| `POST /runs/{runId}/actions` | one tavern action `{type, …, expectedVersion}` | returns `{state, events}`; 409 on version mismatch |
| `POST /runs/{runId}/end-turn` | end turn → battle | returns `{battle: {opponent, events, result}, state}` |
| `GET /runs/{runId}/battles/{turn}` | replay a past battle | |
| `POST /runs/{runId}/abandon` | give up | |
| `GET /leaderboard?season=2026-10` | top 50 by crowns then fewest turns | |

Error body example: `{ "type": "https://heroes.bryanmcneil.pro/errors/rule-violation", "title": "Not enough gold", "status": 422, "code": "NOT_ENOUGH_GOLD", "detail": "You have 2 gold; a hero costs 3." }`

### 4.3 Concurrency & idempotency (ADR-5)
Every run item has a `version` number. Actions carry `expectedVersion`; the write is a DynamoDB
**conditional update** (`version = :expected`). A stale client gets **409** and reloads. Double
submits of the same action are therefore harmless.

### 4.4 Matchmaking (ADR-6)
On end turn *N*: (1) save ghost `{band, turn: N, crowns, hearts, bot:false}` with TTL 30 days;
(2) query ghosts for turn *N* with `crowns ∈ [c−1, c+1]`, excluding this user, take up to 25 most
recent, pick one with the battle RNG; (3) fallback to bot ghosts (`bot:true`); (4) fallback to a
mirror of the player's own band. Store the chosen opponent on the battle log for replay.

*Door left open (Q11, Q29):* the first **private room** mode (play a friend) will be a **same-seed challenge
link** — a `roomId` on the run and a shared seed, no live connection, no new infrastructure. True real-time
rooms would add an API Gateway WebSocket API later; nothing in the table design prevents either. 8-player
Versus is out.

---

## 5. DynamoDB single table `forgotten-heroes-{env}` (ADR-7)

| Entity | PK | SK | Attributes (summary) | TTL |
|---|---|---|---|---|
| Profile | `USER#<sub>` | `PROFILE` | displayName, createdAt, bestCrowns, runsPlayed, activeRunId | — |
| Run | `USER#<sub>` | `RUN#<runId>` | status, turn, gold, hearts, crowns, seed, version, state (JSON, compact), updatedAt | 90 d after finish |
| Ghost | `GHOST#T<turn>` | `<crowns>#<ISO time>#<runId>` | band (JSON), ownerSub, bot (bool) | 30 d |
| Battle log | `RUN#<runId>` | `BATTLE#<turn padded>` | opponentBand, events (JSON, gzip if > 100 KB), result | 7 d |
| Leaderboard | `LB#<season>` | `<99−crowns padded>#<turns padded>#<sub>` | displayName, crowns, turns, finishedAt | end of next season |

Access patterns map to `GetItem`/`Query` only; the ghost SK begins with crowns so the "similar
crowns" filter is a `begins_with`/`between` on the sort key. Item sizes stay < 10 KB except
battle logs (compressed).

---

## 6. End-turn sequence (the most important flow)

```
Client                    API GW           Lambda/Spring                 Engine              DynamoDB
  │ POST end-turn            │                 │                             │                    │
  │ ───────────────────────► │ JWT ok ───────► │ load run (version check)  ─────────────────────► │
  │                          │                 │ ◄──────────────────────────────────────────────── │
  │                          │                 │ fire END_OF_TURN ─────────► │ events             │
  │                          │                 │ save ghost ─────────────────────────────────────► │
  │                          │                 │ pick opponent (query) ──────────────────────────► │
  │                          │                 │ resolve battle(seed) ──────► │ battle events      │
  │                          │                 │ apply result (hearts/crowns/turn+1, StartTurn)    │
  │                          │                 │ save run (cond. write) + battle log ────────────► │
  │ ◄─── {battle, state} ─── │ ◄────────────── │                                                    │
  │ play back events at chosen speed; show result; render new tavern
```

---

## 7. Authentication (ADR-8)

* Cognito **User Pool** + app client (public, PKCE, no secret) + **Managed Login** branding.
* React uses `react-oidc-context`: redirect to Cognito → back with code → tokens in memory (not
  localStorage) with silent refresh. API calls send `Authorization: Bearer <access token>`.
* API Gateway **JWT authorizer** (issuer = pool URL, audience = app client id) rejects bad tokens
  before Lambda. Spring reads claims from the request context the adapter exposes.
* Local profile: no Cognito; `X-Dev-User: bryan` header sets the user. The `local` profile class
  is in package `…security.local` and ArchUnit asserts nothing in `prod` config references it.
* Demo account for recruiters (created in the pool; credentials on the landing page).

---

## 8. Frontend (`frontend/`)

```
src/
├── app/            routes, providers (auth, query), layout shell, error boundary
├── api/            generated types (openapi-typescript), client.ts (fetch + auth + 409 retry)
├── content/        hooks to load /content once; lookups by id
├── features/
│   ├── home/       title screen, play/continue, how-to-play, leaderboard link
│   ├── tavern/     TavernScreen, BandRow, TavernRow, HeroCard, RelicCard, Hud, dnd wiring, hotkeys
│   ├── battle/     BattleScreen, BattlePlayer (event → animation timeline), UnitSprite, DamageNumber, FxLayer
│   ├── run/        RunStore (Zustand): state, pending flags, apply action, end turn
│   ├── results/    victory/defeat/draw screens
│   ├── leaderboard/
│   └── settings/   volume, speed, reduced motion, keybinds
├── sprites/        Sprite component (CSS steps animation), atlas loader, manifest types
├── audio/          Howler manager, sprite map, event→sfx mapping
├── intro/          Corgi Space Cadet intro
├── dev/            /dev gallery, replay-from-JSON, event inspector (behind VITE_DEV_TOOLS)
└── styles/         Tailwind tokens, pixel-art utilities
```

**ADR-9 — Playback engine.** `BattlePlayer` turns the event list into a timeline of steps with
durations (Clash 450 ms, DamageDealt 300 ms, UnitFell 400 ms, UnitSummoned 350 ms, StatsChanged
200 ms, AbilityTriggered 500 ms with a callout). Speeds 1×/2×/4×, pause, skip-to-end. Each step
updates a local "battle view model" (unit positions, displayed stats) — the view model is the
only thing React renders. Deterministic, testable without DOM.

**ADR-10 — Rendering.** DOM + CSS; sprites via background-position `steps()`; 3× integer scale;
`image-rendering: pixelated`; Motion for layout moves (reorder, shift). PixiJS is the documented
upgrade path if FX demands it.

---

## 9. Infrastructure (`infra/template.yaml`)

Resources: `ApiFunction` (Java 25, 1024 MB, 15 s timeout, SnapStart on published versions,
`AutoPublishAlias: live`, reserved concurrency 10, env vars TABLE_NAME/COGNITO_ISSUER),
`HttpApi` (JWT authorizer, CORS for localhost dev, stage throttling; the app's `OriginVerifyFilter` reads
its secret from SSM), `Table` (PK/SK, TTL
attribute `expiresAt`, on-demand), `UserPool` + `UserPoolClient` + `UserPoolDomain`,
`SiteBucket` (private; the OAC may `s3:ListBucket` so misses are 404) + `CloudFrontDistribution` (OAC, SPA fallback on 404, **403 → `blocked.html`**, compression,
response-headers policy, **`/api/*` behaviour → HttpApi origin with `x-origin-verify`**, **geo restriction
allow-list from `AllowedCountries` = `US`**) + optional `AssetsBucket` (private, for the asset pipeline), `LogGroup`
(14 d), `Dashboard`, `Alarms` (5xx rate, Lambda errors, Lambda throttles, DynamoDB throttles),
`AlarmTopic` (SNS e-mail) + **`KillSwitchFunction`** (Python; subscribed to `AlarmTopic` and the $20
budget; sets reserved concurrency → 0, sign-ups off, distribution disabled; gated by `AutoKill`).
Parameters: `Env` (dev|prod), `SiteDomain` (`heroes.bryanmcneil.pro` in prod), `CertificateArn`,
`AllowedCountries` (default `US`), `AutoKill` (default true in prod). Outputs: API URL, site URL, pool ids.

Environments: `forgotten-heroes-dev` (auto on `main`) and `forgotten-heroes-prod` (tags `v*`, manual approval).

---

## 10. CI/CD (`.github/workflows`)

| Workflow | Trigger | Jobs |
|---|---|---|
| `ci.yml` | PR, push to main | `backend-test` (JDK 25, `./mvnw -B verify`, Testcontainers via Docker on the runner) · `frontend-test` (Node 26, `npm ci`, lint, typecheck, unit tests, build) · `e2e` (Playwright against docker-compose stack; on PR label `e2e` and nightly) |
| `deploy-dev.yml` | push to main | OIDC → `sam build` + `sam deploy` (dev) → pull assets from S3 → `npm run build` with dev API/pool → `aws s3 sync` → CloudFront invalidation → smoke tests (health + one Playwright flow) |
| `deploy-prod.yml` | tag `v*` | same steps against prod inside GitHub Environment `production` (required reviewer: you) |
| `security.yml` | weekly | `npm audit`, OWASP dependency-check, gitleaks |

OIDC trust: `repo:bryan-mcneil/forgotten-heroes:ref:refs/heads/main` (dev role) and
`…:ref:refs/tags/v*` + environment `production` (prod role). No AWS keys in GitHub.

---

## 11. Observability & operations
* **Logs**: JSON lines (`logstash-logback-encoder`), one summary line per request
  (`method path status ms userId runId`), correlation id from `x-amzn-trace-id`.
* **Metrics**: Embedded Metric Format for `BattlesResolved`, `RunsStarted`, `RunsWon`,
  `MatchmakingFallback` — 4 custom metrics, under the free 10.
* **Dashboard**: requests, p50/p95 latency, 5xx, Lambda cold starts (`InitDuration`), DynamoDB
  consumed units, matchmaking fallbacks.
* **Alarms → e-mail**: 5xx > 5 in 5 min; Lambda errors > 5 in 5 min; Lambda throttles > 0;
  DynamoDB throttles > 0; Budget 50/80/100 %.
* **Runbook** (`docs/runbook.md`): what each alarm means and the first three things to check.

---

## 12. Security & anti-cheat checklist
Server-authoritative rules (ADR-1) · JWT at the edge · least-privilege IAM for the Lambda (only
its table) · input validation with Bean Validation + size limits (actions ≤ 2 KB) · CORS locked to
the site origin · security headers via CloudFront response-headers policy · no secrets in repo
(SSM Parameter Store if ever needed) · dependency scanning weekly · rate limiting at the stage ·
version-checked writes · ghosts store only what the opponent needs (no e-mail, no sub exposed to
clients, display names only) · **US-only geo-restriction at CloudFront** (VPN exits abroad included — Q27; blocked
visitors see `blocked.html`) · **API reachable only through
CloudFront** (`x-origin-verify`) · AWS Shield Standard (automatic) · **cost kill switch** on alarms and
budget (concurrency 0, sign-ups off, CDN disabled) · Cognito sign-up canary alarm.

---

## 13. ADR index
| ADR | Decision | Alternative rejected |
|---|---|---|
| 1 | Server computes, client replays an event log | client-side simulation + hash (SAP) — later upgrade |
| 2 | Monorepo | separate repos |
| 3 | Framework-free Java engine enforced by ArchUnit | engine inside Spring module |
| 4 | Seeded, forkable RNG; seeds derived from (runSeed, turn) | random per request |
| 5 | Optimistic locking with `version` + conditional writes | DynamoDB transactions / locks |
| 6 | Ghost matchmaking with bot and mirror fallbacks | live opponents |
| 7 | DynamoDB single table with TTL | RDS/Postgres |
| 8 | Cognito Managed Login + PKCE + API GW JWT authorizer | custom auth, Auth0 |
| 9 | Timeline playback engine with view model | animate directly from events in components |
| 10 | DOM/CSS rendering, 3× pixel scale | PixiJS / Phaser canvas |
| 11 | One Lambda running Spring Boot (Lambdalith), SnapStart, Java 25 | many small functions; containers |
| 12 | SAM + GitHub Actions OIDC, two stacks | CDK, Terraform, console |
| 13 | Public repo; processed art lives in a private S3 bucket, not git | commit atlases; private repo |
| 14 | **Elements palette** for all art; TF→Elements conversions are authored in the Forgotten Kanji toolkit (`gen_tf_to_elements.py --batch`) so both games share them (Q13) | original Time Fantasy palette; converting inside this repo |
| 15 | CloudFront fronts the API (one domain, `x-origin-verify`), **US-only geo-restriction** (kept even though it blocks VPN users — Q27; CloudFront 403 → `blocked.html`), cost **kill switch** Lambda on alarms + budget (Q4) | WAF ($5+/month); API exposed directly; budgets only |
