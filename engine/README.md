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

From the repo root (the wrapper downloads Maven 3.9.16 into `~/.m2/wrapper` the first time):

```bash
./mvnw -pl engine -B -ntp verify             # compile, run every engine test, write the coverage report, apply the gate
./mvnw -pl engine -Dtest=SmokeTest test      # run one test class
start engine/target/site/jacoco/index.html   # open the coverage report (Git Bash on Windows)
```

Read the `Tests run:` line, not only `BUILD SUCCESS`: a test saved outside `src/test/java/...` is silently
skipped. The coverage gate is 60 % of lines (Step 07) and rises to 90 % in Step 23. CI runs the same
`./mvnw -B -ntp verify` in the `java` job.
