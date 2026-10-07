# Tech Stack Research — AWS Serverless + Java/Spring Boot + React

> Question answered here: *given how SAP works (see `super_auto_pets_knowledge.md`), will the stack
> you want — AWS serverless, Java/Spring Boot, React — work? Where will it hurt? What exact versions
> and services should we use?*
>
> Verified against vendor docs/releases as of **October 2026**. Where I chose between options,
> the choice and the reason are both written down so you can disagree later with full information.

---

## 1. Does the game shape fit serverless? (Yes — unusually well)

SAP's Arena mode is **turn-based and asynchronous**: a handful of HTTP requests per turn, one
heavier request (end turn → battle). No realtime sockets, no long-lived connections, no ticking
game loop on the server. That is the ideal workload for AWS Lambda:

| Game need | Serverless fit | Notes |
|---|---|---|
| ~15 small requests per turn (buy/sell/roll/freeze/reorder) | ✅ | Each is a 50–150 ms Lambda invocation once warm |
| 1 heavier request per turn (battle) | ✅ | A full battle resolves in a few milliseconds of CPU in Java |
| Opponent = stored snapshot ("ghost") | ✅ | A DynamoDB query, no live opponent needed |
| Idle most of the month (portfolio) | ✅✅ | **Zero cost when idle** — the main reason to go serverless |
| Login / accounts | ✅ | Cognito, free up to 10,000 monthly active users |
| Static game client (sprites, audio, JS) | ✅ | S3 + CloudFront, 1 TB/month free forever |
| Versus mode (8 players, timers, lobby) | ⚠️ | Needs WebSockets (API Gateway WebSocket API) + a scheduler — **out of MVP scope** |

Where it hurts:
1. **Cold starts.** A Spring Boot app can take 5–10 s to boot from zero on Lambda. For a game that
   is unacceptable. **Fix: Lambda SnapStart for Java** — Lambda snapshots the *initialized* JVM and
   restores it in a few hundred ms; it is **free for Java** (Python/.NET pay for it). Confirmed:
   Java 25 runtime supports SnapStart.
2. **The 15 round-trips per turn** each pay network latency (~50–120 ms). Acceptable; SAP's
   "batch the shop phase + hash" trick is the documented upgrade if it ever feels slow.
3. **Local development** of a Lambda-shaped app can be clumsy. **Fix:** the Spring Boot app is a
   normal web app locally (`mvnw spring-boot:run`); only the deployed artifact is wrapped in a
   Lambda handler. You rarely need `sam local`.
4. **Debugging in the cloud** is logs-first. We set up structured JSON logs and CloudWatch
   dashboards early so you are never blind.

---

## 2. Backend: Java + Spring Boot on Lambda

### 2.1 Versions (verified)
| Component | Version | Why |
|---|---|---|
| Java | **25 (LTS)** via Amazon Corretto | Lambda added the Java 25 managed runtime on 14 Nov 2025 (SnapStart supported). Your machine has Java 26 (non-LTS) — install Corretto 25 so local == cloud |
| Spring Boot | **4.1.x** (4.1.0 released 10 Jun 2026; 4.0 line is also fine) | Spring Framework 7 baseline, supports Java 17–25 |
| Lambda adapter | `com.amazonaws.serverless:aws-serverless-java-container-springboot4` **3.0.x** (3.0.2, Jun 2026) | AWS Labs library; your Spring controllers run unchanged inside Lambda |
| Build | **Maven with wrapper (`mvnw`)** | Nothing to install globally; identical on CI |
| AWS SDK | **AWS SDK for Java v2** + DynamoDB **Enhanced Client** | Maps Java records/classes to DynamoDB items |
| API docs | **springdoc-openapi** | Generates OpenAPI; the frontend generates TypeScript types from it |
| Tests | JUnit 5, AssertJ, **jqwik 1.9.x** (property-based), **ArchUnit 1.4.x**, **Testcontainers** (DynamoDB Local image), **PIT** (mutation testing, optional) | See `quality_and_testing_strategy.md` |

### 2.2 Two ways to run Spring on Lambda (and which we choose)
| Option | How it works | Pros | Cons |
|---|---|---|---|
| **A. aws-serverless-java-container (chosen)** | A Lambda handler translates the API Gateway event into a fake servlet request; Spring MVC handles it normally | Keep ordinary `@RestController`s, run locally as a normal app, mature, SnapStart-friendly | Slightly heavier than pure functions |
| B. Spring Cloud Function | Each endpoint is a `Function<In,Out>` bean | Idiomatic "FaaS" | Different programming model, less like a real Spring job, similar cold starts in benchmarks |
| C. Lambda Web Adapter (container) | Run the Spring Boot jar as an HTTP server inside a container image | Zero code changes | Container images cold-start slower, ECR storage cost, no SnapStart |

Reason: you want to **learn Spring Boot the way employers use it** (controllers, services,
repositories, profiles, tests). Option A gives that *and* serverless deployment.

### 2.3 One Lambda ("Lambdalith") vs many small Lambdas
We deploy **one Lambda function running the whole Spring Boot API** behind an HTTP API `$default`
route. Reasons: one SnapStart snapshot to warm, one deployment unit, far simpler to reason about,
and the service is small (≈10 endpoints). Many-small-functions is the "classic serverless" shape
but it fights Spring's startup model and multiplies cold starts. This is a recognized pattern
(AWS calls it a "monolithic Lambda") and is a legitimate talking point for interviews.

### 2.4 SnapStart specifics you must know
* SnapStart runs your init code at **publish** time and snapshots the JVM. Anything created during
  init that must be unique per instance (random seeds, connection state) must be re-created in a
  `@PostRestore` hook (CRaC `Resource` interface; the AWS Java SDK handles its own connections).
* **Never seed game RNG at startup.** Our seeds derive from `(runId, turn)` at request time — so
  this is naturally safe.
* Requires a **published version/alias**; SAM handles it with `AutoPublishAlias` + `SnapStart: ApplyOn: PublishedVersions`.
* Memory: start at **1024 MB** (CPU scales with memory; Java likes ≥1 GB). Tune later with
  Lambda Power Tuning.

---

## 3. Data: DynamoDB (single table)

Why not a SQL database: RDS costs money 24/7 (≈ $13+/month minimum) and needs a VPC, which drags in
NAT costs for Lambda. DynamoDB is **pay per request**, has an **always-free 25 GB**, no VPC, and
our access patterns are simple key lookups. On-demand pricing (us-east-1, verified Oct 2026):
**$0.625 per million writes, $0.125 per million reads, $0.25/GB-month** after the free 25 GB.

Access patterns (full design in `architecture.md`):
1. Get/put a **run** by (user, runId) — the entire board state is one JSON item (<20 KB).
2. Append a **ghost snapshot** for turn *N*; fetch a few random ghosts for turn *N*.
3. Store/fetch a **battle log** by (runId, turn) for replay.
4. **Leaderboard** top-N for a season.
5. **Profile** by user.

Single-table with `PK`/`SK` keys covers all five. **TTL** attributes expire ghosts and battle logs
automatically so storage stays near zero.

---

## 4. Auth: Amazon Cognito

* **User Pool** (Essentials tier). Free tier: **10,000 MAU** for Lite/Essentials, permanent.
* **Managed Login** (hosted sign-up/sign-in pages) + **PKCE** flow in the React app via
  `react-oidc-context`. We do not build password forms ourselves (good security, less code).
* **HTTP API JWT authorizer** validates the Cognito token at the gateway — unauthenticated requests
  never reach Lambda (saves cost, simplifies code). Spring additionally reads the `sub` claim.
* Recruiter friction: a **demo account** (credentials on the landing page) so nobody has to sign up
  to look. Guest accounts (SAP's `RegisterGuestRequest`) are a stretch goal.

---

## 5. API Gateway: HTTP API (not REST API)

| | HTTP API (chosen) | REST API |
|---|---|---|
| Price | **$1.00 / million** | $3.50 / million |
| JWT authorizer | built-in | needs Lambda authorizer or Cognito authorizer |
| Latency | lower | higher |
| Features we'd miss | usage plans/API keys, request validation models, WAF attach | — |

Throttling is configured at the stage (e.g. 50 req/s, burst 100) to bound cost. WAF costs ≥$5/month
and is deliberately skipped.

---

## 6. Frontend: React 19 + Vite 8 + TypeScript

| Component | Version | Why |
|---|---|---|
| React | **19.2.x** | Current stable; `use`, Actions, improved Suspense |
| Vite | **8.x** (Rolldown bundler) | Fast dev server, tiny builds |
| TypeScript | 5.x strict | Generated API types catch mistakes before runtime |
| Styling | **Tailwind CSS v4** (`@tailwindcss/vite`) | Utility classes, design tokens via CSS variables, no config file needed |
| State | **Zustand 5** | Tiny store for run state + playback; no boilerplate |
| Routing | **React Router 7** | Home / Tavern / Battle / Leaderboard / Dev routes |
| Drag & drop | **dnd-kit** | Pointer + **keyboard** sensors, screen-reader announcements built in |
| Animation | **Motion** (formerly Framer Motion) | Layout animations for cards; respects `prefers-reduced-motion` |
| Audio | **Howler.js** with audio sprites | One file, many SFX; handles browser autoplay rules |
| Auth | `react-oidc-context` (oidc-client-ts) | PKCE with Cognito Managed Login |
| API | `openapi-typescript` + typed `fetch` wrapper | Types generated from the backend's OpenAPI |
| Tests | **Vitest** + React Testing Library + **MSW** (mock API) + **Playwright** (E2E) + axe-core + Lighthouse CI | See quality doc |

### Rendering decision: DOM/CSS vs Canvas (PixiJS)
SAP's board is five sprites, a shop row, and some numbers. That is **not** a rendering workload.
We render with **plain React DOM + CSS sprite-sheet animation** (`steps()` keyframes on a
`background-position`, `image-rendering: pixelated`) and Motion for tweens. Reasons: fully
accessible (real buttons, focus, ARIA), trivially testable with Testing Library, no second
rendering world to learn, and performance is a non-issue at this scale. PixiJS v8 (with
`@pixi/react` for React 19) is the upgrade path if we ever want particle-heavy FX — the battle
renderer is isolated behind a `BattlePlayer` component so it could be swapped without touching
game logic.

---

## 7. Hosting the client: S3 + CloudFront (primary), Hostinger (secondary)

* **S3 + CloudFront** with Origin Access Control (bucket stays private), SPA fallback (404 →
  `index.html`), gzip/brotli, long-lived cache headers for hashed assets. CloudFront's **always-free
  1 TB/month + 10M requests** covers a portfolio many times over. Default `*.cloudfront.net` URL costs $0.
* **Custom domain at $0 (Q2 → `heroes.bryanmcneil.pro`):** request a free **ACM certificate** in us-east-1
  and validate it via DNS records added in **Hostinger's DNS** for `bryanmcneil.pro` (so no Route 53 hosted
  zone at $0.50/month). Add a CNAME `heroes` → CloudFront. The API is served by the same distribution under
  `/api/*` (one origin, no CORS in prod; the US-only geo-restriction and Shield Standard cover it; an
  origin-verify header blocks direct access) — `architecture.md` ADR-15.
* **Hostinger Business** (you already pay for it): supports Git auto-deploy to `public_html` and
  Node.js apps. We use it for the **Corgi Space Cadet studio site** (`corgi.bryanmcneil.pro`, static landing page), which
  also gives the game a "publisher" home. It *could* host the React build too, but then the AWS
  demonstration would be weaker — so the game stays on CloudFront.

---

## 8. Infrastructure as Code: AWS SAM (not CDK, not Terraform, not console clicks)

> Console clicks have exactly one place here: **learning**. Every AWS resource is first built by hand in the
> web console on `dev` (Q31, click-by-click instructions written at step time), then recreated by SAM and the
> manual copy deleted. Prod is only ever deployed from code.

| | SAM (chosen) | CDK | Terraform |
|---|---|---|---|
| Language | YAML (CloudFormation + shortcuts) | TypeScript/Java code | HCL |
| Learning curve | lowest; one file you can read top to bottom | highest (constructs, synth) | medium; separate state file to manage |
| Serverless fit | built for Lambda/API GW/DynamoDB | great | fine |
| Local tooling | `sam build`, `sam local`, `sam deploy`, `sam pipeline bootstrap` (creates OIDC roles!) | `cdk deploy` | `terraform apply` |

SAM gives you CloudFormation knowledge (the AWS fundamental), a single `template.yaml` as the
truth, and OIDC bootstrap for GitHub Actions. CDK is a reasonable later upgrade.

---

## 9. CI/CD: GitHub Actions with OIDC (no AWS keys in GitHub)

* Repo is **public** (recruiters) → GitHub Actions minutes are **unlimited and free**; GitHub
  **Environments with required reviewers** (manual approval for prod) are free on public repos.
* Workflows authenticate to AWS with **OpenID Connect**: GitHub mints a short-lived token, AWS
  trusts it for *this repo and branch only*, and issues temporary credentials. **No long-lived
  access keys exist anywhere.** `aws-actions/configure-aws-credentials@v4`.
* Pipeline shape: PR → `ci.yml` (tests) · merge to `main` → `deploy-dev.yml` (dev stack + dev
  bucket + smoke test) · tag `v*` → `deploy-prod.yml` (manual approval → prod).
* Two stacks in one account: `forgotten-heroes-dev`, `forgotten-heroes-prod`. Free tiers are per
  account, so a second environment is effectively free at our scale.

---

## 10. Local development loop (what a normal day looks like)

```
Terminal 1:  docker compose up           # DynamoDB Local (+ a tiny admin UI)
Terminal 2:  ./mvnw -pl backend spring-boot:run -Dspring-boot.run.profiles=local
Terminal 3:  cd frontend && npm run dev  # Vite on :5173, proxies /api → :8080
```
Profile `local` disables Cognito JWT checks and accepts an `X-Dev-User` header (an ArchUnit test
proves that code cannot be active in prod). Tests: `./mvnw verify` (engine + backend) and
`npm test` / `npm run e2e`.

---

## 11. Risks & mitigations (honest list)

| Risk | Likelihood | Mitigation |
|---|---|---|
| Spring Boot 4.1 + serverless-java-container 3.0.x incompatibility surprises | low | Pin versions; the archetype `aws-serverless-springboot4-archetype` is a known-good template; fall back to Spring Boot 4.0.x |
| SnapStart restore still ~0.5–1 s on first hit after a deploy | medium | Priming (`@PostRestore` warm a fake request), keep the jar lean, show a "Waking the tavern…" loader on first call; optional cheap CloudWatch Events "keep-warm" ping every 5 min (free tier) |
| Windows-specific friction (paths, line endings, Docker Desktop) | medium | `.gitattributes` forces LF; all scripts in Bash (Git Bash on Windows, Q18); Testcontainers needs Docker Desktop running |
| Asset licensing in a public repo | medium | Processed sprite atlases are **not committed**; CI pulls from a private S3 bucket (see `asset_inventory_and_pipeline.md`) |
| Scope creep (Versus mode, 300 heroes) | high | The plan ships Arena with 36 heroes; everything else is Phase 6+ |
| Learning overload | high | Each step teaches one concept; "Sensei notes" per step; glossary file |

---

## 12. Sources
* AWS Lambda Java 25 runtime (14 Nov 2025): https://aws.amazon.com/about-aws/whats-new/2025/11/aws-lambda-java-25
* Lambda SnapStart docs (Java no extra charge): https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html
* aws-serverless-java-container springboot4 3.0.2 (Jun 2026): https://repo1.maven.org/maven2/com/amazonaws/serverless/aws-serverless-java-container-springboot4/
* Spring Boot 4.1.0 (10 Jun 2026): https://www.herodevs.com/blog-posts/spring-boot-versions-eol-dates-and-latest-releases-april-2026
* DynamoDB on-demand pricing: https://aws.amazon.com/dynamodb/pricing/on-demand/
* API Gateway pricing: https://aws.amazon.com/api-gateway/pricing/
* Cognito pricing (10k MAU free): https://aws.amazon.com/cognito/pricing/
* CloudFront free tier (1 TB): https://aws.amazon.com/free/networking/
* AWS Free Tier 2025 changes: https://dev.to/ushpal/new-aws-free-tier-updates-after-july-15-2025-what-you-need-to-know-3m36
* GitHub Actions OIDC + SAM: https://aws.amazon.com/blogs/compute/securing-ci-cd-pipelines-with-aws-sam-pipelines-and-oidc/
* Hostinger Git deploy: https://docs.hostinger.com/websites/git · Node.js apps: https://docs.hostinger.com/node.js/creating-an-app
* PixiJS React v8: https://pixijs.com/blog/pixi-react-v8-live · Vite 8: https://www.voidzero.dev/blog
* dnd-kit accessibility: https://dndkit.com/guides/accessibility · Howler.js: https://howlerjs.com/
* jqwik: https://jqwik.net/ · ArchUnit 1.4.2: https://archunit.org/ · PIT: https://pitest.org/quickstart/maven
