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
./mvnw -pl backend -am test-compile spring-boot:run   # then in another terminal:
curl http://localhost:8080/api/health          # {"status":"UP",...}
./mvnw -B -ntp verify
```

**Commit:** `step-25: backend module with health endpoint`

**Check your understanding:** What does "auto-configuration" mean, and where would you look to see what Spring configured for you? (hint: `--debug` flag / Actuator `conditions`)

**As built (2026-10-15):** Spring Boot 4.1.1 names the web starter `spring-boot-starter-webmvc` (not `-web`) and its test
starter `spring-boot-starter-webmvc-test`; `@WebMvcTest` moved to `org.springframework.boot.webmvc.test.autoconfigure` and
`@MockBean` is gone, replaced by Spring Framework's `@MockitoBean`. The Initializr zip was downloaded and read, not merged:
the backend pom was typed by hand with our root pom as parent, so the Spring Boot BOM already there supplies every version.
`spring-boot-maven-plugin` is `skip=true` in the root `pluginManagement` and `skip=false` in `backend` (the Step 22 exec-plugin
trick), otherwise `-am` runs `spring-boot:run` on the root pom and fails with "unable to find a suitable main class"; the run
command therefore adds `test-compile` so `engine` is compiled in the same reactor. The sha comes from `GitProperties`
(auto-configured from `git.properties`), not `BuildProperties`; the plugin's full-id key is `git.commit.id`, so the include
pattern is `^git.commit.id(.abbrev)?$`. The controller lives in the `web` layer package. Two tests: the `@WebMvcTest` slice
with a `@MockitoBean GitProperties` and `MockMvcTester`, and the Initializr `@SpringBootTest contextLoads` that boots the
whole app in CI with the real `git.properties`. JaCoCo reports (75 % lines) but the backend gate waits for Step 36.

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

**As built (2026-10-16):** four commits on one branch (shapes, routes, errors, contract). `ActionRequest` is a sealed
interface with eight records told apart by `"type"` (`@JsonTypeInfo` SIMPLE_NAME, like the engine's `Action`); its
numbers are `Integer`, not `int`, because Jackson 3 refuses to map a missing field into a primitive
(`FAIL_ON_NULL_FOR_PRIMITIVES` is on by default), so only a wrapper lets `@NotNull` report "slot must not be null" as a
tidy 400. `RunStateDto` carries no `runSeed` (it would reveal future rolls). End-turn takes a body `{expectedVersion}`
and `POST /runs` answers 201. `DtoMapper` copies engine state to DTOs and turns requests into engine actions with a
switch over the sealed type. Services are stubs throwing `UnsupportedOperationException`, which the handler maps to
**501** until Step 27. The handler extends `ResponseEntityExceptionHandler`, so Spring's own ProblemDetails (unknown
route, bad path variable …) also get a `code`; `spring.mvc.problemdetails.enabled` is therefore not needed. Unknown JSON
fields are rejected only because `application.yaml` sets `spring.jackson.deserialization.fail-on-unknown-properties:
true` (the MVC mapper accepts them otherwise); `default-property-inclusion: non_null` leaves absent values out. The
engine's `EngineJsonModule` reaches Spring's mapper through `ApiJsonModule`, a `@Component` module (a `JacksonModule`
bean is picked up by `@WebMvcTest` slices; a `@Configuration` class is not). Spring 7 no longer guesses `@PathVariable`
names from reflection, so the root pom compiles with `-parameters` (the Initializr pom has it; our hand-typed one did
not). springdoc **3.1.1** (the Spring Boot 4 line), `springdoc.paths-to-match: /api/**`,
`default-produces-media-type: application/json`, and `SpringDocUtils.replaceWithSchema` so `UnitId`/`Position` are
strings and `Rank` a number in the contract; `application-prod.yaml` turns api-docs and Swagger UI off (tested with
`@ActiveProfiles("prod")`). **Deviation:** no `springdoc-openapi-maven-plugin`. It needs the app started on a port
during `verify` (and, from Step 28, a DynamoDB), so `OpenApiContractTest` (`@SpringBootTest` + MockMvc) fetches
`/v3/api-docs` itself, writes `backend/target/openapi.json` and compares it with
`backend/src/test/resources/openapi.snapshot.json`; refresh on purpose with
`./mvnw -pl backend -am -Dtest=OpenApiContractTest -Dopenapi.snapshot.update=true test`. Events and actions appear as
`oneOf` with a `type` discriminator, validation becomes `minimum: 0` and `required`. 24 backend tests.

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

**As built (2026-10-16):** `repo/` holds four interfaces (`RunRepository`, `GhostRepository`, `BattleLogRepository`,
`ProfileRepository`), the stored records (`Profile`, `Ghost`, `BattleLog`) and the `InMemory*` implementations; the run
repository stores JSON text through `RunStateJson`, exactly as DynamoDB will, so a caller always gets a fresh object
back. **Deviation:** the in-memory beans are plain `@Repository`, not `@Profile("local") @Primary`: with the profile
condition the default run (`spring-boot:run` without a profile, `@SpringBootTest`) would have no repositories at all
until Step 28 supplies the DynamoDB ones; Step 28 decides which profile each set belongs to. `MatchmakingService` is an
interface and `InMemoryGhostPool` returns the mirror only (name `"Mirror"`); `RunService` saves the player's ghost
itself from `TurnResult.ghost()`. `security/CurrentUser` is an interface with `DevCurrentUser` answering `"dev"`;
controllers pass `user.id()` down, services never see a request. `SeedSource` (interface, `SecureSeedSource` bean) draws
the run seed so tests can replay a fixed one. `RunService` checks `expectedVersion` against the saved version before the
engine runs (409 `VERSION_CONFLICT`); Step 29 keeps the DynamoDB conditional write as the guard against two writers.
`POST /runs` while a run is in progress is 409 `RUN_ALREADY_ACTIVE` with `activeRunId`. The engine gained
`GameSession.abandon(state)` (one `RunEnded`, status `ABANDONED`, version bump; refused once over), since only the
engine may end a run. `EndTurnResponse` now carries `battle` (the battle's events, the same shape the replay route
returns, cut from the turn's events between `BattleStarted` and `BattleEnded`), `events` (the whole turn in order for
the playback engine) and `state`; the OpenAPI snapshot was refreshed on purpose. Leaderboard stays 501 until Step 32.
Tests: `RunServiceTest` (plain JUnit, real engine, in-memory repos: three turns against the mirror, stale version, rule
violation leaves the saved run untouched, owner isolation, abandon, same seed = same shop), `ApiHappyPathTest`
(`@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`, which in Boot 4 needs `spring-boot-starter-restclient` in test
scope), controller tests with a mocked `CurrentUser`. **Gotcha:** with VS Code open, its Java extension rebuilt
`target/classes` without `-parameters`, Maven then saw the classes as up to date and `@PathVariable` failed with a 500;
`./mvnw -pl backend -am clean test` fixes it. 34 backend tests, 386 in all.

---

## Step 28 — DynamoDB repositories, Testcontainers, local profile
**Split on 2026-10-16** into 28-a and 28-b (teaching method v2, `knowledge/teaching_method.md`): one new idea per
step. Both PRs point at issue #30; 28-b closes it. `LeaderboardItem` moved to Step 32 (no interface exists before it);
gzip of battle logs over 100 KB moved to Step 36 (a size-guard test stands in).

### Step 28-a — DynamoDB Local by hand, the table, and the first repository
**Branch:** `step-28a-dynamodb-local` · **est 3h**

**Goal:** a real DynamoDB running on your machine, one entity (the profile) saved in it by the API, and a test
that proves it against a throwaway container.

**You will have:** `docker-compose.yml` (`amazon/dynamodb-local` on 8000, `aaronshaf/dynamodb-admin` on 8001);
`infra/table.json` (the `create-table` input: `PK`, `SK`, on-demand) and `scripts/create-local-table.sh` (creates
the table, then switches TTL on for `expiresAt`); the AWS SDK BOM in the root pom and `dynamodb-enhanced` +
`url-connection-client` in the backend pom; an `app.storage` switch (`memory` is the default, `dynamo` turns the
DynamoDB beans on) read by `@ConditionalOnProperty` on both repository sets; `DynamoConfig` (`DynamoDbClient` and
`DynamoDbEnhancedClient`; endpoint override and dummy credentials when `app.dynamodb.endpoint` is set); `Keys`;
`ProfileItem` (`@DynamoDbBean`) and `DynamoProfileRepository`; `application-local.yaml` (`app.storage: dynamo`,
endpoint `http://localhost:8000`); `TableDefinition` (reads `infra/table.json` and creates the table, used by the
tests); `DynamoProfileRepositoryIT` with Testcontainers (`@Testcontainers`, `GenericContainer`,
`@DynamicPropertySource`).

**Sensei notes**
- *What:* DynamoDB is a giant map, not a table with columns: the **partition key** picks a drawer, the **sort key**
  orders the folders inside it; you read one folder (`GetItem`) or one drawer in order (`Query`). **Single-table
  design** puts every entity in that one map, told apart by key prefixes. The **Enhanced Client** maps a Java class
  to an item. **Testcontainers** starts DynamoDB Local in Docker for the test and throws it away.
- *Why:* one table is one line in the SAM template, inside the always-free 25 GB, with no VPC. A test against the
  real thing catches key and mapping mistakes a mock hides.
- *How:* taught in chunks (the loop): 1 Docker and the compose file; 2 the table by hand with the CLI; 3 the SDK
  and the config bean; 4 the item and the repository (full worked example); 5 the Testcontainers test; 6 the
  switch and the `local` profile; 7 `GET /api/me` through Swagger, then the item in the admin UI.
- *Where:* `docker-compose.yml`, `infra/table.json`, `scripts/create-local-table.sh`, `backend/.../repo/dynamo/`,
  `backend/src/main/resources/application-local.yaml`.

**Verify:** `docker compose up -d && ./mvnw -pl backend -am verify` green; app started with
`-Dspring-boot.run.profiles=local`, `GET /api/me` in Swagger, and the item `USER#dev` / `PROFILE` visible at
`http://localhost:8001`.

**Commit:** `step-28a: DynamoDB Local, table definition, first repository with Testcontainers`

**Say it (answered by doing):** what a partition key and a sort key are, compared with a SQL primary key; why a
made-up access key works against DynamoDB Local; what Testcontainers replaces in the test.

**As built (2026-10-16):** chunks 1–5 typed by Bryan with the loop (two commits per evening session), chunks 6–7 built
by Claude at his request the same night. **Deviations:** the repository test is `DynamoProfileRepositoryTest`, not
`…IT`, because the build has Surefire only (no Failsafe) and `verify` must run it; it is plain JUnit + Testcontainers
with no Spring context and therefore no `@DynamicPropertySource` (it calls the two `DynamoConfig` bean methods by hand
and builds `DynamoProfileRepository` with `new`); `TableDefinition` lives in the test tree and reads
`../infra/table.json` (Maven runs the backend's tests from `backend/`); `scripts/create-local-table.sh` creates the
table only, TTL is switched on in 28-b together with the first `expiresAt`; `ProfileItem` maps the Java fields `pk`/`sk`
to the ledger's `PK`/`SK` with `@DynamoDbAttribute`, a mismatch the first test run found ("One of the required keys was
not given a value"); the `app.storage` condition sits on `InMemoryProfileRepository` only until 28-b gives the other
three repositories a DynamoDB twin, and `application.yaml` spells the default `app.storage: memory` out;
`DynamoProfileRepository` is a `@Repository` reading `app.dynamodb.table`. Verified: `verify` green with the
Testcontainers box, and `GET /api/me` under the `local` profile wrote `USER#dev` / `PROFILE` into the compose box
(`activeRunId` is simply absent, the Enhanced Client's `PutItem` leaves null fields out).

### Step 28-b — Run, ghost and battle-log repositories
**Branch:** `step-28b-dynamodb-repositories` · **est 3h**

**Goal:** the whole run lives in DynamoDB; a game played through Swagger leaves RUN, GHOST and BATTLE items behind.

**You will have:** `RunItem` and `DynamoRunRepository` (state as compact JSON through `RunStateJson`, plus `status`,
`turn`, `gold`, `hearts`, `crowns`, `version`, `updatedAt` for the admin view; `expiresAt` 90 days after the run
ends); `GhostItem` and `DynamoGhostRepository` (PK `GHOST#T<turn>`, SK `<crowns, two digits>#<ISO time>#<runId>`,
band as JSON, TTL 30 days; query newest first); `BattleLogItem` and `DynamoBattleLogRepository` (PK `RUN#<runId>`,
SK `BATTLE#<turn, three digits>`, events as JSON, TTL 7 days; a test that fails if an item passes 300 KB);
Testcontainers tests for all three: save/load a run (JSON round trip equal), ghosts by turn newest first, battle
log save/load.

**Sensei notes**
- *What:* a **composite sort key** (`a#b#c`) is how one drawer answers several questions in order; **TTL** is an
  epoch-seconds attribute DynamoDB deletes items by, for free.
- *Why:* the ghost drawer is read by turn and filtered by crowns (Step 30), so crowns must come first in the key;
  ghosts and battle logs expire on their own, so storage stays near zero.
- *How:* the loop again, faded: `RunItem` is a worked example, `GhostItem` is a skeleton with blanks, `BattleLogItem`
  is labels only.
- *Where:* `backend/.../repo/dynamo/`.

**Verify:** `docker compose up -d && ./mvnw -pl backend -am verify` green; a full run through Swagger with the
`local` profile shows RUN, GHOST and BATTLE items at `http://localhost:8001`.

**Commit:** `step-28b: run, ghost and battle-log DynamoDB repositories`

**Say it (answered by doing):** why the ghost's sort key is `crowns#time#runId` rather than just `time`.

**As built (2026-10-17, by Claude at Bryan's request):** `RunItem` (PK `USER#<userId>`, SK `RUN#<runId>`; `state` is the
`RunStateJson` text, `status`/`turn`/`gold`/`hearts`/`crowns`/`version`/`updatedAt` copied out for the admin page,
`expiresAt` only once the run is over, 90 days), `GhostItem` (PK `GHOST#T<turn>`, SK `<crowns, 2 digits>#<time, fixed
millisecond ISO>#<runId>`, `band` as the engine's JSON through `EventJson.mapper()`, `bot`, `expiresAt` 30 days after
`savedAt`), `BattleLogItem` (PK `RUN#<runId>`, SK `BATTLE#<turn, 3 digits>`, `events` through the typed
`EventJson.toJson(list)` so every element keeps its `type`, `expiresAt` 7 days). `Keys` spells all of it, including the
fixed-width time format (a bare `Instant.toString()` drops trailing zeros, and `"…26Z"` would sort after `"…26.728Z"` as
text). **Deviations:** `Ghost` gained `runId`; `findByTurn` reads the drawer with `scanIndexForward(false)` (highest
crowns first, newest first within equal crowns); `DynamoBattleLogRepository` refuses an item over 300 KB with an
`IllegalStateException` (DynamoDB's hard limit is 400 KB; Step 36 adds gzip) and the test proves both the refusal and
that a real battle stays far under it; the three remaining in-memory repositories got the `app.storage=memory`
condition, so `local` now runs entirely on the box; TTL is switched on by the create script (idempotent, it asks
`describe-time-to-live` first) and by `TableDefinition`. Tests: `DynamoLocal` (test helper: the container recipe, the
connection through `DynamoConfig`, a raw `GetItem` for attribute-level checks) and one class per repository, each with its
own box. Verified: `verify` green; under the `local` profile a run played through the API left `USER#dev`/`RUN#…`,
`GHOST#T1`/`00#…`, and `RUN#…`/`BATTLE#001` in the compose box.

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

**As built (2026-10-17, by Claude at Bryan's request):** `RunRepository.save(userId, state, expectedVersion)` is the
whole interface change; `expectedVersion` is the version the caller *loaded* (`RunRepository.NEW_RUN`, 0, for a run that
does not exist yet), never the one the engine produced since. `DynamoRunRepository` sends a `PutItemEnhancedRequest`
with the condition `attribute_not_exists(PK) OR #v = :expected` (`#v` → `version`, `:expected` → the number) and turns
`ConditionalCheckFailedException` into `VersionConflictException` after one extra `GetItem` for `currentVersion`;
`InMemoryRunRepository` does the same check inside `ConcurrentHashMap.compute`. `RunService` passes the client's
`expectedVersion` on `apply` and `endTurn`, the loaded version on `abandon`, `NEW_RUN` on `start`, and keeps
`requireVersion` as the quick, friendly 409 before the engine runs. **Deviations:** `VersionConflictException` lives in
`repo` now; the race test is `RunServiceConcurrencyTest` (`@RepeatedTest(20)`, two threads behind a `CountDownLatch`,
the real service on the four DynamoDB repositories, one box) rather than a script looping Maven twenty times; nothing in
the API changed (`expectedVersion` has been `@NotNull` since Step 26, the 409 body with `currentVersion` since Step 27),
so the OpenAPI snapshot is untouched. Verified: `verify` green including the twenty repetitions, each leaving exactly
one `VersionConflictException`, one `ActionResponse`, and the stored version one step ahead.

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

**As built (2026-10-17, by Claude at Bryan's request):** `GhostRepository.findCandidates(turn, crownsMin, crownsMax,
limit)`: `QueryConditional.sortBetween` on `GHOST#T<turn>` from `Keys.ghostLowerBound(min)` (`"06#"`) to
`Keys.ghostUpperBound(max)` (`"09#"`, max + 1, which no real key equals), `scanIndexForward(false)`, `limit` as the read
cap, and only the first `Page` is taken (a `PageIterable` would fetch further pages). `GhostMatchmaking` (`@Service`,
the one `MatchmakingService`; `InMemoryGhostPool` deleted) clamps the window to 0..10, drops the player's own ghosts,
prefers `bot=false`, then `bot=true`, then the mirror; the pick uses `new GameRandom(SeedDerivation.battleSeed(runSeed,
turn)).fork("matchmaking")`, so a replay with the same candidates chooses the same ghost and the battle's rolls never
shift. **Deviations:** `MatchmakingService.pick(userId, run)` takes the whole `RunState` (seed, turn, crowns, band) instead
of four arguments; the opponent name is `Ghost of <ownerUserId>` until Step 32 copies display names onto ghosts;
fallbacks are logged as `MatchmakingFallback kind=bot|mirror` and the metric name is a constant, EMF comes in Step 35;
`tools/seed-ghosts/seed-ghosts.sh` only prints what Step 72 will do; the ladder is tested in memory
(`GhostMatchmakingTest`: window, never self, same run same pick, bots only when no player, mirror when empty, other turns
are other drawers) and the window Query against the box (`DynamoGhostRepositoryTest`); `RunServiceTest` and the
happy-path test still meet the mirror because a single user's own ghosts are excluded. The in-memory ghost repository
sorts like the sort key now, so both storages answer in the same order.

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
`/api/content`, CORS placeholder), `Table` (from `infra/table.json`), `UserPool` + `UserPoolClient` +
`UserPoolDomain`, `Parameters: Env`, tags `project=forgotten-heroes`, `Outputs`.

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
