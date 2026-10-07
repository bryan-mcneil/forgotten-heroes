# Quality & Testing Strategy

> "Great automated QA" means: every rule has a test, every bug becomes a test, and the computer
> blocks merges that break anything. Because the engine is deterministic, **every bug is
> reproducible from a seed plus a list of actions** — the single most valuable property for QA.

---

## 1. The pyramid (and what each layer is for)

| Layer | Tooling | Runs in | Catches |
|---|---|---|---|
| **Engine unit tests** (hundreds, ms each) | JUnit 5 + AssertJ | every build | wrong rule arithmetic, ordering, targeting |
| **Scenario tests** (table-driven JSON) | JUnit 5 dynamic tests | every build | each hero/relic doing exactly what its text says |
| **Property-based tests** | jqwik | every build | invariants under random actions: gold ≥ 0, band ≤ 5, hp of living units > 0, determinism, battle terminates |
| **Architecture tests** | ArchUnit | every build | engine importing Spring/AWS; `Math.random` in engine; local-auth leaking to prod config |
| **Backend slice tests** | `@WebMvcTest`, MockMvc | every build | HTTP contracts, validation, error bodies |
| **Backend integration tests** | Testcontainers (`amazon/dynamodb-local`) | every build (Docker required) | repository mapping, conditional writes, TTL attributes, queries |
| **Contract snapshot** | springdoc OpenAPI JSON diff | every build | accidental API changes (must be intentional + regenerate client types) |
| **Frontend unit** | Vitest | every build | store logic, playback timeline, formatting |
| **Frontend component** | React Testing Library + MSW | every build | cards render stats, drag/keyboard flows, error states |
| **End-to-end** | Playwright (Chromium; WebKit nightly) | PR label `e2e` + nightly + deploy smoke | real flows: sign in (dev auth) → start run → buy → reorder → end turn → replay → finish |
| **Accessibility** | axe-core in Playwright, manual keyboard run | PR + release | missing names, contrast, focus traps |
| **Performance** | Lighthouse CI budgets, bundle analyzer | PR (frontend) | regressions in size/LCP |
| **Mutation testing** | PIT (engine only) | weekly / optional | tests that pass even when the code is wrong |
| **Balance regression** | `sim` module thresholds | nightly | a content change that breaks win-rate bands or causes clash-cap draws |
| **Security** | `npm audit`, OWASP dependency-check, gitleaks | weekly | vulnerable deps, leaked secrets |

Targets: engine line coverage ≥ 90 % (JaCoCo gate), backend ≥ 80 %, frontend ≥ 70 % of
non-visual code. Coverage never decreases in a PR.

---

## 2. Scenario tests — the heart of content QA

One JSON file per hero/relic in `engine/src/test/resources/scenarios/<id>.json`:
```json
{
  "name": "Aoi's fall buffs a random ally",
  "seed": 42,
  "home": [ {"hero":"aoi","attack":2,"health":2}, {"hero":"farmhand","attack":1,"health":3} ],
  "away": [ {"hero":"merchant","attack":4,"health":1} ],
  "expectEvents": [
    {"type":"Clash"}, {"type":"DamageDealt","target":"home:0","amount":4},
    {"type":"UnitFell","unit":"home:0"},
    {"type":"AbilityTriggered","unit":"home:0","ability":"aoi"},
    {"type":"StatsChanged","unit":"home:1","attack":"1→2","health":"3→4"}
  ],
  "expectResult": "WIN"
}
```
A JUnit `@TestFactory` turns every file into a named test. Adding a hero without a scenario fails
a meta-test ("every hero id has ≥ 1 scenario").

---

## 3. Property tests worth their weight
* **Determinism**: same seed + same action list → identical event JSON (byte-for-byte).
* **Invariants after any legal action**: `0 ≤ gold ≤ 50`, band size ≤ 5, no unit with health ≤ 0
  remains, tavern slot counts match the turn table, frozen items persist across a roll.
* **Illegal actions never corrupt state**: a rejected action leaves `state` and `version` unchanged.
* **Battles terminate** within the clash cap for random bands up to 50/50 stats with random perks.
* **Serialization round-trip**: `state → JSON → state` is the identity.

---

## 4. Backend testing details
* `@WebMvcTest(RunController.class)` with a mocked `RunService`: 200/404/409/422 bodies match
  the ProblemDetail format; unknown fields rejected; oversized payloads rejected.
* Testcontainers: `GenericContainer("amazon/dynamodb-local")` on a random port; create the table
  from the same definition the SAM template uses (kept in `infra/table.json`, loaded by both).
* Conditional write test: two writers with the same `expectedVersion` → one succeeds, one gets 409.
* Matchmaking test: seeded table with ghosts at turns 3–5 and crowns 0–4 → picks within window,
  never self, falls back to bots, then mirror.
* Lambda handler test: build the zip and run `sam local invoke` with a recorded API Gateway event
  (CI job `lambda-smoke`, Docker). Proves the adapter wiring before any deploy.

---

## 5. Frontend testing details
* **Playback timeline** is pure TypeScript (`events → steps → view-model snapshots`): unit-test it
  thoroughly; the DOM only renders snapshots.
* **Component tests** use the real store with MSW mocking `/api`: "dragging a tavern hero onto an
  empty slot calls POST actions with type BUY and renders the new hero".
* **Keyboard tests**: every mouse flow has a keyboard twin test (`userEvent.keyboard`).
* **E2E fixtures**: the backend `local` profile exposes `POST /dev/seed` (local only) to create a
  deterministic run at a chosen turn, so E2E tests don't have to play 10 turns to test the result screen.
* **Visual snapshots** (Playwright `toHaveScreenshot`) for Title, Tavern, Battle, Result at
  1280×720, threshold 0.2 %, fonts self-hosted so renders are stable.

---

## 6. The simulator as a QA tool (`sim/`)
* `sim run --games 10000 --bot greedy --seed 1` → JSON + Markdown report (pick rates, win rates,
  battle length distribution, economy stats, clash-cap draws).
* `sim check --report build/report.json --thresholds sim/thresholds.yaml` → fails if any balance
  target in the GDD §9 is violated. Nightly workflow posts the report as a build artifact.
* `sim fuzz --actions 1000000` → random legal/illegal actions through `GameSession`; any exception
  other than `RuleViolation` is a bug; the failing seed + actions are printed as a ready-made scenario.
* `sim replay --seed 123 --actions actions.json` → reproduces a reported bug locally.

---

## 7. CI gates (branch protection on `main`)
Required checks: `backend-test`, `frontend-test`, `lint`, `coverage`, `openapi-contract`. Optional
but visible: `e2e`, `lighthouse`, `pit`. Merges are **squash** so one step = one commit on main.

---

## 8. Definition of Done (for every step in the plan)
1. The step's **Verify** commands pass locally and in CI.
2. New rules/logic have tests at the right layer (table above).
3. Docs updated: the step's `Sensei notes` are reflected in `docs/` or module README if they add
   a concept; `PLAN.md` status flipped.
4. No TODOs without an issue number; no commented-out code.
5. For UI steps: a screenshot or GIF in the PR; keyboard path checked.
6. For infra steps: `sam validate --lint` passes; cost checklist reviewed (no fixed-cost resources).
7. You wrote **one sentence in your own words** in the PR about what you learned (this is the
   "sign-off" — if you can't write it, we are not done teaching).

---

## 9. Bug workflow
1. Open an issue with the **seed, turn, action list (copy from `/dev` inspector), expected vs actual**.
2. Add a failing scenario or property test first (red).
3. Fix (green). Refactor if needed. The test stays forever.
4. Label `type:bug`, link PR, close via commit message `Fixes #n`.

---

## 10. Phase-end review checklist (the "Review Phase")
* Live demo of everything the phase promised (from `PLAN.md`), on a clean clone (`git clone` →
  run instructions → works).
* Test counts & coverage screenshots pasted in `docs/reviews/phase-N.md`.
* Retro: 3 things that went well, 3 to change, update `open_questions.md` and risk list.
* Tag `phase-N-complete`. Sign-off = your name and date in the review file.
