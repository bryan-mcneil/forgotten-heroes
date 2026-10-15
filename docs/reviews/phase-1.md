# Phase 1 review — Game Engine (Steps 07–24)

Reviewed on 2026-10-14. The checklist comes from `knowledge/quality_and_testing_strategy.md` §10. Phase 1 promised:
"a complete, tested, deterministic rules engine + simulator you can run from the terminal", built in pure Java with
no Spring and no AWS. Steps 07–09 were built by Bryan with Claude; Steps 10–24 were built by Claude at Bryan's
request (logged in the `PLAN.md` changelog on 2026-10-14) so the AWS phases arrive sooner.

## 1. What the phase promised, and the proof

| Promise | Proof on 2026-10-14 |
|---|---|
| Clean-clone demo | `git clone` of `main` into an empty folder → `./mvnw -B -ntp verify` → `BUILD SUCCESS` in 28 s (engine 342 tests, sim 9 tests, both coverage gates met) → `scripts/sim.sh run --games 300 --bot greedy --seed 2` → report written in 9 s |
| A complete rules engine | every rule of GDD §4 implemented (`engine/README.md`, "Rules as implemented"); 36 heroes, 7 tokens, 16 relics as data; every content id has a scenario file (59), enforced by a meta-test |
| Deterministic | `GameRandom` is the only dice, every seed derives from the run seed (`SeedDerivation`); the jqwik property "same seed + same actions → byte-identical event JSON" holds for 150 random cases per build; ArchUnit fails the build on `Math.random`, `java.util.Random`, `SecureRandom`, `java.time` and system time |
| Event-sourced | 34 `GameEvent` records with before/after values, numbered once per run; the golden file `engine/src/test/resources/golden/events-sample.json` is the contract with the frontend and is checked on every build |
| One facade | `GameSession.start / apply / endTurn`; the simulator's ArchUnit rule proves it uses nothing from `engine.resolver` or `engine.mutator` (except `RuleViolation`) |
| Simulator from the terminal | `scripts/sim.sh` with `run`, `check`, `fuzz` and `replay`; 2,000 greedy games in 2.5 s; 200,000 fuzz actions with 0 engine exceptions; the first report is `docs/balance/2026-10-report.md` |
| Tests ≥ 90 % | engine 97.8 % lines / 86.9 % branches behind a 90 % JaCoCo gate; sim 93.7 % / 88.9 % behind a 70 % gate |
| "Add a hero in under 15 minutes" | followed `engine/README.md` → "How to add a hero" on the fresh clone: one JSON row, one scenario file, one test run; the scenario passed first time and the only other guard that fired was the pinned hero count in `ContentLoaderTest` (a new hero is a roster decision, so that guard stays and the recipe says so); about 5 minutes including the build |

### Test counts and coverage (fresh clone, 2026-10-14)

| Module | Tests | Lines | Branches | Gate | Notes |
|---|---|---|---|---|---|
| engine | 342 | 97.8 % | 86.9 % | 90 % lines | 59 scenario files, 5 jqwik properties, 9 ArchUnit rules, 1 golden file |
| sim | 9 | 93.7 % | 88.9 % | 70 % lines | 1 ArchUnit rule; `fuzz` 20,000 actions inside the suite |

Mutation testing (optional profile `pit`): PIT ran once at the end of the phase in 5 min 44 s: 1,501 mutations, 1,230 killed (82 %), test strength 86 %, 77 mutations in uncovered lines; report in `engine/target/pit-reports/index.html`. The 18 % of surviving mutants are Step 70's reading list, not a gate.

The sentence the learning path says Bryan can now say, built by Claude this time and ready to be studied: *"I built
a deterministic, event-sourced rules engine with a JSON ability DSL, 90 %+ coverage, property tests for invariants
and a simulator that plays 10,000 games to tune balance."*

## 2. Retrospective

**Went well**

1. Design before content: the DSL written from the whole roster in Step 10 needed no new word for any of the 36
   heroes in Step 19; the surprises were all at the engine's edges (which side is a target on, when a Fall fires).
2. One trigger dispatcher for the tavern and the battle (Step 16): the battle resolver (Step 17) had no fall logic
   of its own, and every later mechanic (perks, summons, Witch, Necromancer) was a scenario, not a new code path.
3. Tests that write themselves: hand-calculated event sequences (Step 17), one scenario file per id (Step 19),
   properties (Step 23) and the fuzzer (Step 22) found real bugs on their first runs: a stat change on the enemy
   side with no position, a refused purchase that moved the unit-id counter.

**Change for Phase 2**

1. Bryan types the files again. Steps 10–24 were delegated to reach AWS sooner; from Step 25 the Phase 0 agreement
   (Claude explains, dictates and reviews; Bryan writes) is back, and the engine's teaching material is
   `engine/README.md`, the journal entries of Steps 10–24 and the glossary, to be read one step at a time.
2. Keep the "as built" paragraph per step in the phase file: it was the cheapest way to keep the plan honest when
   the build deviated (the plan's 1v1 numbers, the bonus slot, tavern damage).
3. Generated documentation over hand-kept lists: the event catalogue is produced from the Javadoc one-liners
   (`tools/docs/event_catalog.py --check`); the OpenAPI export in Step 26 should follow the same pattern.

**Agreed for Phase 2**

- Step 25 starts with Bryan reading `engine/README.md` "Package map", "Flow of one action" and "How to reproduce a
  bug from a seed", because the backend only ever touches `GameSession`, `RunStateJson` and `BandSnapshot`.
- The backend's JSON must use the engine's mappers (`EventJson.mapper()`, `ContentLoader.mapper()`,
  `RunStateJson.mapper()`) or a Spring `JsonMapper` customised the same way (no is-getter auto-detection, the
  `EngineJsonModule`), or `isEmpty`/`isPerk` leak into the API as fields.
- The `AWS is console-first` rule (Q31) applies unchanged from Step 34 on.

## 3. Open questions and risks

- No question was added during the phase; nothing is pending in `knowledge/open_questions.md`.
- Decisions the GDD left open are listed in `engine/README.md` → "Decisions made while building" and in the
  `PLAN.md` changelog rows of Steps 10–24 (XP is +1/+1 per point, tavern ability damage is permanent, a fallen
  unit still answers Hurt, the bonus slot closes at the next start of turn, the free roll after the automatic
  refill).
- The risk register (`knowledge/project_management.md` §9) gained two rows: the balance is untuned (32 of 74
  thresholds fail today; Step 70) and delegated steps are learning debt (read the journal before Phase 2).

## 4. Sign-off

Phase 1 complete on `main` at the Step 24 squash commit. Prepared by Claude on behalf of Bryan McNeil (Steps 10–24
delegated on 2026-10-14); Bryan countersigns by adding his name and date here when he has read the journal entries.
Tag: `phase-1-complete`.
