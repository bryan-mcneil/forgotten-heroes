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

TODO Step 25: `./mvnw -pl backend spring-boot:run -Dspring-boot.run.profiles=local`, then open
`http://localhost:8080/api/health`.
TODO Step 28: `docker compose up -d && ./mvnw -pl backend -am verify` for the DynamoDB-backed tests.
