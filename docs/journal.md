# Learning journal

One paragraph per session, in Bryan's words (grammar tidied, meaning untouched). Each step's
"What I learned" sign-off sentence lives here too, so it survives outside the pull request.

## 2026-10-07 — Step 01, Toolchain

Verified the toolchain and wrote `scripts/check-env.sh` and `docs/setup.md`. What I learned: be aware
of version differences between local and production environments, especially when using services like
AWS. Also be aware that tools can find a JDK through different paths: `java` on the PATH and
`JAVA_HOME` can point to different things and cause confusion when debugging. If local Java were 26 and
Lambda ran 25, everything would work locally and break on Lambda, and we would wonder why.

## 2026-10-07 — Step 02, Create the repository

Turned the folder into a Git repository on `main`, with the `Super Auto Pets/` reference copy ignored
before the first `git add`, and pushed it to GitHub as `bryan-mcneil/forgotten-heroes`. What I learned:
always add ignore rules before the first commit; a file that is committed and untracked later is in the
history forever. `.gitattributes` can force LF for every file, which keeps the files consistent across
machines, but some Windows-only files such as `.cmd` and `.ps1` need CRLF, so know which files those are.

## 2026-10-07 — Step 03, Monorepo skeleton

Created the monorepo skeleton on the first step branch and pull request: twelve folders, each with a
README that says what lives there, what must not, and how to run it; the brief moved to
`docs/project_goal.md`. What I learned: some of the folder structure we will be using — rules data lives
in the engine, and the tools pipeline handles the assets, which live in a private S3 bucket. A new hero's
JSON goes in `engine/src/main/resources/content/`; its sprite frames are never committed, the pipeline
builds them into the gitignored `frontend/public/assets/`.

## 2026-10-08 — Step 04, Project management on GitHub

Put the whole plan on GitHub: labels, one milestone per phase, issue and PR templates, the "Forgotten
Heroes" board, and 77 issues made by a script from `PLAN.md`. Did the first label, milestone and issue by
hand with `gh`, then ran the scripts. A short dash typed by hand instead of the long dash in the plan made one
duplicate issue and one duplicate milestone; the script matches titles letter for letter. What I learned: a
little about GitHub issues and tracking their status on the project board, and some `gh` terminal commands.

## 2026-10-14 — Step 05, CI skeleton & branch protection

Added the first GitHub Actions workflow: a `docs` job that lints every Markdown file and checks the links
in `README.md` and `PLAN.md` on every PR and every push to `main`. Locked `main` with branch protection
(the `docs` check is required, admins included, squash merges only, no force pushes) and proved it by
pushing an empty commit that was rejected. What I learned: the three parts of GitHub Actions in detail. A
workflow says when and what to run, in YAML. A job is one unit of work on its own virtual machine, the
runner. Steps run one after the other on that machine, choosing which commands to run and in which order.

## 2026-10-14 — Step 06, AWS account hardening and budgets

Locked the AWS account: MFA on the root user, an everyday IAM user `bryan` with MFA and no access keys, and the
terminal signed in through the browser with `aws login`. The plan's IAM Identity Center had to wait: enabling it
creates an AWS Organization, which ends the Free plan and its credits (Q32). Built the $5 and $20 budgets by hand
in the console, then as `infra/budgets.yaml`, and deleted the hand-made ones. Along the way Cost Explorer showed
two forgotten tutorial servers in Oregon eating about $23 of credits a month; terminated the same day. What I
learned: how to create a custom budget with e-mail alerts.

## 2026-10-14 — Phase 0 review

Phase 0 closed: machine verified, public repo with six squash commits, 77 cards on the board, CI required on
`main`, AWS account hardened and budgeted. The review is `docs/reviews/phase-0.md`. The three changes for Phase 1
came from Bryan: he writes the files himself from now on, messages stay short and say how things connect, and
every step keeps its teaching moment.

## 2026-10-14 — Step 07, Maven parent, wrapper and engine module

Built the Java skeleton: the Maven Wrapper (no global Maven; `./mvnw` downloads 3.9.16 once into `~/.m2`), a
root `pom.xml` that pins Java 25 and every plugin version and imports Spring Boot's BOM for library versions
only, an `engine` module with JUnit and AssertJ behind a 60 % JaCoCo gate, and a smoke test. Saved the two Java
files one folder too shallow at first: the build stayed green with zero tests, which is why the `Tests run:`
line matters more than `BUILD SUCCESS`. Broke the test on purpose and read `expected: 3 / but was: 2`. CI
gained a `java` job on Corretto 25, now a required check beside `docs`. What I learned: how Maven uses the
pom to manage dependencies, and how `./mvnw verify` runs the tests.

## 2026-10-14 — Step 08, core value types

First real Java in the engine: package `model` with `Rules` (every rulebook number in one holder), the records
`Stats`, `Xp`, `Position` and `UnitId`, and the enums `Rank`, `Side` and `Perk`, all immutable and validated
in compact constructors. Seven test classes, 41 tests; the GDD's turn and XP tables became parameterized
tests. Gotcha: VS Code auto-imported a wrong package because it had not yet seen the poms; delete the import
and reload the window once. What I learned: how to set up model types with their tests, and about Java types
like enums and records.

## 2026-10-14 — Step 09, deterministic RNG

Randomness you can replay: package `rng` with `Hash64` (the splitmix64 mixer, proven equal to the JDK's own by a
known-answer test), `SeedDerivation` (turn, battle and roll seeds all computed from one stored run seed) and
`GameRandom`, the engine's only dice, whose `fork(label)` comes from the stored seed plus a name so an extra draw
in the shop never reshuffles the battle. Replayed "seed 42, turn 5" twice in jshell and got the same three numbers;
broke `fork` on purpose and watched exactly one test fail. Kept the `UnitId` counter after all, because random ids
can collide and a counter cannot. What I learned: pseudorandom RNG with SplittableRandom, and the interesting idea
of using pseudorandom numbers to track history and keep the server in sync with the game.

## 2026-10-14 — Steps 10–24, built by Claude

From this entry to the Phase 1 review, Claude wrote the code and the docs at Bryan's request (logged in the PLAN
changelog): the engine is the road to the AWS phases, which are the goal, so the "type every file yourself" rule
from the Phase 0 retro is paused for Steps 10–24. Each step still gets its own branch, pull request, CI run and
squash commit, and each entry below ends with the one lesson worth keeping from that step, written by Claude for
Bryan to read rather than by Bryan.

## 2026-10-14 — Step 10, content model, loader and schema

Heroes, tokens and relics became data: package `content` holds the DSL as records (`HeroDefinition`,
`AbilityDefinition`, a sealed `EffectDefinition` with ten `type`-tagged kinds, `TargetSpec`, `Amount`, `Limit`,
`Condition`) and `ContentLoader` reads the three JSON files with Jackson 3, checks them against
`content.schema.json` first, and hands back an immutable `ContentIndex` that also cross-checks ids and references.
A deliberately broken file under `src/test/resources/content-broken` fails with `/heroes/0/ability: property
'triger' is not defined`. Rank I is complete (Aoi, Farmhand, Merchant, Bard, Archer, Hotaru), plus Scarecrow and
Sprite and the five rank I–II relics; 25 new tests, 84 in the module. Lesson: a sealed interface plus an exhaustive
`switch` turns "did I forget an effect type?" into a compile error, and a JSON Schema turns a typo in data into a
build error with a path, both long before a rule runs.

## 2026-10-14 — Step 11, board model

The mutable heart of a run: `Unit` (base + permanent bonus + temporary bonus − damage, all derived on read, capped
at 50), `Band` (ordered, no gaps, five at most, `move(4, 0)` puts a unit in front), `TavernSlot` and `Tavern` (slot
counts from the GDD table, the level-up bonus slot after the regular ones, the Royal Decree buff), `RunState` (the
aggregate root with gold, hearts, crowns, turn, status, version, the event sequence and the unit-id counter) and
`assertInvariants()`, which throws on a sixth unit, a fallen unit outside a battle, a frozen empty slot or slot
counts that do not match the turn. Every class has a deep `copy()` and a hand-written `equals`, so a later test can
prove a rejected action changed nothing. 45 new tests, 129 in the module, plus `TestFixtures` for one-line units
and bands. Lesson: keep base, permanent and temporary stats apart and derive the number the player sees, because
"clear the battle buffs" is then one line that cannot accidentally erase a Ration.

## 2026-10-14 — Step 12, events

The vocabulary of facts: a sealed `GameEvent` with 33 nested records (one Javadoc line each, for the README), every
one carrying `seq`, before/after values and ids the client can draw with (`"u7"`, `"home:0"`). `EventLog.append`
stamps the next number and can continue from where the previous log ended, so a whole run is one contiguous
sequence; `EventJson` is the mapper configured once (`type` = the record's simple name, no nulls, is-getters off,
`\n` line ends), and `src/test/resources/golden/events-sample.json` holds one of every event, pretty-printed, as
the contract with the frontend. Three things bit on the first run: a nested record's default type id is
`GameEvent$PerkGained`, a `List<GameEvent>` written without a typed writer loses every `type` field, and Jackson
writes `isEmpty()` as `"empty": true` unless told not to. 11 new tests, 140 in the module. Lesson: a golden file
turns "did the JSON change?" from a code review guess into a build failure with a diff, which is exactly what a
contract between two code bases needs.

## 2026-10-14 — Step 13, tavern actions I

The first real rules. Package `action` holds the sealed `Action` family (ten records, JSON `type`), `resolver` holds
`RuleViolation` (a `Code` enum plus `require`) and `TavernResolver`, whose `apply(state, action)` validates everything
first and only then calls the mutators in package `mutator` (`GoldMutator`, `TavernMutator`, `RunMutator`, each
appending its event). `StartTurn` reads like GDD §4.2: next turn, 10 gold, mercy heart on turn 3, slot counts from
the table, bonus slot closed, automatic refill with dice derived from `rollSeed(turnSeed, 0)`; `Roll` is free once
and then 1 gold; `Freeze`/`Unfreeze` keep or release a shelf item, bonus slot included. A rejected roll with no gold
leaves the state equal to a copy taken before it. 36 new tests, 176 in the module; the first turn's story reads
`RunStarted → TurnStarted → GoldChanged → TavernRolled`. Lesson: validate everything before mutating anything, and
derive the dice from the saved state, and "rejected action, state unchanged" and "same seed, same shelves" both
come for free.

## 2026-10-14 — Step 14, tavern actions II

The whole tavern economy: `Buy` (3 gold; the band index places the hero, the band's size appends, a copy at the
index merges), `Sell` (gold = level, `SELL` trigger recorded before the unit leaves), `Move` (no gaps, 4 → 0 puts a
hero in front) and `Merge` (free, band to band). The merge rule is now explicit: keep the higher of each stat, XP =
survivor + copy + 1, and every XP point is +1/+1 for good, so three copies make level 2, six copies or two level-2s
make level 3, and a level-3 hero refuses a seventh. A level-up opens the bonus slot with a hero of the next rank,
announced by a new `BonusSlotOpened` event. New mutators `BandMutator` and `UnitMutator`; the resolver's dice for
anything but a roll come from `actionSeed(turnSeed, lastEventSeq)`. 14 new tests (incl. a 400-action random loop
that checks gold and invariants after every accepted or rejected action), 190 in the module; `TavernResolver` at
100 % lines. Lesson: write the general rule ("every XP point is +1/+1") and let the single-copy case fall out of it,
instead of coding the example from the plan and discovering two level-2s later.

## 2026-10-14 — Step 15, relics

All 16 relics as data, and the first interpreter: `EffectApplier.apply(effect, context)` switches over the sealed
effect types, resolves amounts, asks `Targeting` for the units (`SELF`, `CHOSEN`, `TRIGGERING_UNIT`, `ALL_ALLIES`,
`RANDOM_ALLY` with a seeded shuffle) and calls the mutators; `EffectContext` carries the moment (state, band, chosen
hero, level, dice, log, pending triggers). `BuyRelic(slot, bandIndex?)` validates the shape of the request (a
"one hero" relic needs an index, a self-targeting one refuses it), pays, empties the shelf and runs the effects.
Ration is permanent, Battle Brew temporary (cleared with the battle buffs), a second perk replaces the first,
Poison Vial costs 1 and makes a hero fall in the tavern, Royal Decree buffs the shelves now and every future hero,
the Tome levels a hero at the threshold and opens the bonus slot. 11 new tests, 201 in the module, including a loop
that buys every relic on a three-hero band. Lesson: build the interpreter before the content, and the content
becomes rows plus one test loop; the one deviation (`untilEndOfBattle` means temporary only in the tavern, because
a battle runs on copies anyway) is the kind of thing a DSL makes you write down.

## 2026-10-14 — Step 16, trigger system and tavern triggers

Abilities fire. `TriggerDispatcher.dispatch(board, dice, log, moments)` drains a queue of moments: for each it
collects the candidates the trigger allows (the unit itself for `BUY`/`SELL`/`FALL`…, its living band mates for
`ALLY_*`, everybody for the start of turn or battle), drops the ones whose per-turn or per-battle limit is spent,
sorts them by the GDD §4.6 rule (attack, health, home, front), announces `AbilityTriggered` and runs
`AbilityInterpreter → EffectApplier`. Moments caused on the way are queued, never recursed, and a guard of 1,000
activations turns a looping content file into `ENGINE_GUARD`. After every moment a settle pass processes units
at zero health: their Fall ability fires while they still hold their place, Fairy Charm and Phoenix Feather do
their thing, friends' Ally-fall abilities fire, then the unit leaves and its summons take the slot. The resolver
dispatches at the end of every action, so Merchant pays on sell, Hotaru heals on buy, Bard buffs the bought ally,
Treasurer and Herbalist fire at the start of the turn, a poisoned Standard Bearer buffs the two allies behind, a
poisoned Farmhand leaves a Scarecrow that Bard then buffs, and a poisoned Alchemist takes a weak ally with it whose
own Fall still fires. Rank II heroes joined `heroes.json` early for these tests. One rule had to be decided:
ability damage in the tavern is permanent, as in SAP's shop. 19 new tests, 220 in the module. Lesson: a cascade
is a queue, not a recursion, and whoever removes a fallen unit must be the same code that fires its Fall, or
position-relative abilities silently break.

## 2026-10-14 — Step 17, battle resolver I

Two bands fight. `BattleResolver` copies both bands into a `BattleState`, announces the line-ups, fires
`START_OF_BATTLE`, and loops: `Clash`, `BEFORE_ATTACK`, both hits computed from the pre-hit stats and applied
together, `HURT` and `KNOCKOUT` moments, the dispatcher's settle pass (falls, Fall abilities, summons), `LineShifted`
for each side that closed up, then `AFTER_ATTACK` and `ALLY_AHEAD_ATTACKS` for attackers still standing; after 300
clashes it is a draw by `CLASH_CAP`. `Damage` knows Iron Blade (+3) and Chainmail (−2, never below 1) and already
reports which once-only perk a hit would spend, for Step 18. The tests were written by hand first (1v1, an opener
that ends the battle before any clash, Aoi's fall buffing Farmhand, a trade that draws, two unkillable units at the
cap, the away player's flipped outcome, Shield Maiden growing when hurt, determinism of the JSON) and matched the
engine on the first run. 13 new tests, 233 in the module; `printsStory()` logs a readable battle. Lesson: compute
both hits before applying either, and two 1/1 fronts are a draw, not a home win; and the dispatcher built for the
tavern runs the battle's falls unchanged, which is what "one trigger system" was for.

## 2026-10-14 — Step 18, battle resolver II

Every battle mechanic in the GDD. `Damage` now knows all nine perks (Iron Blade, Enchanted Blade, Aegis, Ward,
Chainmail, Venom in the numbers; Flame Oil's splash, Fairy Charm's Sprite and Phoenix Feather's return in the
resolver and the settle pass) and `Damage.spend` reports a once-only perk before the damage lands; attacks, splashes
and ability damage share that one path, so Archer's opener against an Aegis is 0 and spends it. Summons in battle
needed no new code: Farmhand's Scarecrow takes its slot, a full band fails the summon, Plague Rat's Dirty Rat appears
at the enemy front, Bard and Beastmaster buff battle summons, Druid's second Wolf fails when one slot is left.
Rank III heroes joined for those tests (Assassin, Clay Golem, Berserker with its percent-of-attack damage, Iwao,
Beastmaster, Druid). One rule changed: a fallen unit still answers its own Hurt, because a lethal hit is still a
hit. Three of my hand-written expectations were wrong on the first run and the engine was right, which is the
other thing a hand-calculated test is for. 16 new tests, 249 in the module. Lesson: route every kind of damage
through one function, then nine perks are nine lines instead of nine special cases in three places.

## 2026-10-14 — Step 19, full MVP content and scenario tests

All 36 heroes, 7 tokens and 16 relics are data now (`ContentIndex.summary()` reads `heroes 36 (I=6 II=6 III=6 IV=6
V=6 VI=6), tokens 7, relics 16`), and not one needed a new word in the DSL. The proof is table-driven: `ScenarioTest`
turns every file under `src/test/resources/scenarios` into a named test through `ScenarioRunner`, which builds the
board from the file, runs the battle or the tavern actions, and matches the expected events as an ordered subsequence
by position, id and `before→after` strings; a meta-test insists every content id owns a file. 59 scenarios: 36 heroes,
16 relics, 7 tokens. They caught one engine bug (Stone Golem's buff on an enemy reported no position, because the
applier used the caster's band for the target) and two slips in my own arithmetic. 60 new tests, 309 in the module.
Lesson: which hero needed a DSL feature I had not planned? None, because the DSL was written from the whole roster
first; the surprises came from the engine's edges (which side is a target on?), which is exactly where a scenario
per hero earns its keep.

## 2026-10-14 — Step 20, GameSession facade and run lifecycle

One door into the engine: `GameSession.start(runId, seed)` begins turn 1, `apply(state, action)` runs a tavern
action and bumps the version, `endTurn(state, ghost)` fires the end-of-turn abilities, snapshots the band as this
player's ghost, fights the given ghost (home on odd turns, away on even), applies the crown or the heart, clears the
temporary stats, ends the run at ten crowns or zero hearts and otherwise starts the next turn, all as one contiguous
event list. `BandSnapshot`/`UnitSnapshot` are what a ghost is: hero, the stats it fought with, XP, perk, nothing
else. Tests: ten empty ghosts win the run, an unbeatable ghost loses it on turn 6 with the mercy heart on the way
(4, then 4 again because the mercy heart lands in the same call as the turn-2 loss, then 3, 2, 1, 0), temporary stats vanish after the battle, the run's units are never touched
by the battle copies, and twelve scripted turns against a mirror give the same JSON twice and never a gap in the
numbering. 11 new tests, 320 in the module. Lesson: `endTurn` takes the ghost as a parameter because choosing an
opponent is the backend's job (a database query with fallbacks), while fighting it is the engine's; keeping that
line means the engine stays a pure function of state, action and seed.

## 2026-10-14 — Step 21, bots

The engine's first clients. `Bot` is one method, state in and action out; `RandomBot` plays anything and ends the
turn one time in five once gold is low, `GreedyBot` merges copies, buys the strongest affordable hero, uses a relic
on its best fighter, keeps the toughest hero in front and rolls when nothing is worth buying. `BotRunner` drives a
run through `GameSession` exactly as the backend will drive a human, counts refused actions instead of crashing on
them, and has three safety valves so a run always ends; `RunSummary` records every battle with the band that fought
it, and `GhostPool` files those bands per turn so later runs fight earlier ones, like production's ghost table.
Greedy wins about 97 % of its battles against random-bot ghosts and every one of 200 runs; a mirror ghost, as
expected, mostly draws. 7 new tests, 327 in the module, the whole bot suite in under two seconds. Lesson: "Greedy
beats Random" is not about correctness but it is still a test, because it is the cheapest alarm for a rule change
that quietly makes the game random, and because a bot that keeps proposing illegal moves is pointing at a
confusing rule.
