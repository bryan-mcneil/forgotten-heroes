# Glossary for Beginners

> Plain-language definitions of every term the plan uses. If a word in a step confuses you, it is
> here. Organised by area; skim once, then use as a lookup.

## Git & GitHub
* **Repository (repo)** — a folder whose entire history Git remembers. Ours is `forgotten-heroes`.
* **Commit** — a saved snapshot with a message. One step = one commit on `main`.
* **Branch** — a parallel line of commits. We make `step-NN-…` branches and merge them back.
* **Pull request (PR)** — "please merge my branch"; the place for review, CI results, screenshots.
* **Squash merge** — all branch commits become one commit on `main`. Keeps history = plan.
* **CI (Continuous Integration)** — a robot (GitHub Actions) that runs tests on every PR.
* **CD (Continuous Delivery/Deployment)** — the robot also deploys when `main` changes or a tag is pushed.
* **Tag** — a named commit (`v1.0.0`). Prod only deploys from tags.
* **OIDC (OpenID Connect)** — a way for GitHub's robot to prove its identity to AWS without stored passwords.
* **Branch protection** — rule: nobody (including you) merges to `main` without green CI.
* **`git init`** — turns a plain folder into a repository by creating the hidden `.git/` folder; `-b main` names the first branch.
* **Remote / `origin`** — a copy of the repo somewhere else (GitHub). `origin` is the conventional name for the main one; `git push` sends commits to it.
* **`gh` (GitHub CLI)** — GitHub from the terminal: `gh repo create`, `gh pr create`, `gh issue list`. Our scripts use it so nothing has to be clicked.
* **`.gitignore`** — paths Git must pretend do not exist: build output, secrets, the `Super Auto Pets/` folder. A trailing `/` means "directory". `git check-ignore -v <path>` tells you which rule matched.
* **Line endings (LF vs CRLF)** — the invisible character(s) that end a line. Linux, macOS and Git Bash use LF (`\n`); Windows tools use CRLF (`\r\n`). A bash script saved with CRLF fails with `bad interpreter: ^M`; a `.cmd` file saved with LF can confuse `cmd.exe`.
* **`.gitattributes`** — per-path rules for Git: which files are text (and which line ending they get on checkout) and which are binary. Ours: everything LF, except `.cmd`/`.ps1` (CRLF); images and audio binary.
* **EditorConfig (`.editorconfig`)** — a tiny file every editor understands: indent size, charset, line endings, final newline. Keeps formatting out of code review.
* **Licence (MIT)** — a short permissive licence: anyone may use, copy and modify the code as long as the copyright notice stays. Ours covers the code only; art and audio are not in the repo.

## Java & build
* **JDK** — the Java toolkit (compiler + runtime). **LTS** = long-term-support version (21, 25).
* **Maven** — Java's build tool; `pom.xml` lists modules and dependencies. **Maven Wrapper (`mvnw`)**
  downloads the right Maven automatically, so nobody installs it.
* **Module** — a sub-project (`engine`, `backend`, `sim`) with its own `pom.xml`.
* **Dependency** — a library you download by naming it in the pom.
* **Record** — a tiny immutable class for data (`record Stats(int attack, int health)`).
* **Sealed interface** — an interface with a fixed, listed set of implementations; lets the compiler
  check we handled every `Action`/`GameEvent` type (`switch` exhaustiveness).
* **JUnit 5 / AssertJ** — test framework / readable assertions (`assertThat(gold).isEqualTo(7)`).
* **jqwik** — property-based testing: you state a rule ("gold never negative") and it tries thousands of random inputs.
* **ArchUnit** — tests about code structure ("engine must not import Spring").
* **JaCoCo** — measures test coverage. **PIT** — mutation testing: changes your code on purpose to see if tests notice.
* **Jackson** — converts Java objects ⇄ JSON.

* **Script-only wrapper** — the `mvnw` scripts plus `.mvn/wrapper/maven-wrapper.properties`; the properties file names the Maven version to download. No global Maven install needed.

## Spring Boot
* **Spring Boot** — the standard Java web framework; auto-configures a web server, JSON, validation.
* **Bean** — an object Spring creates and hands to whoever needs it (**dependency injection**).
* **Controller** — receives HTTP requests (`@RestController`). **Service** — business logic.
  **Repository** — talks to the database. Together: the **layers**.
* **DTO (Data Transfer Object)** — the JSON shape sent over the wire, separate from internal models.
* **Profile** — a named configuration set (`local`, `prod`). Lets local dev skip Cognito safely.
* **ProblemDetail (RFC 9457)** — the standard JSON shape for errors.
* **springdoc / OpenAPI** — generates a machine-readable description of the API; the frontend
  generates TypeScript types from it.
* **MockMvc / `@WebMvcTest`** — test a controller without starting a real server.
* **Testcontainers** — starts a real database in Docker just for a test, then throws it away.

## AWS
* **Region** — a geographic datacenter group; we use **us-east-1** (N. Virginia).
* **IAM** — identities and permissions. **Role** — a set of permissions something can *assume*
  temporarily. **Least privilege** — grant only what is needed.
* **Lambda** — run code without a server; billed per request and per millisecond. **Cold start** —
  the delay when Lambda must boot a fresh copy. **SnapStart** — Lambda saves a snapshot of the
  booted JVM and restores it fast; free for Java. **Reserved concurrency** — a cap on how many copies may run at once.
* **API Gateway (HTTP API)** — the public HTTPS front door that forwards requests to Lambda and
  can check JWTs. **Stage** — a deployed version with its own URL and throttle settings.
* **DynamoDB** — a key-value/document database; you design around **partition key (PK)** and
  **sort key (SK)**. **Single-table design** — many entity types in one table, distinguished by key
  prefixes. **On-demand** — pay per request. **TTL** — items auto-delete after a timestamp.
  **Conditional write** — "update only if version = 7" (our optimistic locking). **GSI** — a
  secondary index (costs extra writes; we avoid unless needed).
* **S3** — file storage (buckets of objects). **CloudFront** — the CDN that serves S3 files fast
  worldwide and handles HTTPS. **OAC (Origin Access Control)** — lets only CloudFront read the bucket.
* **Cognito User Pool** — managed sign-up/sign-in; **Managed Login** — its hosted pages;
  **JWT** — the signed token proving who you are; **PKCE** — the safe login flow for browser apps.
* **ACM** — free TLS certificates. **Route 53** — AWS DNS (we use Hostinger DNS instead).
* **CloudFormation** — AWS's infrastructure-as-code language (a stack = one deployed template).
  **SAM** — a friendlier dialect for serverless apps + a CLI (`sam build/deploy/local`).
* **CloudWatch** — logs, metrics, dashboards, alarms. **SNS** — sends notifications (e-mail).
  **AWS Budgets** — e-mail when spend crosses a threshold. **EMF** — a log format that becomes metrics for free.
* **Infrastructure as Code (IaC)** — your cloud setup is a file in git, not clicks in a console.

* **Geo-restriction** — a CloudFront switch that drops requests from countries not on an allow-list (we allow only the US; a VPN whose exit is abroad counts as abroad — Q27). Blocked visitors get our static `blocked.html`. Free. **AWS Shield Standard** — automatic, free protection against network-level floods on CloudFront and API Gateway.
* **Origin-verify header** — a secret header CloudFront adds when it calls our API; the API rejects anything without it, so the API's own URL is useless to attackers.
* **Kill switch** — our small Lambda that, when an alarm or the $20 budget fires, sets the API's concurrency to 0, turns sign-ups off and disables the CDN. The project's "pull the plug" button.
* **Free plan / credits** — how AWS accounts created after July 2025 work: $100–200 of credits for 6 months and no billing beyond them; then upgrade to the Paid plan or the account closes. **Always Free** — usage that stays free on both plans (Lambda 1M requests/month, DynamoDB 25 GB, …).
* **SSM Parameter Store** — a free place in AWS to keep small config values and secrets (our origin-verify secret).

## React & web
* **SPA (Single-Page App)** — one HTML file; JavaScript swaps screens. Needs the "404 → index.html" trick on CloudFront.
* **Component** — a function that returns UI. **Props** — inputs. **State** — remembered values.
  **Hook** — a `useSomething` function that gives components powers.
* **Zustand store** — a shared state object components subscribe to.
* **React Router** — maps URLs to screens. **Vite** — the dev server and bundler. **TypeScript** —
  JavaScript with types (catches mistakes early). **ESLint/Prettier** — code rules / formatting.
* **Tailwind** — CSS utility classes (`p-4 text-gold`). **Design tokens** — named colours/sizes used everywhere.
* **dnd-kit** — drag-and-drop with keyboard support. **Motion** — animation library.
  **Howler** — audio library; **audio sprite** — one sound file containing many clips.
* **fetch** — the browser's HTTP call. **CORS** — browser rule about which sites may call an API.
  **ETag** — a version tag so the browser can skip re-downloading unchanged data.
* **MSW (Mock Service Worker)** — fakes the API in tests. **Vitest / Testing Library / Playwright** —
  unit / component / end-to-end test tools. **axe** — accessibility checker. **Lighthouse** — performance audit.
* **ARIA / live region** — attributes that tell screen readers what things are and announce changes.
* **`prefers-reduced-motion`** — a user setting we respect by shortening animations.

## Game & engine
* **Auto-battler** — you build a team; the fight resolves automatically.
* **Deterministic** — same inputs → same outputs, always. **Seed** — the number that makes random
  choices reproducible. **RNG** — random number generator.
* **Action** — a player intent (Buy, Roll…). **Resolver** — applies rules. **Mutator** — one tiny
  state change. **Event** — a fact about what happened. **Event log** — the ordered list of events;
  the client animates it. **Event sourcing** — storing/transmitting events rather than only final state.
* **Trigger** — the moment an ability fires (Fall, Hurt…). **Target** — who it affects. **Effect** — what it does.
* **DSL (Domain-Specific Language)** — our small JSON vocabulary for abilities.
* **Ghost** — a saved copy of another player's band used as an opponent. **Matchmaking** — picking it.
* **Lambdalith** — one Lambda running a whole (small) app.
* **Simulator / bot** — code that plays the game thousands of times to measure balance.
* **Scenario test** — a test written as data: starting board, what happens, expected events.

* **Elements palette** — FinalBossBlues' newer, deeper colour set; all our art is converted into it so sprites match.
* **Cameo** — a hero named after a character from *The Forgotten Kanji* (Aoi, Hotaru, Iwao, Kashi, Kaede, Nagi).

## Operations
* **Environment** — a complete copy of the system (`dev`, `prod`). **Deploy** — put a version live.
  **Rollback** — go back to the previous version. **Smoke test** — a quick "is it alive?" check after deploy.
* **Runbook** — what to do when an alarm fires. **Observability** — logs + metrics + traces so you can see inside.
* **Idempotent** — doing it twice has the same effect as once (safe retries).
* **Optimistic locking** — assume no conflict; detect it with a version number; retry if wrong.
