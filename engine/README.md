# engine — the game rules (pure Java)

## What lives here

- The rules of Forgotten Heroes as code, in package `com.forgottenheroes.engine`: value types (Step 08),
  the deterministic random source (Step 09), the content loader (Step 10), the board and run model
  (Step 11), the event family (Step 12), the tavern and battle resolvers (Steps 13–18) and the
  `GameSession` facade the backend calls (Step 20).
- The content itself: `src/main/resources/content/heroes.json`, `relics.json` and `tokens.json`, checked
  against a JSON schema (Step 10; the full roster of 36 heroes and 16 relics lands in Step 19).
- Its tests: unit, scenario, property (jqwik) and ArchUnit rules, behind a JaCoCo coverage gate of
  90 % (Step 23). Step 24 rewrites this README with the rules as implemented, the event list and the
  ability DSL reference.

## What must NOT live here

- Spring, AWS SDKs, HTTP, databases, files or clocks. The engine is plain Java with no I/O; the backend
  talks to it only through `GameSession`.
- `Math.random()`, `new Random()` or `System.currentTimeMillis()`. Determinism is a hard rule (ADR-4):
  same seed and same actions must give a byte-identical event log, and ArchUnit fails the build if they
  appear.
- Anything the browser needs directly. The frontend reads heroes and relics from `GET /api/content`;
  it never gets a copy of this folder.

## How do I run it

TODO Step 07: `./mvnw -pl engine -B -ntp verify` runs every engine test with the coverage gate;
`./mvnw -pl engine -Dtest=BandTest test` runs one test class.
