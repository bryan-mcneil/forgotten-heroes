# engine — the game rules (pure Java)

The rules of Forgotten Heroes as code: a deterministic, event-sourced rules engine with a JSON ability DSL.
No Spring, no AWS, no I/O, no clock. The backend drives it only through `GameSession`; the frontend never
recomputes a rule, it replays the events. This file describes the engine **as implemented** (Steps 07–24);
the rulebook it implements is [`knowledge/game_design_document.md`](../knowledge/game_design_document.md)
(GDD) and the design is [`knowledge/architecture.md`](../knowledge/architecture.md) §3.

## How do I run it

From the repo root (the wrapper downloads Maven 3.9.16 into `~/.m2/wrapper` the first time):

```bash
./mvnw -pl engine -B -ntp verify             # compile, every engine test, coverage report, the 90 % gate
./mvnw -pl engine -Dtest=BandTest test       # one test class
./mvnw -pl engine -Dtest=EventJsonTest test -Dgolden.update=true   # rewrite the event golden file on purpose
./mvnw -pl engine -Ppit verify               # optional mutation testing, minutes; engine/target/pit-reports/index.html
start engine/target/site/jacoco/index.html   # the coverage report (Git Bash on Windows)
scripts/sim.sh run --games 1000              # the balance simulator (module sim/)
python tools/docs/event_catalog.py --check   # the event catalogue below is generated; --write refreshes it
```

Read the `Tests run:` line, not only `BUILD SUCCESS`: a test saved outside `src/test/java/...` is silently
skipped. CI runs `./mvnw -B -ntp verify` in the `java` job on every pull request.

## What must NOT live here

- Spring, AWS SDKs, HTTP, databases, files or clocks (`ArchitectureTest` fails the build on them).
- `Math.random()`, `new Random()`, `SecureRandom`, `System.currentTimeMillis()`: `GameRandom` is the only dice
  and every seed derives from the run seed (ADR-4). Same seed + same actions = byte-identical events.
- Anything the browser needs directly. The frontend gets heroes and relics from `GET /api/content`.

## Package map

```text
com.forgottenheroes.engine
├── model      Stats, Rank, Xp, Side, Position, UnitId, Perk, Rules (the numbers), Unit, Band, Tavern, TavernSlot,
│              RunState (the aggregate root), RunStatus, Outcome, BattleSummary, Counters, ItemKind
├── content    the DSL as records: HeroDefinition, TokenDefinition, RelicDefinition, AbilityDefinition,
│              EffectDefinition (sealed, 10 kinds), TargetSpec, Amount, Limit, Condition, Trigger, TargetSelect;
│              ContentLoader (JSON + schema check), ContentIndex
├── rng        GameRandom (seeded, forkable), SeedDerivation (run → turn → roll / action / battle seeds), Hash64
├── action     sealed Action: StartTurn, Roll, Freeze, Unfreeze, Buy, BuyRelic, Sell, Move, Merge, EndTurn
├── event      sealed GameEvent (34 records), EventLog (numbering), EventJson (the mapper), UnitView
├── json       EngineJsonModule (rank ⇄ number, unit id ⇄ text, position ⇄ "home:0"), RunStateJson (storage DTOs)
├── mutator    the only writers of state: GoldMutator, TavernMutator, BandMutator, UnitMutator, RunMutator
├── resolver   TavernResolver (shop rules), TriggerDispatcher (abilities, ordering, limits, falls, guard),
│              AbilityInterpreter, EffectApplier, Targeting, EffectContext, Board, TavernBoard,
│              PendingTrigger, TavernResult, RuleViolation
│   └── battle BattleResolver (the clash loop), BattleState, Damage (perks), BattleResult
├── session    GameSession (the facade), ActionResult, TurnResult, BandSnapshot, UnitSnapshot
└── bot        Bot, RandomBot, GreedyBot, BotRunner, RunSummary, GhostProvider, GhostPool
```

Flow of one action: `Action → TavernResolver.apply` validates everything first (a `RuleViolation` leaves the
state exactly as it was), then calls mutators, each of which appends the event that describes its change; the
action ends with one `TriggerDispatcher.dispatch` that fires the abilities the action woke up, cascades included.
A battle is the same machinery on a `BattleState` with two bands.

## Rules as implemented

Section numbers refer to the GDD. Where the build had to decide something the GDD leaves open, the decision is
listed under *Decisions made while building* and in the `PLAN.md` changelog.

- **Run** (§4.1): turn 1, 5 hearts, 0 crowns, 10 gold. Win +1 crown, loss −1 heart, draw nothing. 10 crowns win the
  run (`RunEnded WON`), 0 hearts lose it. At the start of turn 3 a missing heart comes back (`HeartsChanged MERCY`).
- **Turn** (§4.2): `StartTurn` is engine-internal (`RunStarted` on turn 1, `TurnStarted`, gold to 10, the mercy
  heart, this turn's slot counts, bonus slot closed, per-turn ability counters reset, the automatic `TavernRolled`,
  then `START_OF_TURN` abilities). The player then acts freely; `EndTurn` goes through `GameSession.endTurn`.
- **Tavern** (§4.3): hero price 3, relic price per relic (Poison Vial 1), `Roll` free once per turn then 1 gold, sell
  for the hero's level, 3/4/5 hero slots on turns 1–4/5–8/9+, 1/2 relic slots on turns 1–2/3+, rank I at turn 1
  then II/III/IV/V/VI at turns 3/5/7/9/11, every slot drawn uniformly from the heroes (relics) of rank ≤ the
  turn's rank, frozen items survive rolls and the next start of turn, band of five, stats capped at 50.
- **Leveling** (§4.4): dropping a copy onto a hero (`Buy` onto it for 3 gold, `Merge` from the band for free)
  keeps the higher attack and the higher health, adds the copy's XP + 1, and **every XP point is +1/+1** for good.
  Level 2 at 2 XP, level 3 at 5; a level-3 hero refuses more copies (`MAX_LEVEL`). A level-up opens the **bonus
  slot** (one hero of the next rank, index `heroSlots().size()`) until it is bought, rolled away or the turn ends.
  Tome of Experience is +1 XP by the same rule.
- **Battle** (§4.5): both bands are copied; the player is home on odd turns and away on even ones
  (`BattleStarted.playerSide`). `START_OF_BATTLE`, then the loop: `Clash`, `BEFORE_ATTACK` ×2, both hits computed
  from the stats *before* either lands and applied together, `HURT` for everyone damaged, `Knockout` for an
  attacker whose hit reached zero, the settle pass (below), `LineShifted` per side that closed up,
  `ALLY_AHEAD_ATTACKS` and `AFTER_ATTACK` for attackers still standing. After 300 clashes it is a draw by `CLASH_CAP`.
- **Ordering** (§4.6): when several abilities answer one moment they run by attack ↓, health ↓, home before away,
  front before back. Units that fall during a moment still answer their own `FALL`, `SELL` and `HURT`; a cascade
  (a summon, a level-up, a fall caused by an ability) is queued, never recursed, and more than 1,000 activations in
  one action is `ENGINE_GUARD`.
- **Falls** (the settle pass, §4.5): units at zero health are processed in the ordering: `UnitFell`, the unit's own
  Fall ability while it still holds its place (its summons wait), Fairy Charm's Sprite, Phoenix Feather's
  `UnitRevived`, the friends' `ALLY_FALL` and `ALLY_AHEAD_FALLS`, then removal and the summons landing in the slot
  (`UnitSummoned`, then `ALLY_SUMMONED` and `SUMMONED`). A full band means `SummonFailed`; a summon for the enemy
  side lands at the enemy front; in the tavern there is no enemy band.
- **Temporary vs permanent** (§4.7): a battle runs on copies, so every battle buff dies with the battle; a tavern
  buff is permanent unless the effect says `untilEndOfBattle` (Battle Brew, Bard's gift), in which case it is
  cleared after the next battle. Temporary bonuses travel with a ghost (`BandSnapshot` keeps the fought-with stats).
- **Damage and perks** (§4.8): `Damage.attack(attacker, defender)` = attack + Iron Blade 3 + Enchanted Blade 20
  (once) → Aegis ignores it once → Ward takes 20 off once → Chainmail takes 2 off, never below 1; Venom makes any
  damaging attack lethal; Flame Oil also hits the enemy behind the target for 5 (`SPLASH`, through that unit's own
  perks). Ability damage goes through the defender's perks too (`Damage.incoming`). Once-only perks are reported
  (`PerkConsumed`) before the damage event. A hero holds one perk; a new one replaces it.

### Decisions made while building

- Every XP point is +1/+1, so a hero's XP bonus always equals its XP (Step 14).
- Ability damage **in the tavern** is a permanent health loss (bonus health first, then base), as in SAP's shop;
  Poison Vial simply drops a hero to zero (Step 16).
- A fallen unit still answers its own `HURT` (a lethal hit is still a hit, so Kaede strikes back) but not
  `KNOCKOUT` (Step 18).
- `SET_HEALTH_PERCENT` (Witch) is a `StatsChanged`, never damage: nobody is "hurt" by it (Step 16).
- After the automatic start-of-turn refill the player still has one free `Roll` (plan wording, Step 13).
- `RunStarted` carries no seed: a client that knew the seed could predict the shelves (Step 12).
- Buying a copy onto a hero fires `BUY` on the survivor but not `ALLY_SUMMONED` (merging is not a summon, Step 14).
- The bonus slot can be frozen; it closes at the next start of turn even so (Step 13).

## Event catalogue

Every event carries `seq` (its number in the run's log) and `type` (the record's name); unit ids are text
(`"u7"`, ghosts `"g1"`, battle summons `"s1"`), positions are `"home:0"`, ranks are numbers. The golden file
`src/test/resources/golden/events-sample.json` holds one of each and is the contract with the frontend.

<!-- events:start (generated by tools/docs/event_catalog.py; do not edit by hand) -->
| Event | Fields (besides `seq` and `type`) | Meaning |
|---|---|---|
| **run and turn** | | |
| `RunStarted` | `runId` | A run began: the first fact of every log. |
| `TurnStarted` | `turn`, `gold`, `rank`, `heroSlots`, `relicSlots` | A turn began: gold refilled, the tavern's rank and slot counts for this turn. |
| `TurnEnded` | `turn` | The player ended the tavern phase; the battle follows. |
| `GoldChanged` | `before`, `after`, `reason` | Gold went from before to after, and why. |
| **tavern** | | |
| `TavernRolled` | `free`, `heroes`, `relics` | The tavern shelves were (re)filled; frozen items stayed, the rest are new. |
| `TavernItemFrozen` | `kind`, `slot`, `itemId` | A shelf item was frozen: it survives rolls and the next start of turn. |
| `TavernItemUnfrozen` | `kind`, `slot`, `itemId` | A shelf item was unfrozen: the next roll replaces it. |
| `TavernBuffed` | `bonus`, `permanent` | Every hero on the shelves gained stats; when permanent, future heroes spawn with them too (Royal Decree). |
| `RelicStocked` | `relicId`, `slot`, `price` | An ability put a relic on a shelf at a price (Herbalist). |
| `BonusSlotOpened` | `slot`, `item` | A level-up opened the temporary bonus slot with a hero of the next rank (GDD §4.4). |
| `HeroBought` | `unitId`, `heroId`, `slot`, `bandIndex`, `stats`, `price` | A hero was bought from a shelf and placed on the band. |
| `RelicBought` | `relicId`, `slot`, `targetUnitId`, `price` | A relic was bought and used, on the chosen unit when it needs one. |
| `HeroSold` | `unitId`, `heroId`, `bandIndex`, `level`, `gold` | A hero left the band for gold equal to its level. |
| `HeroesReordered` | `from`, `to`, `order` | A unit moved from one index to another; `order` is the band's unit ids afterwards. |
| `HeroesMerged` | `unitId`, `heroId`, `source`, `removedUnitId`, `before`, `after` | Two copies became one: the survivor keeps the higher stats and gains +1/+1 with the XP. |
| `XpGained` | `unitId`, `heroId`, `before`, `after` | A unit's experience went from before to after. |
| `LevelUp` | `unitId`, `heroId`, `level` | A unit reached level 2 or 3; its Level-up ability fires and the tavern opens a bonus slot. |
| **units** | | |
| `StatsChanged` | `unitId`, `heroId`, `position`, `attackBefore`, `attackAfter`, `healthBefore`, `healthAfter`, `temporary`, `source` | A unit's attack and/or health changed; `temporary` buffs vanish after the battle. |
| `PerkGained` | `unitId`, `heroId`, `position`, `perk`, `replaced` | A unit gained a perk, replacing the one it held if any. |
| `PerkConsumed` | `unitId`, `heroId`, `position`, `perk` | A once-only perk did its job and is gone. |
| `AbilityTriggered` | `unitId`, `heroId`, `position`, `trigger`, `level` | A unit's ability fired; the effects' events follow. |
| **battle** | | |
| `BattleStarted` | `turn`, `playerSide`, `home`, `away` | A battle began between the two bands as they stood; `playerSide` says which one is the player's. |
| `Clash` | `number`, `homeUnitId`, `awayUnitId` | The two front units are about to hit each other. |
| `DamageDealt` | `sourceUnitId`, `sourcePosition`, `targetUnitId`, `targetPosition`, `amount`, `healthBefore`, `healthAfter`, `kind` | A unit took damage from an attack, a splash or an ability. |
| `Knockout` | `unitId`, `heroId`, `position`, `targetUnitId` | A unit's damage brought another to zero. |
| `UnitFell` | `unitId`, `heroId`, `position` | A unit reached zero health; its Fall ability fires, then it leaves the band. |
| `UnitRevived` | `unitId`, `heroId`, `position`, `stats` | A fallen unit came back in its slot with new stats (Phoenix Feather). |
| `UnitSummoned` | `unitId`, `heroId`, `position`, `stats`, `level`, `perk`, `summonerUnitId` | A new unit appeared on a band, bought or created by an ability. |
| `SummonFailed` | `tokenId`, `side`, `summonerUnitId` | A summon found no room: the band already had five units. |
| `LineShifted` | `side`, `order` | Fallen units were removed and the line closed up; `order` is the side's unit ids afterwards. |
| `BattleEnded` | `homeOutcome`, `playerOutcome`, `reason`, `clashes` | The battle is over: the outcome for the home side and for the player, and why it stopped. |
| **run progress** | | |
| `HeartsChanged` | `before`, `after`, `reason` | Hearts went from before to after: a lost battle, or the turn-3 mercy heart. |
| `CrownsChanged` | `before`, `after` | Crowns went from before to after: a won battle. |
| `RunEnded` | `status`, `turn`, `crowns`, `hearts` | The run is over: won at ten crowns, lost at zero hearts, or abandoned. |
<!-- events:end -->

## The ability DSL

Heroes, tokens and relics are data in `src/main/resources/content/` (`heroes.json`, `tokens.json`,
`relics.json`), checked against `content.schema.json` at load time (a typo fails with its path, e.g.
`/heroes/0/ability: property 'triger' is not defined`), then cross-checked (unique ids, every summoned token and
stocked relic exists). One interpreter runs every ability; adding a hero is a JSON row plus a scenario file.

```json
{
  "id": "aoi", "name": "Aoi", "title": "the Novice Scribe", "kanji": "葵",
  "rank": 1, "attack": 2, "health": 2, "sprite": "sv/set1/1",
  "ability": {
    "trigger": "FALL",
    "text": "Fall → one random ally gains +1/+1 (×level).",
    "effects": [
      { "type": "MODIFY_STATS", "target": { "select": "RANDOM_ALLY" },
        "attack": { "perLevel": 1 }, "health": { "perLevel": 1 }, "untilEndOfBattle": false }
    ],
    "limit": { "perBattle": { "flat": 3 } }
  }
}
```

**Triggers** and who answers them:

| Trigger | Answered by | Fired when |
|---|---|---|
| `START_OF_TURN`, `END_OF_TURN` | every unit on the band | the turn begins (after the refill) / the player ends it |
| `BUY`, `SELL`, `LEVEL_UP`, `EAT_RELIC` | the unit it happened to | bought (a merge counts), sold (before leaving), levelled, given a relic |
| `SUMMONED` | the unit that appeared | a token appeared (tavern or battle) |
| `ALLY_SUMMONED` | its living band mates | a unit was bought or summoned next to them (merging is not a summon) |
| `START_OF_BATTLE` | every unit on both sides | before the first clash |
| `BEFORE_ATTACK`, `AFTER_ATTACK` | the attacking front unit | around its hit (after: only if it survived) |
| `ALLY_AHEAD_ATTACKS` | the unit directly behind the attacker | the attacker hit (and survived) |
| `HURT` | the unit that took damage (> 0) | an attack, a splash or an ability hurt it, even fatally |
| `KNOCKOUT` | the living attacker whose damage reached zero | an attack, a splash or an ability killed |
| `FALL` | the fallen unit, still in place | its health reached zero (or Poison Vial) |
| `ALLY_FALL`, `ALLY_AHEAD_FALLS` | its living band mates / the unit directly behind it | a friend fell |

**Targets** (`"target": { "select": …, "count": amount }`, count defaults to 1; only living units; "ally" never
includes the firing unit; "enemy" is empty in the tavern): `SELF`, `CHOSEN` (the hero the player picked for a
relic), `TRIGGERING_UNIT` (the summoned ally, the fallen friend), `ALL_ALLIES`, `RANDOM_ALLY`, `ALL_ENEMIES`,
`RANDOM_ENEMY`, `ALL_UNITS` (both sides, self included), `ADJACENT` (the unit ahead and the unit behind),
`NEAREST_ALLIES_BEHIND`, `NEAREST_ALLIES_AHEAD`, `FIRST_ENEMY`, `LAST_ENEMY`, `LOWEST_HEALTH_ENEMY`,
`HIGHEST_HEALTH_ENEMY` (ties: front first), `FRONT_ALLY`, `LEVEL_2_PLUS_ALLIES` (random among them).
`TAVERN_HEROES` is not a unit selector: use `BUFF_TAVERN`.

**Effects** (`"type"` picks the record):

| Effect | Fields | Does |
|---|---|---|
| `MODIFY_STATS` | `target`, `attack`?, `health`?, `untilEndOfBattle` | adds stats; temporary in a battle always, in the tavern when the flag says so |
| `DEAL_DAMAGE` | `target`, `amount`, `condition`? | damage through the defender's perks; `HURT`/`KNOCKOUT` in battle; permanent loss in the tavern |
| `SET_HEALTH_PERCENT` | `target`, `percent` | removes that share of current health as a stat change, not damage |
| `SUMMON` | `tokenId`, `count`?, `attack`?, `health`?, `perk`?, `side` (`ALLY`/`ENEMY`), `position` (`HERE`/`FRONT`) | puts tokens on a band; `HERE` is the fallen unit's slot during a fall, else right behind the firing unit |
| `GAIN_GOLD` | `amount` | gold for the player (tavern) |
| `GIVE_PERK` | `target`, `perk` | attaches a perk, replacing the old one |
| `STOCK_RELIC` | `relicId`, `price` | puts a relic on the first empty relic shelf, or an extra one |
| `GIVE_XP` | `target`, `amount` | XP with +1/+1 per point; a level-up opens the bonus slot |
| `BUFF_TAVERN` | `attack`?, `health`?, `permanent` | buffs every hero on the shelves; when permanent, every future one too |
| `KILL` | `target` | the target falls at once (its Fall ability fires) |

**Amounts** (exactly one shape): `{ "flat": 4 }`, `{ "perLevel": 1 }` (1/2/3), `{ "byLevel": [2, 1, 0] }`,
`{ "percentOfAttack": 50 }` (50 % × level of the firing unit's attack, rounded down). Every number that may scale
is an amount, including `count`, summon stats, stocked prices and limits.

**Limits**: `"limit": { "perTurn": amount, "perBattle": amount }`; counted in `Counters` per unit, reset at the
start of a turn and when a battle copy is made. **Conditions**: `{ "ifTargetRank": 1, "multiplier": 2 }` on
`DEAL_DAMAGE` (Minotaur hits rank I twice as hard).

## How to add a hero

1. Add a row to `src/main/resources/content/heroes.json` (id in `lower_snake_case`, `rank` 1–6, level-1 `attack`
   and `health`, a `sprite` id for Step 40, the ability in the DSL above). A token it summons goes in
   `tokens.json`; a relic it stocks must exist in `relics.json`.
2. Write `src/test/resources/scenarios/<id>.json`: a battle (`home`, `away`, `expectEvents`, `expectResult`) or a
   tavern sequence (`turn`, `gold`, `band`, `tavern`, `actions`, `expectState`). Expected events name units by
   position (`"unit": "home:1"`), stat changes as `"attack": "1→2"`, summons by `"token"` and `"stats"`. Copy a
   neighbour's file: `aoi.json` (battle), `merchant.json` (tavern), `druid.json` (summons).
3. `./mvnw -pl engine -B -ntp verify`. The loader rejects a schema typo with its path, `ScenarioTest` runs your file by
   name, `everyContentIdHasAScenario` fails until the file exists, and the pinned roster count in `ContentLoaderTest`
   (`heroes 36 (I=6 …)`) fails on purpose: a new hero is a roster decision (GDD §5, Q7), so update that line knowingly.
4. `scripts/sim.sh run --games 1000` and look at the hero's row (pick rate, win rate on band) before Step 70 judges it.
5. If the hero needs a verb the DSL lacks, add the effect record to `EffectDefinition`, its branch to
   `EffectApplier.apply` (the compiler lists every `switch` that must learn it), its shape to `content.schema.json`,
   and a row to the table above.

## How to reproduce a bug from a seed

- A run is a function of its seed and its actions: `scripts/sim.sh replay --file bug.json` with
  `{"seed": 42, "actions": [{"type": "Buy", "slot": 0, "bandIndex": 0}, {"type": "EndTurn"}]}` prints every event
  and refusal; `sim fuzz` prints exactly this file when it finds an engine exception.
- A battle is a function of its two bands and `battleSeed(runSeed, turn)`: write a scenario file with the bands and
  the seed, and the expected events you believe in; the runner prints the full numbered story when they differ.
- A failing jqwik property prints its seed and the shrunk sample; put the seed in `@Property(seed = "…")` to replay.
- Every event in a run is numbered once, contiguously, so "events 310–318 of run X" is a precise bug report.
