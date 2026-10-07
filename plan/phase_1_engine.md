# Phase 1 — Game Engine in pure Java (Steps 07–24)

**Where you are:** the repo exists, CI runs, AWS is safe. **At the end of this phase:** a complete,
tested rules engine — you can run `./mvnw -pl sim exec:java -Dexec.args="run --games 1000"` and
watch bots play thousands of Forgotten Heroes runs in seconds. No Spring, no AWS, no browser yet:
**the game exists before the app does.** This is the phase that teaches Java properly.

Package root: `com.forgottenheroes.engine`. Rulebook: `knowledge/game_design_document.md` (GDD).
Architecture: `knowledge/architecture.md` §3. Index 0 of a band is the **front** (fights first);
a band is an ordered list with no gaps (empty slots are only a UI concept).

---

## Step 07 — Maven parent, wrapper, `engine` module, first test
**Branch:** `step-07-maven-engine` · **est 2h**

**Goal:** a building Java project with one passing test and a coverage report.

**You will have:** root `pom.xml` (packaging `pom`, modules `engine`), `mvnw`/`mvnw.cmd`,
`engine/pom.xml`, `engine/src/main/java/com/forgottenheroes/engine/package-info.java`,
`engine/src/test/java/.../SmokeTest.java`, JaCoCo report.

**Sensei notes**
- *What:* Maven builds Java: `pom.xml` declares modules, dependencies and plugins; the **wrapper**
  (`mvnw`) downloads a pinned Maven so everyone uses the same one.
- *Why a parent pom:* one place for versions. We import Spring Boot's **BOM**
  (`spring-boot-dependencies`) purely to *manage versions* of JUnit/AssertJ/Jackson — the engine
  still has **no Spring code**; ArchUnit will enforce that in Step 23.
- *How:* **no global Maven.** The wrapper is two scripts plus a properties file that says which Maven to
  download; we unzip the official "script-only" wrapper and write that file by hand (a good way to see
  there is no magic in it).
- *Where:* repo root and `engine/`.

**Do this**
1. Root `pom.xml`: `groupId com.forgottenheroes`, `artifactId forgotten-heroes`, `packaging pom`,
   `<modules><module>engine</module></modules>`; properties `maven.compiler.release=25`,
   `project.build.sourceEncoding=UTF-8`, `spring-boot.version=4.1.x` (latest 4.1 patch);
   `dependencyManagement` importing `org.springframework.boot:spring-boot-dependencies:${spring-boot.version}:pom`;
   `pluginManagement` with `maven-compiler-plugin` (3.14+), `maven-surefire-plugin` (3.5+),
   `jacoco-maven-plugin` (0.8.14+, Java 25 support), `maven-enforcer-plugin` (require Java 25, Maven ≥ 3.9).
2. Maven Wrapper without Maven (run at the repo root):
   ```bash
   curl -sLO https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper-distribution/3.3.4/maven-wrapper-distribution-3.3.4-only-script.zip
   unzip -o maven-wrapper-distribution-3.3.4-only-script.zip mvnw mvnw.cmd && rm maven-wrapper-distribution-3.3.4-only-script.zip
   chmod +x mvnw && mkdir -p .mvn/wrapper
   printf 'wrapperVersion=3.3.4\ndistributionType=only-script\ndistributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip\n' > .mvn/wrapper/maven-wrapper.properties
   ./mvnw -v     # downloads Maven 3.9.16 into ~/.m2/wrapper once, then prints "Apache Maven 3.9.16"
   ```
3. `engine/pom.xml`: parent = root; test deps `org.junit.jupiter:junit-jupiter`,
   `org.assertj:assertj-core` (versions from the BOM); JaCoCo `prepare-agent`, `report`, and a
   `check` rule `LINE COVEREDRATIO ≥ 0.60` (raised to 0.90 in Step 23).
4. `SmokeTest`: `assertThat(1 + 1).isEqualTo(2)`. A `package-info.java` with a Javadoc describing the engine's purpose.
5. Add `backend-test` placeholder job to `ci.yml`? Not yet — add job `java` that runs `./mvnw -B -ntp verify` with `actions/setup-java@v4` (`distribution: corretto`, `java-version: 25`, `cache: maven`).

**Verify**
```bash
./mvnw -B -ntp verify           # BUILD SUCCESS, 1 test
start engine/target/site/jacoco/index.html   # opens the coverage report in your browser
```

**Review checklist:** [ ] `java -version` in CI log shows 25 · [ ] wrapper files committed (`.mvn/wrapper/maven-wrapper.properties`) · [ ] no Spring *dependency* (only the BOM import) in `engine/pom.xml`.

**Commit:** `step-07: maven parent, wrapper and engine module`

**Check your understanding:** What is the difference between `dependencyManagement` and `dependencies`? Why can the engine import Spring's BOM without depending on Spring?

---

## Step 08 — Core value types
**Branch:** `step-08-value-types` · **est 2h**

**Goal:** the smallest building blocks, each with tests, using modern Java features.

**You will have (package `model`):**
- `record Stats(int attack, int health)` — `plus(Stats)`, `withAttack`, `withHealth`, `clamp()` to 0..50 (`MAX_STAT = 50`), validation in the compact constructor (no negatives).
- `enum Rank { I, II, III, IV, V, VI }` — `turnUnlocked()` (1,3,5,7,9,11), `static Rank availableAt(int turn)`, `next()` (VI stays VI).
- `record Xp(int points)` — `level()` (0–1 → 1, 2–4 → 2, 5 → 3), `isMax()`, `plus(int)` capped at 5, `static final int LEVEL2_AT = 2, LEVEL3_AT = 5`.
- `enum Side { HOME, AWAY }` — `opposite()`.
- `record UnitId(String value)` with factory `UnitId.random(GameRandom)` (Step 09) — for now a counter-based factory.
- `enum Perk { FAIRY_CHARM, IRON_BLADE, CHAINMAIL, FLAME_OIL, WARD, PHOENIX_FEATHER, ENCHANTED_BLADE, AEGIS, VENOM }` with `consumable()`.
- `record Gold(int amount)` or plain `int`? Decide: **plain `int` with named constants** in `Rules`
  (`GOLD_PER_TURN = 10`, `HERO_PRICE = 3`, `RELIC_PRICE = 3`, `ROLL_PRICE = 1`, `BAND_SIZE = 5`,
  `START_HEARTS = 5`, `CROWNS_TO_WIN = 10`, `MERCY_TURN = 3`, `CLASH_CAP = 300`).

**Sensei notes**
- *What:* **records** are immutable data classes with free `equals/hashCode/toString`; **enums**
  are fixed sets with behaviour; a **compact constructor** validates.
- *Why:* rules numbers live in one `Rules` class (SAP has `ArenaConstants`/`BoardConstants`). Types
  like `Rank` make illegal states impossible (`rank = 7` cannot compile).
- *How:* `record Stats(int attack, int health) { Stats { if (attack < 0) throw …; } }`.
- *Where:* `engine/src/main/java/com/forgottenheroes/engine/model/`, `…/Rules.java`.

**Do this:** write each type + a focused test class (`StatsTest`, `RankTest`, `XpTest`). Use
`@ParameterizedTest` with `@CsvSource` for the rank/turn and xp/level tables straight from the GDD.

**Verify:** `./mvnw -pl engine -B -ntp verify` → ~20 tests green.

**Commit:** `step-08: engine value types and rules constants`

**Check your understanding:** Why is `Xp` a record and `Rank` an enum? What would go wrong if `Stats` were mutable?

---

## Step 09 — Deterministic RNG
**Branch:** `step-09-rng` · **est 2h**

**Goal:** randomness you can replay.

**You will have (package `rng`):** `GameRandom` (wraps `SplittableRandom`; `nextInt(bound)`,
`chance(percent)`, `<T> T pick(List<T>)`, `<T> List<T> shuffled(List<T>)`, `fork(String label)`),
`SeedDerivation` (`long turnSeed(long runSeed, int turn)`, `long battleSeed(long runSeed, int turn)`,
`long rollSeed(long turnSeed, int rollIndex)` using a splitmix64-style mixer), tests.

**Sensei notes**
- *What:* a seeded generator produces the same sequence every time. **Forking** derives independent
  child generators by label so adding a random call in one subsystem does not shift another.
- *Why:* replays, scenario tests, bug reproduction ("seed 42, turn 5") and the server/client
  contract all rest on this. SAP has `BoardRandom`.
- *How:* `new SplittableRandom(seed)`; never touch `Math.random()`/`new Random()`; no time-based seeds in the engine.
- *Where:* `engine/.../rng/`.

**Do this:** implement; tests: same seed → identical 1,000-number sequences; different labels →
different sequences; `pick` on a single-element list returns it; `shuffled` returns a permutation.
Add a `Hash64` helper (public, reused by matchmaking later).

**Verify:** tests green; a `main`-less sanity check: run the same test twice, compare outputs.

**Commit:** `step-09: seeded, forkable game random`

**Check your understanding:** Why derive the battle seed from `(runSeed, turn)` instead of generating a new random seed at battle time?

---

## Step 10 — Content model, JSON loader, schema validation
**Branch:** `step-10-content-model` · **est 3h**

**Goal:** heroes and relics described as data, loaded and validated at startup.

**You will have (package `content`):** records `HeroDefinition`, `TokenDefinition`,
`RelicDefinition`, `AbilityDefinition(trigger, text, effects, limit)`, sealed `EffectDefinition`
(`ModifyStats`, `DealDamage`, `SetHealthPercent`, `Summon`, `GainGold`, `GivePerk`, `StockRelic`,
`GiveXp`, `BuffTavern`), `TargetSpec(select, count)`, `Amount` (flat / perLevel / byLevel /
percentOfAttack), `Limit(perTurn, perBattle)`, `Trigger` enum, `TargetSelect` enum, `ContentLoader`,
`ContentIndex`; resources `content/heroes.json`, `tokens.json`, `relics.json`, `content.schema.json`
with **rank I heroes (6), their tokens, and rank I–II relics** for now.

**Sensei notes**
- *What:* a **DSL** = a small vocabulary (trigger/target/effect) that describes behaviour as data.
  Jackson maps JSON ⇄ records; **polymorphic** `type` fields pick the record subtype; a **JSON
  Schema** rejects typos before any test runs.
- *Why:* SAP's `Abilities.{Triggers, Conditions, Effects, Parameters}` — 300+ pets, one
  interpreter. Adding a hero becomes data + a scenario test.
- *How:* `@JsonTypeInfo(use = Id.NAME, property = "type") @JsonSubTypes({...})` on the sealed
  interface; `com.networknt:json-schema-validator` for the schema; `ContentLoader.load()` reads from
  the classpath; `ContentIndex` offers `hero(id)`, `heroesOfRankUpTo(rank)`, `relicsOfRankUpTo`, `token(id)`.
- *Where:* `engine/src/main/resources/content/`, `engine/.../content/`.

**Do this:** write the records (Jackson needs `jackson-databind`; version from the BOM); write the
schema (required fields, enums for trigger/select/type, ints ≥ 0); write `heroes.json` for Aoi (the Rank I cameo, GDD §5),
Farmhand, Merchant, Bard, Archer, Acolyte exactly as the GDD table; tests: loads without error;
ids unique; every `Summon.tokenId` exists; every rank I–VI will later have ≥ 6 heroes (assert ≥ 1 now);
an intentionally broken JSON in `src/test/resources` fails validation with a readable message.

**Verify:** `./mvnw -pl engine -B -ntp verify`; print `ContentIndex.summary()` in a test log (counts per rank).

**Commit:** `step-10: content DSL, loader, schema and rank I content`

**Check your understanding:** What does a sealed interface give us when we `switch` over effect types in the interpreter later?

---

## Step 11 — Board model
**Branch:** `step-11-board-model` · **est 3h**

**Goal:** the mutable state of a run, with invariants that are impossible to violate silently.

**You will have (package `model`):**
- `Unit` — `UnitId id`, `String heroId`, `Stats base`, `Stats permanentBonus`, `Stats temporaryBonus`,
  `Xp xp`, `Optional<Perk> perk`, `Counters counters` (ability uses this turn/battle),
  `int damageTaken` (battle only); derived `attack()`, `health()` (base + bonuses − damage, clamped),
  `level()`, `isAlive()`, `clearTemporary()`, `heal()`; factory `Unit.fromDefinition(def, id)`.
- `Band` — ordered `List<Unit>` max 5; `front()`, `back()`, `indexOf`, `insert(index, unit)`,
  `remove(unit)`, `move(from, to)`, `alive()`, `isEmpty()`, `copyForBattle()` (deep copy).
- `TavernSlot` (hero or relic reference, `price`, `frozen`), `Tavern` (`heroSlots`, `relicSlots`,
  `permanentBuff: Stats`, `bonusSlot: Optional<TavernSlot>`), `slotCountsFor(turn)` table.
- `RunState` — `runId`, `runSeed`, `turn`, `gold`, `hearts`, `crowns`, `band`, `tavern`,
  `freeRollAvailable`, `status` (`IN_PROGRESS, WON, LOST, ABANDONED`), `lastBattle` (summary), `version`.
- `RunState.assertInvariants()` used by tests and by the resolver in debug mode.

**Sensei notes**
- *What:* an **aggregate**: one root object (`RunState`) owning everything that must stay consistent.
- *Why mutable here:* mutators (Step 12+) change state in place and emit events — simplest for
  beginners and fast; immutability lives at the edges (records inside, deep copies for battle).
- *How:* keep fields private, expose intention-revealing methods, no public setters that bypass rules.
- *Where:* `engine/.../model/`.

**Do this:** implement with unit tests for `Band` ordering/moves and `Unit` stat math (base 2/2 +
perm 1/0 + temp 0/3 − damage 2 → 3/3; clamp at 50). Add a `TestFixtures` class (test scope) with
helpers like `unit("aoi", 2, 2)` and `band(...)` — you will use it everywhere.

**Verify:** tests green; `assertInvariants` throws on a 6-unit band in a test.

**Commit:** `step-11: unit, band, tavern and run state`

**Check your understanding:** Why does `Unit` keep *base*, *permanent* and *temporary* stats separately instead of one number?

---

## Step 12 — Events
**Branch:** `step-12-events` · **est 2h**

**Goal:** the vocabulary of facts the engine emits — the contract with the frontend.

**You will have (package `event`):** sealed `GameEvent` with the ~30 record types listed in
`architecture.md` §3.4 (each with `seq` assigned by `EventLog`), `EventLog` (append → assigns
sequence, `List<GameEvent> events()`, `since(seq)`), `EventJson` (Jackson mapper configured once:
polymorphic `type`, no nulls, stable field order), `engine/src/test/resources/golden/events-sample.json`.

**Sensei notes**
- *What:* an **event** is a past-tense fact (`HeroBought`), never a command. An **event log** is
  the ordered list. **Golden test** = compare serialized output with a committed file.
- *Why:* the frontend animates events; tests assert on events; replays store events. SAP's
  `BoardEvents` + `BoardEventRendererSystem`.
- *How:* records implementing the sealed interface; include before/after values
  (`StatsChanged{unitId, attackBefore, attackAfter, healthBefore, healthAfter, temporary}`).
- *Where:* `engine/.../event/`.

**Do this:** write the records; `EventLogTest` (sequence numbers increase from 1; `since`);
`EventJsonTest` golden file round trip (serialize → compare to file; deserialize → equals).
Document each event in Javadoc with one line — Step 24 copies these into the README.

**Verify:** tests green; golden file reviewed by eye once (it is the frontend's contract).

**Commit:** `step-12: game events and JSON contract`

**Check your understanding:** Why include "before" values in `StatsChanged` when the client could track them itself?

---

## Step 13 — Tavern actions I (start turn, roll, freeze)
**Branch:** `step-13-tavern-actions-1` · **est 3h**

**Goal:** the first real rules: a turn begins, the tavern fills, you can roll and freeze.

**You will have (packages `action`, `mutator`, `resolver`):** sealed `Action`
(`StartTurn, Roll, Freeze(slot), Unfreeze(slot), Buy, BuyRelic, Sell, Move, Merge, EndTurn` —
only the first four implemented now, others throw `UnsupportedOperationException` until Step 14/15),
`RuleViolation extends RuntimeException` with `code` (`NOT_ENOUGH_GOLD`, `SLOT_EMPTY`,
`BAND_FULL`, `INVALID_INDEX`, `WRONG_PHASE`, …), mutators `SetGold`, `SpendGold`, `GainGold`,
`FillTavern`, `SetFrozen`, `AdvanceTurn`, `ChangeHearts`; `TavernResolver.apply(state, action, rng) → EventLog`.

**Sensei notes**
- *What:* **Action** = intent; **Resolver** = validate then apply; **Mutator** = the only code that
  writes to state, and it appends an event for every change.
- *Why:* keeps rules readable (resolver reads like the GDD) and makes every change observable.
- *How:* `switch (action) { case StartTurn s -> startTurn(...); case Roll r -> roll(...); … }` —
  the compiler forces you to handle every action type (sealed + exhaustive switch).
  Start of turn: gold = 10; `freeRollAvailable = true`; rank from `Rank.availableAt(turn)`;
  slot counts from the GDD table; non-frozen slots refilled using `rng.fork("tavern")`; frozen
  slots kept; permanent tavern buff applied to new heroes; mercy heart at `MERCY_TURN`.
  Roll: if `freeRollAvailable` consume it, else require gold ≥ 1 and `SpendGold(1)`; refill non-frozen.
- *Where:* `engine/.../resolver/TavernResolver.java`, `…/mutator/*`, `…/action/*`.

**Do this:** implement + tests: slot counts for turns 1,2,3,4,5,8,9,12; rank availability on
turns 1–11; frozen item survives roll and next StartTurn; rolling with 0 gold and no free roll →
`RuleViolation(NOT_ENOUGH_GOLD)` and **state unchanged** (assert equality with a copy); mercy heart
only when hearts < 5 at turn 3.

**Verify:** tests green; log the event list of a `StartTurn` in one test and read it — it should tell the story.

**Commit:** `step-13: start turn, roll, freeze with resolver and mutators`

**Check your understanding:** Why must a rejected action leave the state exactly as it was? How does the sealed `Action` help you when you add `Sell` in the next step?

---

## Step 14 — Tavern actions II (buy, sell, move, merge, leveling)
**Branch:** `step-14-tavern-actions-2` · **est 4h**

**Goal:** the complete tavern economy.

**You will have:** `Buy(slot, bandIndex)` (place a new hero at index, or merge if the hero at that
index has the same `heroId`), `Sell(bandIndex)`, `Move(from, to)`, `Merge(from, to)` (band→band);
mutators `AddUnit`, `RemoveUnit`, `MoveUnit`, `StackUnits` (merge: keep max stats, +1 XP → +1/+1),
`GiveXp`, `OpenBonusSlot`; events `HeroBought`, `HeroSold`, `HeroesReordered`, `HeroesMerged`,
`XpGained`, `LevelUp`, `GoldChanged`; triggers are **recorded but not fired yet** (Step 16) via a
`PendingTriggers` list on the resolver result.

**Sensei notes**
- *What:* merging = SAP's `StackMinionMutator`; the **level-up bonus slot** = a temporary 6th
  tavern slot holding a hero of rank + 1.
- *Why:* leveling is the main long-term decision; getting XP math right is where many clones go wrong (test the 3-copies → level 2, 6 → level 3 rule explicitly).
- *How:* `Buy` validates `gold ≥ price`, `slot has hero`, `bandIndex ∈ [0, size]` (size = append),
  band not full unless merging; `Sell` grants `level` gold and removes; `Move` reorders without gaps.
- *Where:* `TavernResolver`, mutators.

**Do this:** implement + tests: buy into empty band; buy into full band fails; buy onto same hero
merges (stats = max + 1/1, xp 1); three copies → level 2 and a `LevelUp` event and a bonus slot of
rank + 1; six copies → level 3; sell gives 1/2/3 gold; move 4→0 puts the unit in front; gold never
below 0 (property-style loop over random legal actions).

**Verify:** tests green; coverage of `TavernResolver` ≥ 90 % in the JaCoCo report.

**Commit:** `step-14: buy, sell, move, merge and leveling`

**Check your understanding:** Explain in one sentence why buying a 4th copy of a level-2 hero adds +1/+1 but does not level it.

---

## Step 15 — Relics
**Branch:** `step-15-relics` · **est 3h**

**Goal:** buy and apply relics; perks exist on units.

**You will have:** `BuyRelic(slot, targetBandIndex?)`; `EffectApplier` (first version: applies
`ModifyStats`, `GivePerk`, `GiveXp`, `BuffTavern`, `Kill` (Poison Vial), `DealDamage` to explicit
targets), `Targeting` v1 (`SELF`, chosen unit, `RANDOM_ALLY(n)`, `ALL_ALLIES`, `TAVERN_HEROES`);
mutators `GivePerk`, `BuffTavern`, `ModifyStats` (temporary flag); events `RelicBought`,
`PerkGained`, `StatsChanged`; relics.json completed for ranks I–VI (16 relics).

**Sensei notes**
- *What:* instant relics mutate immediately; **perk** relics attach a `Perk` to the unit (one at a
  time, replacing); `Royal Decree` buffs the tavern's `permanentBuff` so future heroes spawn stronger.
- *Why:* perks are "equipment" — depth with no new units. Poison Vial is special: it makes a hero
  fall **in the tavern**, which must fire its Fall ability (Step 16 wires that; record the pending trigger now).
- *How:* `BuyRelic` validates target rules (targeted relics need a band index; random ones don't).
- *Where:* `EffectApplier`, `Targeting`, `relics.json`.

**Do this:** implement + tests per relic kind: Ration (+1/+1 permanent), Battle Brew (+3/+3
temporary → cleared by `clearTemporary()`), Iron Blade (perk set; second perk replaces), Poison
Vial (unit removed, pending FALL trigger recorded, cost 1), Royal Decree (tavern buff then a roll
produces buffed heroes), Tome (xp +1 → level-up when crossing threshold), Feast/Banquet random
targets use `rng.fork("relic")`.

**Verify:** tests green; `relics.json` validates; 16 relics listed by `ContentIndex.summary()`.

**Commit:** `step-15: relics, perks and effect applier`

**Check your understanding:** Why does Battle Brew need the "temporary" flag while Ration does not, if both are bought in the tavern?

---

## Step 16 — Trigger system and tavern triggers
**Branch:** `step-16-triggers` · **est 4h**

**Goal:** abilities fire when their moment comes, in a deterministic order, with limits.

**You will have (package `resolver`):** `TriggerDispatcher.fire(Trigger, TriggerContext, state, rng)`
(collects `(unit, ability)` candidates from the relevant side(s), filters by trigger & conditions &
limits, sorts by the GDD §4.6 rule, runs `AbilityInterpreter` for each, processes cascades
breadth-first with a queue), `AbilityInterpreter` (resolves `Amount` by level, `Targeting` v2 with
all selectors, applies via `EffectApplier`), `Counters` increments for limits, event
`AbilityTriggered{unitId, heroId, trigger}`; tavern triggers wired: `BUY`, `ALLY_SUMMONED` (buy
counts as summon), `SELL`, `START_OF_TURN`, `END_OF_TURN`, `LEVEL_UP`, `EAT_RELIC`, `SUMMONED`, and
the pending `FALL` from Poison Vial.

**Sensei notes**
- *What:* one dispatcher instead of one `if` per hero. SAP has one `Trigger…System` per trigger
  with `ActivateAbilityTrySystem/DoSystem`; ours is a single class with a table.
- *Why ordering matters:* two Fall abilities at once must resolve identically every time, on
  server and in tests.
- *How:* candidates → `sorted(comparing(attack).reversed().thenComparing(health).reversed()
  .thenComparing(side == HOME first).thenComparing(position))`; each `AbilityTriggered` event is
  followed by its effects' events; a cascade (an effect that kills → FALL) is enqueued, not recursed.
- *Where:* `TriggerDispatcher`, `AbilityInterpreter`, `Targeting`, `Counters`.

**Do this:** implement + tests using real rank I–II heroes: Merchant sell → +1 gold (×level);
Acolyte buy → random ally +1 health; Treasurer start of turn → +1 gold; Bard → bought ally +1
attack *temporary*; Herbalist stocks a Ration priced 2/1/0 by level; Standard Bearer poisoned in
the tavern buffs two allies behind; limit `perTurn: 1` enforced; ordering test with two copies of Aoi of
different attack (higher attack resolves first — assert event order).

**Verify:** tests green; a deliberately infinite cascade (test-only content) stops at a guard of 1,000 trigger activations with a `RuleViolation(ENGINE_GUARD)`.

**Commit:** `step-16: trigger dispatcher, ability interpreter and tavern triggers`

**Check your understanding:** Why process cascades with a queue instead of recursion? What would a stack overflow mean for a server?

---

## Step 17 — Battle resolver I (the clash loop)
**Branch:** `step-17-battle-1` · **est 4h**

**Goal:** two bands fight to a deterministic result with the core triggers.

**You will have (package `resolver.battle`):** `BattleResolver.resolve(homeBand, awayBand, seed,
content) → BattleResult{events, outcome}`; `BattleState` (two `Band` copies, clash counter);
`START_OF_BATTLE`, `HURT`, `KNOCKOUT`, `FALL`, `ALLY_FALL`, `ALLY_AHEAD_FALLS`; simultaneous
damage; falls removed and line shifted (`LineShifted` event); outcome `WIN/LOSS/DRAW`; `CLASH_CAP`
→ DRAW with `BattleEnded{reason: CLASH_CAP}`; the `damage()` function with Chainmail/Iron Blade
basics (full perk set in Step 18).

**Sensei notes**
- *What:* the loop from GDD §4.5. The two fronts hit **simultaneously** — compute both damages
  from the pre-hit stats, then apply both.
- *Why the cap:* a future hero combo could loop forever; a server must never hang.
- *How:* `while (bothAlive && clashes < CLASH_CAP)`; after every trigger batch call `settle()`
  which removes fallen units (firing FALL in order) until stable, then shifts.
- *Where:* `BattleResolver`, `BattleState`, `Damage`.

**Do this:** implement + **hand-calculated golden tests** (write the expected event sequence by
hand first, then run): 1v1 (2/2 vs 3/1 → away falls first clash, home survives at 2/1 → WIN);
2v2 with Archer opener; Aoi's fall buffs an ally; both last units trade → DRAW; cap test with two
unkillable test units → DRAW with CLASH_CAP.

**Verify:** tests green; a `BattleResolverTest.printsStory()` test logs a readable event story for one battle.

**Commit:** `step-17: battle resolver core loop`

**Check your understanding:** If both fronts have 1 health and 1 attack, why is it a draw and not a win for the home side?

---

## Step 18 — Battle resolver II (summons, remaining triggers, all perks)
**Branch:** `step-18-battle-2` · **est 4h**

**Goal:** every battle mechanic in the GDD.

**You will have:** `BEFORE_ATTACK`, `AFTER_ATTACK`, `ALLY_AHEAD_ATTACKS`, `ALLY_SUMMONED`,
`SUMMONED` in battle; `Summon` effect in battle (appears in the fallen unit's slot; `SummonFailed`
when the band is full; enemy-side summons for Plague Rat; `position: FRONT`); all 9 perks in
`Damage`/`settle()`: Iron Blade (+3), Chainmail (−2, min 1), Ward (−20 once, consumed),
Aegis (ignore once, consumed), Enchanted Blade (+20 once, consumed), Flame Oil (splash 5 to the
unit behind the target), Venom (target falls if damaged), Fairy Charm (Fall → Sprite), Phoenix
Feather (Fall → return as 1/1); `percentOfAttack` amounts; `SetHealthPercent` (Witch).

**Sensei notes**
- *What:* perks are small rules inside `Damage` and `settle()`; summons are mutators that must
  respect the 5-slot rule and fire `ALLY_SUMMONED` for the band-mates.
- *Why:* this is where "interactions" live; each gets its own scenario so a change elsewhere
  cannot silently break it.
- *How:* `Damage.compute(attacker, defender)` returns `{amount, consumedPerk}`; `settle()` handles
  Phoenix Feather before removal; Flame Oil splash goes through the same `Damage` path (so Chainmail reduces it).
- *Where:* `BattleResolver`, `Damage`, `EffectApplier.summon`.

**Do this:** implement + one test per perk + summon tests (Farmhand → Scarecrow in place; full band
→ `SummonFailed`; Plague Rat → Dirty Rat appears at the **enemy** front; Bard buff applies to
battle summons too; Druid summons two Wolves — second one fails if only one slot).

**Verify:** tests green; coverage of `resolver.battle` ≥ 90 %.

**Commit:** `step-18: battle summons, perks and remaining triggers`

**Check your understanding:** Why should Flame Oil's splash damage go through the same `Damage` function as a normal hit?

---

## Step 19 — Full MVP content + scenario tests
**Branch:** `step-19-content-complete` · **est 5h**

**Goal:** all 36 heroes, 7 tokens, 16 relics as data, each proven by a scenario.

**You will have:** complete `heroes.json`/`tokens.json`/`relics.json`; `engine/src/test/resources/scenarios/*.json`
(≥ 1 per hero/relic, format in `quality_and_testing_strategy.md` §2); `ScenarioTest` (a
`@TestFactory` producing one dynamic test per file); a meta-test asserting every content id has a
scenario; any DSL gaps found while writing content fixed (e.g. a new `TargetSelect`).

**Sensei notes**
- *What:* **table-driven tests**: the test is data; one runner.
- *Why:* 36 heroes × hand-written JUnit methods would be unreadable; scenarios double as documentation.
- *How:* `ScenarioRunner` builds bands from the file, runs tavern actions or a battle, then asserts
  the `expectEvents` subsequence (in order, extra events allowed) and `expectResult`.
- *Where:* `engine/src/test/resources/scenarios/`, `ScenarioTest.java`.

**Do this:** fill the content from the GDD tables (copy the stats exactly; ability text strings
are shown to players later — write them well); write scenarios; fix the DSL where content needs it.

**Verify:** `./mvnw -pl engine -B -ntp verify` → 36 + 16 + token scenarios green; `ContentIndex.summary()` prints 6 heroes per rank.

**Commit:** `step-19: full MVP content with scenario tests`

**Check your understanding:** Which hero needed a DSL feature you had not planned? What does that tell you about building the DSL before the content?

---

## Step 20 — `GameSession` facade and run lifecycle
**Branch:** `step-20-game-session` · **est 3h**

**Goal:** one class the backend and simulator can drive without knowing engine internals.

**You will have (package `session`):** `GameSession.start(runId, runSeed, content) → RunState`
(turn 1 started), `apply(state, action) → ActionResult{state, events}`, `endTurn(state, ghostBand)
→ TurnResult{battle, state, events}` (END_OF_TURN triggers, battle with `battleSeed`, hearts/crowns,
status WON/LOST, otherwise `StartTurn` for turn + 1, `clearTemporary()` on the band), `BandSnapshot`
(what a ghost is: hero ids, stats, levels, perks — no run internals), `RunState.toSnapshot()`.

**Sensei notes**
- *What:* a **facade** hides a subsystem behind a few methods.
- *Why:* the backend should read like "load state → session.apply → save"; the simulator the same.
- *How:* compose `TavernResolver`, `TriggerDispatcher`, `BattleResolver`; increment `version`.
- *Where:* `engine/.../session/`.

**Do this:** implement + a **full-run test**: scripted actions for 12 turns against a mirror ghost,
asserting the run ends (WON or LOST) and `events` are contiguous; a "win at 10 crowns" test using a
weak ghost; a "lose at 0 hearts" test with an unbeatable ghost; temporary stats cleared after battle.

**Verify:** tests green; `GameSession` is the only engine class the `sim` module will import (checked in Step 23).

**Commit:** `step-20: game session facade and run lifecycle`

**Check your understanding:** Why does `endTurn` take the ghost band as a parameter instead of fetching it itself?

---

## Step 21 — Bots
**Branch:** `step-21-bots` · **est 2h**

**Goal:** automated players for the simulator and for seeding opponents.

**You will have (package `bot`):** `interface Bot { Action next(RunState, ContentIndex, GameRandom) }`,
`RandomBot` (any legal action, 20 % chance to end turn once gold < 3), `GreedyBot` (buy the
highest-rank affordable hero; merge when possible; buy a relic if gold left; move highest-health
unit to front; roll if nothing good; end turn), `BotRunner.playRun(bot, seed, ghostProvider) → RunSummary`.

**Sensei notes**
- *What:* a bot is a function from state to action — the engine's first "client".
- *Why:* balance needs thousands of games; prod needs bot ghosts on day one.
- *How:* bots call `apply` and must handle `RuleViolation` by choosing another action (log it: a
  bot producing many violations reveals a confusing rule).
- *Where:* `engine/.../bot/`.

**Do this:** implement + tests: each bot finishes 50 seeded runs without exceptions; GreedyBot beats RandomBot ≥ 70 % over 200 mirror-free runs (ghost = the other bot's snapshot at the same turn).

**Verify:** tests green in < 10 s (bots are fast; if not, profile).

**Commit:** `step-21: random and greedy bots`

**Check your understanding:** Why is "GreedyBot beats RandomBot" a useful test even though it is about game feel, not correctness?

---

## Step 22 — `sim` module: balance simulator CLI
**Branch:** `step-22-simulator` · **est 4h**

**Goal:** run thousands of games and get a report that tells you whether the game is balanced.

**You will have:** module `sim/` (depends on `engine`; `info.picocli:picocli` for the CLI);
commands `run --games N --bot greedy|random --seed S --out build/report.json`, `check --report …
--thresholds sim/thresholds.yaml`, `fuzz --actions N --seed S`, `replay --seed S --actions file.json`;
`Report` (pick rate, win rate when on band, average stats per turn, battle length histogram,
clash-cap draws, economy per hero, run length) written as JSON **and** Markdown; `sim/thresholds.yaml`
from GDD §9; `./mvnw -pl sim exec:java` wiring (`exec-maven-plugin`) and a `scripts/sim.sh` shortcut.

**Sensei notes**
- *What:* a **simulator** plays the game with bots and measures. **Fuzzing** throws random
  actions at the engine to find crashes.
- *Why:* "feels balanced" is not evidence; a report is. SAP's team tunes with data too.
- *How:* reuse `BotRunner`; ghosts for a turn come from a pool filled by earlier simulated runs
  (so bots fight bots); aggregate with streams; render Markdown tables.
- *Where:* `sim/`.

**Do this:** implement; `check` exits 1 when a threshold fails; `fuzz` treats any exception other
than `RuleViolation` as a failure and prints a ready-to-save scenario (seed + actions); commit a
first report at `docs/balance/2026-10-report.md` (it will look unbalanced — that is the point of Step 70).

**Verify:**
```bash
./mvnw -B -ntp -pl sim -am package -DskipTests
./scripts/sim.sh run --games 2000 --bot greedy --seed 1        # < 60 s
./scripts/sim.sh fuzz --actions 200000 --seed 7                 # 0 engine exceptions
```

**Commit:** `step-22: balance simulator and fuzzer`

**Check your understanding:** Why do we fail the build on balance thresholds only in a nightly job and not on every PR?

---

## Step 23 — Property tests, architecture rules, coverage gate
**Branch:** `step-23-engine-quality` · **est 3h**

**Goal:** the engine proves its own invariants and structure.

**You will have:** jqwik properties (`EnginePropertiesTest`): determinism (same seed + actions →
identical event JSON), invariants after any legal action (gold 0..50, band ≤ 5, alive units
health > 0, slot counts match turn), rejected actions leave state unchanged, battles terminate
under the cap for random bands with random perks, `RunState` JSON round trip; ArchUnit rules
(`ArchitectureTest`): engine has no dependency on `org.springframework..`, `software.amazon..`,
`java.util.Random`/`Math.random`, `java.time.Clock`/`System.currentTimeMillis`; `sim` only
imports `engine.session`, `engine.bot`, `engine.content`, `engine.model` (read-only);
JaCoCo gate raised to **0.90** for `engine`; PIT configured (`[opt]`, profile `pit`).

**Sensei notes**
- *What:* **property-based testing** generates inputs; **ArchUnit** tests package rules;
  **mutation testing** mutates code to grade tests.
- *Why:* example tests prove "this case works"; properties prove "this never breaks".
- *How:* `@Property void goldNeverNegative(@ForAll("legalActionSequences") List<Action> actions)`
  with a custom `Arbitrary` that generates legal sequences by playing them; ArchUnit's
  `noClasses().that().resideInAPackage("..engine..").should().dependOnClassesThat().resideInAnyPackage(...)`.
- *Where:* `engine/src/test/java/.../properties/`, `.../architecture/`.

**Do this:** add deps (`net.jqwik:jqwik`, `com.tngtech.archunit:archunit-junit5`); write the
tests; fix anything they find (they will find something); raise the gate; add `mvn -Ppit` doc.

**Verify:** `./mvnw -B -ntp verify` green with coverage ≥ 90 % for engine; CI green.

**Commit:** `step-23: property tests, architecture rules and 90 % coverage gate`

**Check your understanding:** Name one invariant a property test can check that an example test realistically cannot.

---

## Step 24 — Engine README + Phase 1 review
**Branch:** `step-24-engine-docs` · **est 2h**

**Goal:** a reader can understand the engine without reading code; the phase is signed off.

**You will have:** `engine/README.md` (rules as implemented with links to GDD sections, package
map, event catalogue generated from the Javadoc one-liners, DSL reference with every trigger/
target/effect and an example, "how to add a hero" recipe, "how to reproduce a bug from a seed");
`docs/reviews/phase-1.md`.

**Do this:** write the README (a small script `tools/docs/event_catalog.py` can extract the
Javadoc lines so it never drifts); run the Phase 1 review (quality doc §10): fresh clone →
`./mvnw verify` → `sim run` works; record test counts and coverage; retro; tag `phase-1-complete`.

**Verify:** a friend (or Claude in a fresh session) can add a fake hero following the recipe in < 15 minutes.

**Commit:** `step-24: engine documentation and phase 1 review`

**Check your understanding:** In two sentences, explain to a recruiter what "deterministic, event-sourced engine" means and why it mattered here.
