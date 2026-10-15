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
