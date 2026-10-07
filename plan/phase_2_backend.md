# Phase 2 — Backend: Spring Boot 4.1 on AWS Lambda (Steps 25–36)

**Where you are:** the engine is complete and tested. **At the end of this phase:** an HTTPS API
running on Lambda in your `dev` AWS stack, saving runs in DynamoDB, protected by Cognito JWTs,
observable in CloudWatch — and still a normal Spring Boot app on your laptop.

Package root: `com.forgottenheroes.backend`. Contract: `knowledge/architecture.md` §4–§7.
Costs: `knowledge/cost_estimator.md`. Everything here must keep working on `./mvnw spring-boot:run`.

---

## Step 25 — `backend` module from Spring Initializr
**Branch:** `step-25-backend-skeleton` · **est 2h**

**Goal:** a Spring Boot app in the monorepo that answers `/api/health`.

**You will have:** `backend/` module (parent = root pom, module added), `BackendApplication`,
`HealthController` (`GET /api/health → {"status":"UP","version":"<git sha>"}`),
`application.yaml` (`server.port: 8080`, `spring.application.name`), `HealthControllerTest` (`@WebMvcTest`).

**Sensei notes**
- *What:* Spring Boot auto-configures an embedded web server; a `@RestController` method returns
  JSON. `@WebMvcTest` starts only the web layer for fast tests.
- *Why Initializr:* it generates a known-good project; we then make it a module of our parent.
- *How:* download a starter zip and merge:
  ```bash
  curl -o starter.zip "https://start.spring.io/starter.zip?type=maven-project&language=java&bootVersion=4.1.0&javaVersion=25&groupId=com.forgottenheroes&artifactId=backend&name=backend&packageName=com.forgottenheroes.backend&dependencies=web,validation,actuator"
  ```
  Unzip into `backend/`, delete its `mvnw` (we have the root one), change its `<parent>` to our
  root pom, and keep `spring-boot-maven-plugin`. Add `<module>backend</module>` to the root.
- *Where:* `backend/`.

**Do this:** as above; add `engine` as a dependency; expose the git sha via
`git-commit-id-maven-plugin` → `git.properties` → `BuildProperties`.

**Verify:**
```bash
./mvnw -pl backend -am spring-boot:run      # then in another terminal:
curl http://localhost:8080/api/health          # {"status":"UP",...}
./mvnw -B -ntp verify
```

**Commit:** `step-25: backend module with health endpoint`

**Check your understanding:** What does "auto-configuration" mean, and where would you look to see what Spring configured for you? (hint: `--debug` flag / Actuator `conditions`)

---

## Step 26 — API design: DTOs, validation, errors, OpenAPI
**Branch:** `step-26-api-contract` · **est 3h**

**Goal:** the HTTP contract exists (with stubs), documented and exported.

**You will have:** DTO records in `web.dto` (`RunStateDto`, `BandDto`, `UnitDto`, `TavernDto`,
`ActionRequest` (sealed by `type`), `ActionResponse{state, events}`, `EndTurnResponse{battle, state}`,
`BattleDto{opponentName, events, result}`, `LeaderboardEntryDto`, `ProfileDto`); controllers with
stubbed service calls for every route in architecture §4.2; `GlobalExceptionHandler`
(`@RestControllerAdvice`) mapping `RuleViolation → 422 ProblemDetail` with `code`, validation →
400, not found → 404, version conflict → 409; springdoc (`springdoc-openapi-starter-webmvc-ui`)
with `/v3/api-docs` and Swagger UI at `/swagger-ui.html` (disabled in prod profile);
`springdoc-openapi-maven-plugin` exporting `backend/target/openapi.json` during `verify`; a
contract snapshot test comparing it with `backend/src/test/resources/openapi.snapshot.json`.

**Sensei notes**
- *What:* **DTOs** are the wire shapes — separate from engine classes so the engine can change
  without breaking clients. **ProblemDetail** is the standard error JSON.
- *Why export OpenAPI:* the frontend generates TypeScript types from it (Step 42) — a typo in a
  field name becomes a compile error, not a runtime bug.
- *How:* `@Valid @RequestBody`, `@NotNull`, `@Size`; `spring.mvc.problemdetails.enabled: true`;
  `ProblemDetail.forStatusAndDetail(...)` + `setProperty("code", …)`.
- *Where:* `backend/.../web/`, `backend/src/main/resources/application.yaml`.

**Do this:** write DTOs + a `DtoMapper` (engine → DTO; Jackson for events can reuse `EventJson`);
stub controllers return `501` from services for now; write the exception handler and tests for
422/400/404 bodies; export and snapshot the OpenAPI file (update the snapshot deliberately when
the API changes — the test message says how).

**Verify:** `./mvnw -pl backend -am verify`; open `http://localhost:8080/swagger-ui.html` and try `/api/health`.

**Commit:** `step-26: API contract, DTOs, error handling and OpenAPI export`

**Check your understanding:** Why 422 for a rule violation instead of 400? Why keep DTOs separate from engine records?

---

## Step 27 — Service layer with in-memory repositories
**Branch:** `step-27-services-inmemory` · **est 3h**

**Goal:** the API works end-to-end on your laptop with no database yet.

**You will have:** `RunService` (`startRun(userId)`, `getRun(userId, runId)`, `applyAction(...)`,
`endTurn(...)`, `abandon(...)`) using `GameSession`; `MatchmakingService` interface with an
`InMemoryGhostPool` (mirror fallback only for now); repository **interfaces** (`RunRepository`,
`GhostRepository`, `BattleLogRepository`, `ProfileRepository`) + `InMemory*` implementations
(`@Profile("local") @Primary` for now); `ProfileService` creating a profile on first `/me`;
service tests with the in-memory repos; controllers call services (no more 501).

**Sensei notes**
- *What:* the **service layer** holds use-cases; **repository interfaces** hide storage.
- *Why in-memory first:* you learn Spring wiring without DynamoDB noise, and the tests stay fast forever (services are tested against in-memory repos; repos get their own tests in Step 28).
- *How:* constructor injection (`final` fields, one constructor — no `@Autowired` on fields);
  `@Service`, `@Repository`; the `userId` comes from a `CurrentUser` bean (a stub returning `"dev"` until Step 31).
- *Where:* `backend/.../service/`, `backend/.../repo/`.

**Do this:** implement; `RunServiceTest` plays a 3-turn run through the service; controller tests
now use `@MockitoBean` services or a `@SpringBootTest` slice with in-memory repos — pick
`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` for one happy-path test
(`POST /runs` → `POST actions` → `POST end-turn`).

**Verify:** run the app; with Swagger UI: `POST /api/runs`, buy a hero, end the turn, see a battle in the JSON.

**Commit:** `step-27: services and in-memory repositories; API plays a full turn`

**Check your understanding:** Why do we inject interfaces rather than concrete classes into `RunService`?

---

## Step 28 — DynamoDB repositories, Testcontainers, local profile
**Branch:** `step-28-dynamodb` · **est 4h**

**Goal:** real persistence, tested against a real (local) DynamoDB.

**You will have:** `infra/table.json` (the table definition shared by tests and SAM: `PK`, `SK`,
TTL `expiresAt`, on-demand); `DynamoConfig` (`DynamoDbEnhancedClient`, endpoint override when
`app.dynamodb.endpoint` is set); `Dynamo*Repository` implementations with item classes
(`RunItem`, `GhostItem`, `BattleLogItem`, `ProfileItem`, `LeaderboardItem`) using the key schema
from architecture §5 (state stored as compact JSON string); `docker-compose.yml`
(`amazon/dynamodb-local` on 8000 + `aaronshaf/dynamodb-admin` on 8001); `scripts/create-local-table.sh`;
profile `local` pointing at `http://localhost:8000`; Testcontainers integration tests
(`@Testcontainers`, `GenericContainer("amazon/dynamodb-local")`) for every repository.

**Sensei notes**
- *What:* **single-table design** — different entities share one table, told apart by key prefixes.
  The **Enhanced Client** maps Java classes to items. **Testcontainers** runs DynamoDB Local in Docker just for the test.
- *Why:* one table = one line in the SAM template, free tier 25 GB, no VPC. Tests against the real
  engine catch mapping mistakes that mocks hide.
- *How:* `@DynamoDbBean` classes with `@DynamoDbPartitionKey`/`@DynamoDbSortKey`; keys built by
  `Keys.user(sub)`, `Keys.run(runId)`, `Keys.ghostPk(turn)`; TTL as epoch seconds.
- *Where:* `backend/.../repo/dynamo/`, `docker-compose.yml`, `infra/table.json`.

**Do this:** implement; `docker compose up -d`; create the table; run the app with
`-Dspring-boot.run.profiles=local`; integration tests: save/load a run (JSON round trip equals),
ghosts query by turn returns newest first, battle log save/load, profile upsert; `DynamoRepositoriesIT`
tagged so `-DskipITs` skips when Docker is absent (but CI always runs them).

**Verify:** `docker compose up -d && ./mvnw -pl backend -am verify` green; `http://localhost:8001` shows items after a run via Swagger.

**Commit:** `step-28: DynamoDB single-table repositories with Testcontainers`

**Check your understanding:** Why is the ghost's sort key `crowns#time#runId` rather than just `time`?

---

## Step 29 — Optimistic locking and idempotency
**Branch:** `step-29-optimistic-locking` · **est 2h**

**Goal:** two tabs or a double-click can never corrupt a run.

**You will have:** `version` on `RunItem`; `RunRepository.save(run, expectedVersion)` using a
conditional expression (`attribute_not_exists(PK) OR version = :expected`); `VersionConflictException
→ 409 ProblemDetail{code: VERSION_CONFLICT, currentVersion}`; `ActionRequest.expectedVersion`
required; `RunService.applyAction` compares and increments; a concurrency IT: two threads apply
different actions with the same version → exactly one 409.

**Sensei notes**
- *What:* **optimistic locking** assumes no conflict and detects one with a version number;
  **idempotency** here falls out of it (a retried action carries a stale version → 409 → client reloads).
- *Why:* serverless has no sticky sessions; two Lambdas may handle two requests for the same run simultaneously.
- *How:* DynamoDB `ConditionExpression`; catch `ConditionalCheckFailedException`.
- *Where:* `DynamoRunRepository`, `RunService`, exception handler.

**Verify:** the IT passes 20 times in a row (`-Dsurefire.rerunFailingTestsCount=0`, loop in a script).

**Commit:** `step-29: optimistic locking with conditional writes`

**Check your understanding:** What does the client do when it receives 409, and why is that safe for the player?

---

## Step 30 — Matchmaking with ghosts
**Branch:** `step-30-matchmaking` · **est 3h**

**Goal:** real opponents, with bots and mirrors as fallbacks.

**You will have:** `DynamoGhostRepository.findCandidates(turn, crownsMin, crownsMax, limit)` (query
`PK = GHOST#T<turn>`, `SK between`), `MatchmakingService.pickOpponent(run)` implementing
architecture §4.4 (exclude own sub, prefer `bot=false`, then `bot=true`, then mirror), ghost
write on end turn with TTL 30 d; `MatchmakingFallback` metric name reserved; `tools/seed-ghosts`
placeholder script (real seeding in Step 72); tests with a seeded table.

**Sensei notes**
- *What:* asynchronous PvP = fight snapshots. SAP's `ArenaMatch` / `ArenaMirror`.
- *Why fallbacks:* a brand-new deployment has zero ghosts; the game must still work.
- *How:* query a small window (25 items) and pick with the battle RNG so replays choose the same opponent given the same candidates (store the chosen ghost on the battle log anyway).
- *Where:* `MatchmakingService`, `DynamoGhostRepository`.

**Verify:** IT: 10 ghosts at turn 3 with crowns 0–4 → pick within `[c−1, c+1]`, never self; empty table → mirror; only bots → bot.

**Commit:** `step-30: ghost matchmaking with fallbacks`

**Check your understanding:** Why store the opponent band inside the battle log instead of a reference to the ghost item?

---

## Step 31 — Authentication (Cognito JWT) and the local dev user
**Branch:** `step-31-auth` · **est 3h**

**Goal:** every request belongs to a real user; locally you can still work without Cognito.

**You will have:** `spring-boot-starter-oauth2-resource-server`; `SecurityConfig` (stateless,
`/api/health` and `/api/content` public, everything else authenticated, CSRF off);
`spring.security.oauth2.resourceserver.jwt.issuer-uri: https://cognito-idp.us-east-1.amazonaws.com/<poolId>`
from env `COGNITO_ISSUER`; `CurrentUser` reads `sub` and `preferred_username` from the `Jwt`;
profile `local`: `LocalDevSecurityConfig` accepts header `X-Dev-User` (default `dev`) and no JWT;
ArchUnit rule: nothing in `prod` configuration or main code outside `security.local` references the
local config; a temporary Cognito **user pool created by hand in the console** for `dev` testing
(the SAM template owns it from Step 33 — note the id in `docs/aws-account-setup.md`).

**Sensei notes**
- *What:* a **resource server** validates JWTs issued by Cognito using its public keys (JWKS,
  cached). The `sub` claim is the stable user id.
- *Why both API Gateway and Spring validate:* defence in depth, and the app stays secure when run outside Lambda.
- *How:* `http.oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))`; `@AuthenticationPrincipal Jwt`.
- *Where:* `backend/.../security/`.

**Do this:** implement; tests: `@WebMvcTest` with `SecurityMockMvcRequestPostProcessors.jwt()`
→ 200; no token → 401; local profile + header → 200; ArchUnit rule green. Try a real token:
Cognito Hosted UI → copy the access token → `curl -H "Authorization: Bearer …"`.

**Verify:** `curl /api/me` without token → 401; with token → profile JSON; local profile works with the header.

**Commit:** `step-31: cognito jwt resource server and local dev auth`

**Check your understanding:** What is the difference between the `access` token and the `id` token, and which one do APIs expect?

---

## Step 32 — Leaderboard and profile
**Branch:** `step-32-leaderboard-profile` · **est 2h**

**Goal:** the social layer, minimal.

**You will have:** `GET /api/leaderboard?season=YYYY-MM` (top 50 from `LB#<season>`),
`LeaderboardService.record(run)` on run end (only WON/LOST runs, keyed by crowns desc, turns asc),
`PUT /api/me {displayName}` with validation (3–16 chars, letters/digits/spaces, blocklist), display
name stored on profile and copied into ghosts/leaderboard entries at write time.

**Sensei notes:** sort keys do the sorting (`99−crowns` zero-padded), so "top 50" is one `Query`
with `Limit 50`; copying the display name at write time avoids joins (NoSQL habit).

**Verify:** IT: three finished runs → ordered leaderboard; invalid names → 400.

**Commit:** `step-32: leaderboard and profile endpoints`

**Check your understanding:** What is the downside of copying display names into ghost items, and why is it acceptable here?

---

## Step 33 — Lambda handler, shaded jar, SnapStart, SAM template v1
**Branch:** `step-33-lambda-sam` · **est 4h**

**Goal:** the same Spring Boot app packaged for Lambda, with the infrastructure described in code.

**You will have:** dependency `com.amazonaws.serverless:aws-serverless-java-container-springboot4`;
`lambda/StreamLambdaHandler` (`SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(BackendApplication.class)`
built once in a static initializer); `lambda/SnapStartPriming implements org.crac.Resource`
(`beforeCheckpoint`: warm the handler with a fake `GET /api/health` and `GET /api/content`;
`afterRestore`: nothing — RNG seeds are per request); `maven-shade-plugin` producing
`backend/target/backend-lambda.jar` (exclude the Spring Boot repackaged jar: set
`spring-boot-maven-plugin` `<skip>true</skip>` for repackage or use a classifier);
`infra/template.yaml` v1 with `ApiFunction` (Runtime `java25`, Handler
`com.forgottenheroes.backend.lambda.StreamLambdaHandler::handleRequest`, MemorySize 1024, Timeout 20,
`SnapStart: {ApplyOn: PublishedVersions}`, `AutoPublishAlias: live`, env `TABLE_NAME`,
`COGNITO_ISSUER`, `SPRING_PROFILES_ACTIVE: prod`, `JAVA_TOOL_OPTIONS: -XX:+TieredCompilation -XX:TieredStopAtLevel=1`),
`HttpApi` (`$default` route → function, JWT authorizer on all routes except `/api/health` and
`/api/content`, CORS placeholder), `Table` (from `infra/table.json`), `UserPool` + `UserPoolClient`
+ `UserPoolDomain`, `Parameters: Env`, tags `project=forgotten-heroes`, `Outputs`.

**Sensei notes**
- *What:* Lambda calls `handleRequest(InputStream, OutputStream, Context)`; the adapter turns the
  HTTP API event into a servlet request. **SnapStart** snapshots the booted JVM at publish time.
  **SAM** describes resources; `sam build` compiles, `sam deploy` creates/updates the CloudFormation stack.
- *Why a shaded jar:* Lambda loads classes from a flat jar; Spring Boot's nested-jar layout is not supported.
- *How:* follow the serverless-java-container README (Spring Boot 4 section); `sam validate --lint`;
  `sam local invoke ApiFunction -e infra/events/health.json` to run the function in Docker locally.
- *Where:* `backend/.../lambda/`, `infra/template.yaml`, `infra/events/`.

**Do this:** implement; record an HTTP API v2 event JSON for `/api/health`; make `sam build`
use the Maven build (`Metadata: BuildMethod: makefile` or build the jar first and point `CodeUri`
at it — the simplest reliable approach: `CodeUri: ../backend/target/backend-lambda.jar` after
`./mvnw package`); add CI job `lambda-smoke` running `sam local invoke` with the health event.

**Verify:**
```bash
./mvnw -B -ntp -pl backend -am package -DskipTests
sam validate --lint -t infra/template.yaml
sam build -t infra/template.yaml
sam local invoke ApiFunction -e infra/events/health.json    # statusCode 200 in the output
```

**Commit:** `step-33: lambda handler, snapstart priming and SAM template`

**Check your understanding:** What exactly is saved in a SnapStart snapshot, and why must per-request randomness never be created during startup?

---

## Step 34 — First deploy to `dev`
**Branch:** `step-34-first-deploy` · **est 2h** (+≈ 2h for the console pass, Q31)

**Goal:** the API is live on the internet (dev).

**You will have:** `infra/samconfig.toml` with a `dev` environment (`stack_name =
forgotten-heroes-dev`, region, `capabilities = CAPABILITY_IAM`, `parameter_overrides = "Env=dev"`,
`resolve_s3 = true`, `tags`); `scripts/deploy-backend.sh dev` (package → `sam deploy
--config-env dev --no-confirm-changeset`); `docs/deploy.md`; cost allocation tag `project`
activated in Billing (now that tagged resources exist).

**Sensei notes:** `sam deploy --guided` the first time writes `samconfig.toml`; afterwards deploys
are one command. The stack's **Outputs** give the API URL and pool ids — paste them into
`docs/environments.md` (dev section).

**Console first (Q31):** before `sam deploy`, build the dev stack **once by hand in the AWS web console**
so every line of `template.yaml` has a face: DynamoDB → Create table (`PK`/`SK`, on-demand, TTL
`expiresAt`); Cognito → Create user pool (e-mail sign-in, app client, Managed Login domain); Lambda →
Create function (Java 25, upload the shaded jar, SnapStart on, env vars); API Gateway → HTTP API with a
JWT authorizer and a `$default` route to the function. Call `/api/health` through it, then **delete all
four** and let SAM recreate them. The numbered click-by-click walk-through is written when you reach this
step — the console UI changes too often to write it now.

**Do this:** `aws sso login; sam deploy --guided` → answer prompts → save config; test:
```bash
api=$(aws cloudformation describe-stacks --stack-name forgotten-heroes-dev --query "Stacks[0].Outputs[?OutputKey=='ApiUrl'].OutputValue" --output text)
curl "$api/api/health"
```
Create a user in the **dev** pool (console → Users → Create), log in through the Hosted UI URL from
the outputs, copy the access token, call `/api/me` and `POST /api/runs` with curl. Check CloudWatch
Logs for the first invocation's `RESTORE_REPORT` (SnapStart) and `REPORT` lines.

**Verify:** health 200 from the internet; a run created from curl appears in the DynamoDB console; `aws ce get-cost-and-usage` for today ≈ $0.

**Commit:** `step-34: dev environment config and deploy script`

**Check your understanding:** Read one `REPORT` log line and explain each field (Duration, Billed Duration, Memory Size, Max Memory Used, Init/Restore Duration).

---

## Step 35 — Observability
**Branch:** `step-35-observability` · **est 3h**

**Goal:** you can see what the system is doing without guessing.

**You will have:** `logstash-logback-encoder` JSON logs (`prod` profile; plain text in `local`),
`RequestLogFilter` writing one summary line per request (`method, path, status, ms, userId, runId,
requestId` from `x-amzn-trace-id`/Lambda context), MDC propagation, EMF metrics
(`software.amazon.cloudwatchlogs:aws-embedded-metrics`) for `RunsStarted`, `BattlesResolved`,
`RunsWon`, `MatchmakingFallback`; SAM additions: `LogGroup` with `RetentionInDays: 14`,
`AWS::CloudWatch::Dashboard` (API requests, p50/p95 latency, 5xx, Lambda errors/throttles/
init-duration, DynamoDB consumed units, custom metrics), 4 alarms (Lambda errors > 5/5 min, 5xx >
5/5 min, Lambda throttles > 0, DynamoDB throttles > 0) → SNS topic with your e-mail; `docs/runbook.md` v1.

**Sensei notes:** logs answer "what happened to this request"; metrics answer "how is the system
doing"; alarms wake you up. Everything here stays inside the CloudWatch free tier (4 custom
metrics, 4 alarms, 1 dashboard, < 5 GB logs).

**Verify:** deploy dev; make 20 requests; the dashboard shows them; force an error (bad JSON) and see the 5xx metric; `aws logs tail /aws/lambda/<fn> --follow` shows JSON lines.

**Commit:** `step-35: structured logging, metrics, dashboard and alarms`

**Check your understanding:** Why log one summary line per request instead of many debug lines in production?

---

## Step 36 — Hardening + Phase 2 review
**Branch:** `step-36-backend-hardening` · **est 2h**

**Goal:** sensible limits and headers before a browser ever talks to this API.

> Prod note: from Step 61 the browser reaches the API **through CloudFront on the same domain**
> (`heroes.bryanmcneil.pro/api/*`), so CORS matters mostly for local development. The US-only
> geo-restriction and the origin-verify header are configured there, not here.

**You will have:** CORS on the HTTP API (`AllowOrigins` = site origin parameter + `http://localhost:5173`
in dev; `AllowHeaders: Authorization, Content-Type`; methods GET/POST/PUT), request size limit
(`spring.servlet.multipart` off; reject bodies > 8 KB in a filter), stage throttling
(`DefaultRouteSettings: ThrottlingRateLimit 20, ThrottlingBurstLimit 50`), Actuator exposure limited
to `health` (no `/env` etc.), Dependabot `maven` ecosystem, `OWASP dependency-check` in a weekly
workflow (`security.yml`), `docs/reviews/phase-2.md`.

**Verify:** `sam validate --lint`; redeploy dev; `curl -X OPTIONS` shows CORS headers; a 100 KB body → 413; `ab -n 200 -c 50` → some 429s (throttling works). Phase review per quality doc §10; tag `phase-2-complete`.

**Commit:** `step-36: CORS, limits, throttling and phase 2 review`

**Check your understanding:** Why is throttling a cost control and not only a security control here?
