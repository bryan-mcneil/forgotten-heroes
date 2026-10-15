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
* **Branch protection** — rule: nobody (including you) merges to `main` without green CI. Ours also requires a straight-line history (squash merges only) and forbids force pushes; the settings live in `infra/github/branch-protection.json`.
* **GitHub Actions: workflow / job / step** — a *workflow* is one YAML file in `.github/workflows/` that says *when* to run (`on: pull_request`). It holds *jobs*; each job runs on its own fresh computer, a *runner* (`ubuntu-latest`). A job is a list of *steps*: a shell command (`run:`) or a reusable action someone published (`uses: actions/checkout@v7`). Ours: workflow `ci`, job `docs`, three steps.
* **Status check** — the green tick or red cross a job reports back to the PR. Branch protection can require a check by its job name (`docs`).
* **Linter** — a program that reads files and complains about rule violations without running anything: `markdownlint` for Markdown now, ESLint for TypeScript later. Rules are switched on and off in a config file (`.markdownlint-cli2.yaml`).
* **Dependabot** — GitHub's robot that opens a PR when something you depend on has a new version. Ours watches the actions used in the workflows, weekly; Maven and npm are added when those files exist.
* **YAML** — the indentation-based text format used for workflows, issue forms and (later) SAM templates: `key: value`, lists start with `-`. Two spaces per level, never tabs; a wrong indent changes the meaning without an error.
* **`git init`** — turns a plain folder into a repository by creating the hidden `.git/` folder; `-b main` names the first branch.
* **Remote / `origin`** — a copy of the repo somewhere else (GitHub). `origin` is the conventional name for the main one; `git push` sends commits to it.
* **`gh` (GitHub CLI)** — GitHub from the terminal: `gh repo create`, `gh pr create`, `gh issue list`. Our scripts use it so nothing has to be clicked.
* **`.gitignore`** — paths Git must pretend do not exist: build output, secrets, the `Super Auto Pets/` folder. A trailing `/` means "directory". `git check-ignore -v <path>` tells you which rule matched.
* **Line endings (LF vs CRLF)** — the invisible character(s) that end a line. Linux, macOS and Git Bash use LF (`\n`); Windows tools use CRLF (`\r\n`). A bash script saved with CRLF fails with `bad interpreter: ^M`; a `.cmd` file saved with LF can confuse `cmd.exe`.
* **`.gitattributes`** — per-path rules for Git: which files are text (and which line ending they get on checkout) and which are binary. Ours: everything LF, except `.cmd`/`.ps1` (CRLF); images and audio binary.
* **EditorConfig (`.editorconfig`)** — a tiny file every editor understands: indent size, charset, line endings, final newline. Keeps formatting out of code review.
* **Licence (MIT)** — a short permissive licence: anyone may use, copy and modify the code as long as the copyright notice stays. Ours covers the code only; art and audio are not in the repo.
* **Monorepo** — several projects (engine, backend, frontend, infra, tools…) in one repository with one history, so one PR can change the engine, the API and the docs together. Git tracks files, not folders: an empty folder does not exist to Git, so each folder in the skeleton holds a `README.md` that says what lives there, what must not, and how to run it.
* **Issue** — one unit of work on GitHub: a number (`#12`), a title, a body, labels, a milestone, open or closed. Ours: one issue per step, titled `Step NN — Title`; the PR says `Closes #12` and merging it closes the issue.
* **Label** — a coloured tag on an issue or PR, for filtering: `phase:0`, `area:engine`, `type:step`. The colour is a 6-digit hex code (`0E8A16`).
* **Milestone** — a named bucket of issues with a progress bar. One per phase: `Phase 0 — Foundations`.
* **Project (board)** — GitHub's kanban: cards (issues) in columns. The "column" is really a field called *Status* whose options we rename to Backlog → Next → In progress → In review → Done; a built-in rule moves a closed issue to Done.
* **Issue form / PR template** — a YAML file in `.github/ISSUE_TEMPLATE/` turns *New issue* into a small form with required fields (`step.yml`, `bug.yml`, `idea.yml`); `pull_request_template.md` pre-fills every PR body.
* **`gh api`** — a raw call to GitHub's HTTP API from the terminal: `gh api repos/:owner/:repo/milestones` (`:owner/:repo` is filled from the current repo, `-f key=value` sends a field, `--jq` filters the JSON reply). Every other `gh` command is one of these calls with a friendlier name.
* **Scope (OAuth)** — one permission on a login token: `repo`, `read:org`, `project`. `gh auth refresh -s project` asks GitHub to add one; a missing scope fails with "missing required scopes".
* **Dry run** — a `--dry-run` flag that prints what a script *would* do and changes nothing. Cheap insurance before 77 API calls; see also *Idempotent* under Operations.

## Java & build
* **JDK** — the Java toolkit (compiler + runtime). **LTS** = long-term-support version (21, 25).
* **Maven** — Java's build tool: it downloads the libraries the code needs, compiles, runs the tests and packages the result from one command. It reads its instructions from `pom.xml`.
* **Maven Wrapper (`mvnw`)** — two committed scripts (`mvnw`, `mvnw.cmd`) that download the exact Maven version named in `.mvn/wrapper/maven-wrapper.properties` into `~/.m2/wrapper` on first use. Nobody installs Maven; every build command is `./mvnw …`. Ours is the *script-only* flavour (no jar in the repo).
* **`pom.xml` / parent pom** — Maven's description of a project. The root pom is the *parent*: it lists the modules and pins every version once; each module's pom names it in `<parent>` and inherits the rest.
* **Module** — a sub-project (`engine`, `backend`, `sim`) with its own `pom.xml`.
* **Dependency** — a library you use by naming it in the pom; Maven downloads it into `~/.m2/repository`.
* **BOM (Bill of Materials)** — a pom that only lists library versions known to work together. We import Spring Boot's (`spring-boot-dependencies`) so JUnit, AssertJ and Jackson get tested versions without typing a number; importing it adds no Spring code.
* **`dependencyManagement` vs `dependencies`** — the first says "if a module asks for library X, use version Y" and downloads nothing; the second actually adds the library to a module. Same split for plugins: `pluginManagement` pins versions, `plugins` runs them.
* **Plugin / goal / execution** — a plugin is a piece of Maven that does one job (compiler, surefire, JaCoCo, enforcer); a goal is one task of it (`jacoco:check`); an execution binds a goal to a point in the build.
* **Phase (build lifecycle)** — Maven runs phases in a fixed order: `validate → compile → test → package → verify`. Asking for `verify` runs everything before it; a failing phase stops the rest, so nothing downstream happens on red.
* **Reactor** — the list of modules Maven builds in one run, parent first (the "Reactor Summary" at the end). `-pl engine` limits it to one module; `-N` builds only the pom you are in.
* **Scope `test`** — a dependency used only to compile and run tests; it never ships with the code.
* **Surefire** — the plugin that runs the JUnit tests and writes `target/surefire-reports/`. Always read the `Tests run:` line: code saved in the wrong folder gives a green build with zero tests.
* **Enforcer** — the plugin that refuses to build on the wrong Java or Maven version (`[25,26)` = 25 or higher, below 26).
* **Coverage gate** — JaCoCo's `check` goal: the build fails if too few code lines ran under a test (60 % now, 90 % from Step 23). The HTML report is `engine/target/site/jacoco/index.html`.
* **Maven flags** — `-B` batch mode (plain output, what CI uses) · `-ntp` no download progress bars · `-pl <module>` only that module · `-N` do not recurse into modules · `-Dtest=BandTest` run one test class · `-v` print the version.
* **`src/main/java` / `src/test/java`** — Maven's folder convention for real code and test code; no configuration names them. Below each, the folders mirror the package name (`com/forgottenheroes/engine`).
* **`target/`** — Maven's build output (classes, jar, reports); gitignored and recreated on every build.
* **Package** — Java's way of grouping classes, like a namespace; the `package` line at the top of a file must match its folder. `package-info.java` is a file that holds only the package's Javadoc.
* **Javadoc** — the `/** … */` comment style that tools turn into documentation.
* **Annotation** — a label on code that tools read, written `@Name`. `@Test` tells JUnit "this method is a test".
* **`import static`** — lets you call a static method by its short name: `assertThat(…)` instead of `Assertions.assertThat(…)`.
* **Smoke test** — the smallest test that proves the machine turns on; `SmokeTest` checks `1 + 1 == 2`. A failing assertion prints `expected: 3 / but was: 2` and the file and line to open.
* **Record** — a class that only carries data: you name the fields once and Java writes the constructor, the getters, `equals`, `hashCode` and `toString` (`record Stats(int attack, int health)`). Two records with the same values are equal.
* **Immutable** — a value that never changes after it is made. To "change" a record you make a new one (`stats.withAttack(5)` returns a new `Stats`; the original is untouched). Safe to share, easy to test, no hidden state.
* **Compact constructor** — the short block inside a record, written `public Stats { … }` with no parameter list, that checks the incoming values before Java stores them (`if (attack < 0) throw new IllegalArgumentException(…)`). It refuses bad values; it never fixes them.
* **`IllegalArgumentException`** — the standard exception for "you called me with a value that makes no sense". Our value types throw it; tests catch it with `assertThatThrownBy(() -> …)`.
* **Enum** — a type with a fixed list of named values and nothing else (`enum Rank { I, II, III, IV, V, VI }`), so `rank = 7` cannot compile. Values can carry data and methods (`Rank.VI.turnUnlocked()`); `values()` is the list in order and `ordinal()` a value's position in it, from 0.
* **Constant (`static final`)** — one shared value that never changes (`Rules.BAND_SIZE = 5`). Every rulebook number lives in `Rules`; nothing else may hard-code them. A constants holder has a private constructor so nobody writes `new Rules()`.
* **Derived value** — something computed from stored data instead of stored beside it: a hero's level is `Xp.level()`, never a second field that could disagree.
* **Nested class** — a class written inside another (`UnitId.Sequence`), used when a helper only makes sense next to its owner.
* **Lambda** — a small piece of code handed over to run later, written `() -> new Stats(-1, 3)`; AssertJ runs it and catches the exception.
* **Ternary `a ? b : c`** — reads "if a then b, else c" in one expression (`this == VI ? VI : values()[ordinal() + 1]`).
* **Parameterized test** — one test method run once per row of a table: `@ParameterizedTest` + `@CsvSource({"3, II", …})`; JUnit converts `"3"` to an `int` and `"II"` to `Rank.II`. The GDD's turn and XP tables are tests this way.
* **Sealed interface** — an interface with a fixed, listed set of implementations; lets the compiler
  check we handled every `Action`/`GameEvent` type (`switch` exhaustiveness).
* **JUnit 5 / AssertJ** — test framework / readable assertions (`assertThat(gold).isEqualTo(7)`).
* **jqwik** — property-based testing: you state a rule ("gold never negative") and it tries thousands of random inputs.
* **ArchUnit** — tests about code structure ("engine must not import Spring").
* **JaCoCo** — measures test coverage. **PIT** — mutation testing: changes your code on purpose to see if tests notice.
* **Jackson** — converts Java objects ⇄ JSON.

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
* **Free plan / credits** — how AWS accounts created after July 2025 work: $100–200 of credits for 6 months and no billing beyond them; then upgrade to the Paid plan or the account closes. Creating an AWS Organization ends the Free plan on the spot (Q32). **Always Free** — usage that stays free on both plans (Lambda 1M requests/month, DynamoDB 25 GB, …).
* **SSM Parameter Store** — a free place in AWS to keep small config values and secrets (our origin-verify secret).
* **Root user** — the e-mail + password that created the account. It can do anything and cannot be limited, so it is used only for MFA, billing settings and the few "root-only" tasks; never for daily work or the CLI.
* **MFA (multi-factor authentication)** — a second proof at sign-in: a six-digit code from an authenticator app on your phone that changes every 30 seconds. A stolen password alone is then useless.
* **IAM user / policy / access key** — an IAM user is a named login inside the account (`bryan`). A policy is a JSON list of allowed actions attached to it (`AdministratorAccess` = everything, `SignInLocalDevelopmentAccess` = may use `aws login`). An access key is a permanent password pair for programs; we never create one.
* **`aws login` / profile / `AWS_PROFILE`** — `aws login` opens the browser, lets you sign in as usual and keeps *temporary* credentials (12 h at most) in `~/.aws/login/cache`. A profile is a named block in `~/.aws/config` (`[profile fh]`) holding the region and the sign-in to use; `export AWS_PROFILE=fh` picks it so you never type `--profile`.
* **AWS Organizations / IAM Identity Center** — Organizations groups several accounts under one management account; Identity Center is the single-sign-on service built on top of it (`aws configure sso`). Creating an Organization ends the Free plan (Q32), so both wait for the Paid-plan upgrade.
* **Budget alert: actual vs forecasted** — *actual* fires when money already spent this month crosses the line; *forecasted* fires when AWS predicts the month will cross it. The forecast needs about five weeks of usage history before it says anything.
* **Budget e-mail verification** — a new address gets a verification e-mail from `@aws.com` and receives nothing until the link is clicked while signed in to the account; once per address per account.
* **CloudFormation: template / stack / parameter** — the template is the YAML file (`infra/budgets.yaml`); a stack is one named deployment of it (`forgotten-heroes-budgets`) that CloudFormation creates, updates or deletes as a unit; a parameter is a value typed at deploy time (`Email=…`) so it stays out of git.
* **Alternate contacts** — three extra e-mail entries (billing, operations, security) on the Account page that AWS can write to besides the root e-mail.
* **Cost allocation tag** — a tag key (`project`) that, once *activated* on the Billing → Cost allocation tags page, becomes a filter in Cost Explorer. It can only be activated after a tagged resource has produced a bill line (Step 34).

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
