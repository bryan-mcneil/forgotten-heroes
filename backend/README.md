# backend — the API (Spring Boot on AWS Lambda)

## What lives here

- One Spring Boot application in package `com.forgottenheroes.backend`: controllers, DTO records,
  validation, RFC 9457 `ProblemDetail` errors and the exported `openapi.json` (Steps 25–26); services
  that drive the engine (Step 27); DynamoDB repositories with optimistic locking and matchmaking
  (Steps 28–30); Cognito JWT security (Step 31); leaderboard and profile (Step 32); the Lambda handler
  and SnapStart priming (Step 33); JSON logs, metrics and hardening (Steps 35–36).
- Its tests: MockMvc, service tests, and Testcontainers against DynamoDB Local.

## What must NOT live here

- Game rules. When the API needs a rule it calls `engine/` through `GameSession`; it never re-implements
  one, and it never trusts a rule computed by the client.
- Infrastructure. The SAM template that deploys this code lives in `infra/`.
- Secrets, AWS keys or `.env` files (gitignored). The `local` profile accepts an `X-Dev-User` header
  instead of a real token, so nothing secret is needed on a laptop.

## How do I run it

`./mvnw -pl backend -am test-compile spring-boot:run` from the repo root (`-am test-compile` compiles `engine` first; the
Spring Boot plugin is skipped on every other module), then `curl http://localhost:8080/api/health` answers
`{"status":"UP","version":"<short git sha>"}`. The sha is written into `target/classes/git.properties` at build time by
`git-commit-id-maven-plugin` and read by Spring's `GitProperties` bean. From Step 28 add `-Dspring-boot.run.profiles=local`.
Tests: `./mvnw -pl backend -am verify` (a `@WebMvcTest` slice for the controller and a `@SpringBootTest` that boots the app).

## The contract (Step 26)

- Routes live in `web/` (`RunController`, `ProfileController`, `LeaderboardController`, `ContentController`,
  `HealthController`); wire shapes in `web/dto/`; `DtoMapper` copies engine state out and turns an `ActionRequest`
  into an engine `Action`. The services in `service/` are the use-cases (Step 27): `RunService` (start, get, act,
  end turn, battle replay, abandon) drives the engine through `GameSession`; `ProfileService` creates a profile on the
  first `/me`; `ContentService` serves the loaded content; `LeaderboardService` stays 501 until Step 32.
- `repo/`: four interfaces (`RunRepository`, `GhostRepository`, `BattleLogRepository`, `ProfileRepository`) with
  `InMemory*` implementations until Step 28. `security/CurrentUser` says who is calling (`"dev"` until Step 31).
- Every error is an RFC 9457 `ProblemDetail` with a `code` (`GlobalExceptionHandler`): 422 rule violation (the
  engine's code), 400 `VALIDATION_FAILED` / `MALFORMED_JSON`, 404 `NOT_FOUND`, 409 `VERSION_CONFLICT`, 501
  `NOT_IMPLEMENTED`, 500 `INTERNAL_ERROR`. Never 403 (CloudFront owns it).
- With the app running: `http://localhost:8080/swagger-ui.html` (try it out) and `/v3/api-docs` (the JSON). Both are
  off under the `prod` profile.
- `backend/target/openapi.json` is written by `OpenApiContractTest` on every build and must equal
  `src/test/resources/openapi.snapshot.json`. When the API changes on purpose:
  `./mvnw -pl backend -am -Dtest=OpenApiContractTest -Dopenapi.snapshot.update=true test`, then review and commit
  the snapshot diff. Step 42 generates the frontend's TypeScript types from this file.

## Storage (Step 28)

- `app.storage` picks the repositories: `memory` (the default in `application.yaml`: maps, gone at restart) or `dynamo`
  (`application-local.yaml`: the `repo/dynamo/` classes against the table named by `app.dynamodb.table`). Both sets are
  `@Repository` beans guarded by `@ConditionalOnProperty`, so exactly one set exists at a time.
- A DynamoDB on your PC: `docker compose up -d` starts `amazon/dynamodb-local` on :8000 and a browser view of it on
  `http://localhost:8001`; `scripts/create-local-table.sh` creates `forgotten-heroes-local` from `infra/table.json`
  (safe to run twice). Then `./mvnw -pl backend -am test-compile spring-boot:run -Dspring-boot.run.profiles=local`
  and `GET /api/me` in Swagger writes the item `USER#dev` / `PROFILE` you can see on :8001.
- `DynamoConfig` builds the `DynamoDbClient` (region, the `url-connection-client` HTTP engine, and with
  `app.dynamodb.endpoint` set the local address plus made-up keys) and the `DynamoDbEnhancedClient` that maps a
  `@DynamoDbBean` such as `ProfileItem` to an item and back. `Keys` is the only place the key prefixes are spelled.
- Tests against the real thing: `DynamoProfileRepositoryTest` starts its own `amazon/dynamodb-local` box with
  Testcontainers (Docker must be running), creates the table from `infra/table.json` through `TableDefinition`, and
  round-trips a profile. `./mvnw -pl backend -am verify` runs it with everything else; it needs no compose box.

If a route answers 500 with "Name for argument … not specified … '-parameters'" on your laptop, the VS Code Java extension
rebuilt `target/classes` without that flag; `./mvnw -pl backend -am clean test` restores Maven's compile.
