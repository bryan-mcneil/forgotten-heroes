# Glossary for Beginners

> Plain-language definitions of every term the plan uses. If a word in a step confuses you, it is
> here. Organised by area; skim once, then use as a lookup.

## Git & GitHub
* **Repository (repo)** — a folder whose entire history Git remembers. Ours is `forgotten-heroes`.
* **Commit** — a saved snapshot with a message. One step = one commit on `main`.
* **Branch** — a parallel line of commits. We make `step-NN-…` branches and merge them back.
* **Pull request (PR)** — "please merge my branch"; the place for review, CI results, screenshots.
* **Squash merge** — all branch commits become one commit on `main`. Keeps history = plan.
* **CI (Continuous Integration)** — a robot (GitHub Actions) that runs tests on every PR.
* **CD (Continuous Delivery/Deployment)** — the robot also deploys when `main` changes or a tag is pushed.
* **Tag** — a named commit (`v1.0.0`). Prod only deploys from tags.
* **OIDC (OpenID Connect)** — a way for GitHub's robot to prove its identity to AWS without stored passwords.
* **Branch protection** — rule: nobody (including you) merges to `main` without green CI. Ours also requires a straight-line history (squash merges only) and forbids force pushes; the settings live in `infra/github/branch-protection.json`.
* **GitHub Actions: workflow / job / step** — a *workflow* is one YAML file in `.github/workflows/` that says *when* to run (`on: pull_request`). It holds *jobs*; each job runs on its own fresh computer, a *runner* (`ubuntu-latest`). A job is a list of *steps*: a shell command (`run:`) or a reusable action someone published (`uses: actions/checkout@v7`). Ours: workflow `ci`, job `docs`, three steps.
* **Status check** — the green tick or red cross a job reports back to the PR. Branch protection can require a check by its job name (`docs`).
* **Linter** — a program that reads files and complains about rule violations without running anything: `markdownlint` for Markdown now, ESLint for TypeScript later. Rules are switched on and off in a config file (`.markdownlint-cli2.yaml`).
* **Dependabot** — GitHub's robot that opens a PR when something you depend on has a new version. Ours watches the actions used in the workflows, weekly; Maven and npm are added when those files exist.
* **YAML** — the indentation-based text format used for workflows, issue forms and (later) SAM templates: `key: value`, lists start with `-`. Two spaces per level, never tabs; a wrong indent changes the meaning without an error.
* **`git init`** — turns a plain folder into a repository by creating the hidden `.git/` folder; `-b main` names the first branch.
* **Remote / `origin`** — a copy of the repo somewhere else (GitHub). `origin` is the conventional name for the main one; `git push` sends commits to it.
* **`gh` (GitHub CLI)** — GitHub from the terminal: `gh repo create`, `gh pr create`, `gh issue list`. Our scripts use it so nothing has to be clicked.
* **`.gitignore`** — paths Git must pretend do not exist: build output, secrets, the `Super Auto Pets/` folder. A trailing `/` means "directory". `git check-ignore -v <path>` tells you which rule matched.
* **Line endings (LF vs CRLF)** — the invisible character(s) that end a line. Linux, macOS and Git Bash use LF (`\n`); Windows tools use CRLF (`\r\n`). A bash script saved with CRLF fails with `bad interpreter: ^M`; a `.cmd` file saved with LF can confuse `cmd.exe`.
* **`.gitattributes`** — per-path rules for Git: which files are text (and which line ending they get on checkout) and which are binary. Ours: everything LF, except `.cmd`/`.ps1` (CRLF); images and audio binary.
* **EditorConfig (`.editorconfig`)** — a tiny file every editor understands: indent size, charset, line endings, final newline. Keeps formatting out of code review.
* **Licence (MIT)** — a short permissive licence: anyone may use, copy and modify the code as long as the copyright notice stays. Ours covers the code only; art and audio are not in the repo.
* **Monorepo** — several projects (engine, backend, frontend, infra, tools…) in one repository with one history, so one PR can change the engine, the API and the docs together. Git tracks files, not folders: an empty folder does not exist to Git, so each folder in the skeleton holds a `README.md` that says what lives there, what must not, and how to run it.
* **Issue** — one unit of work on GitHub: a number (`#12`), a title, a body, labels, a milestone, open or closed. Ours: one issue per step, titled `Step NN — Title`; the PR says `Closes #12` and merging it closes the issue.
* **Label** — a coloured tag on an issue or PR, for filtering: `phase:0`, `area:engine`, `type:step`. The colour is a 6-digit hex code (`0E8A16`).
* **Milestone** — a named bucket of issues with a progress bar. One per phase: `Phase 0 — Foundations`.
* **Project (board)** — GitHub's kanban: cards (issues) in columns. The "column" is really a field called *Status* whose options we rename to Backlog → Next → In progress → In review → Done; a built-in rule moves a closed issue to Done.
* **Issue form / PR template** — a YAML file in `.github/ISSUE_TEMPLATE/` turns *New issue* into a small form with required fields (`step.yml`, `bug.yml`, `idea.yml`); `pull_request_template.md` pre-fills every PR body.
* **`gh api`** — a raw call to GitHub's HTTP API from the terminal: `gh api repos/:owner/:repo/milestones` (`:owner/:repo` is filled from the current repo, `-f key=value` sends a field, `--jq` filters the JSON reply). Every other `gh` command is one of these calls with a friendlier name.
* **Scope (OAuth)** — one permission on a login token: `repo`, `read:org`, `project`. `gh auth refresh -s project` asks GitHub to add one; a missing scope fails with "missing required scopes".
* **Dry run** — a `--dry-run` flag that prints what a script *would* do and changes nothing. Cheap insurance before 77 API calls; see also *Idempotent* under Operations.

## Java & build
* **JDK** — the Java toolkit (compiler + runtime). **LTS** = long-term-support version (21, 25).
* **Maven** — Java's build tool: it downloads the libraries the code needs, compiles, runs the tests and packages the result from one command. It reads its instructions from `pom.xml`.
* **Maven Wrapper (`mvnw`)** — two committed scripts (`mvnw`, `mvnw.cmd`) that download the exact Maven version named in `.mvn/wrapper/maven-wrapper.properties` into `~/.m2/wrapper` on first use. Nobody installs Maven; every build command is `./mvnw …`. Ours is the *script-only* flavour (no jar in the repo).
* **`pom.xml` / parent pom** — Maven's description of a project. The root pom is the *parent*: it lists the modules and pins every version once; each module's pom names it in `<parent>` and inherits the rest.
* **Module** — a sub-project (`engine`, `backend`, `sim`) with its own `pom.xml`.
* **Dependency** — a library you use by naming it in the pom; Maven downloads it into `~/.m2/repository`.
* **BOM (Bill of Materials)** — a pom that only lists library versions known to work together (a version catalogue: it fixes versions, downloads nothing). We import Spring Boot's (`spring-boot-dependencies`) and, from Step 28, the AWS SDK's (`software.amazon.awssdk:bom` with `<type>pom</type><scope>import</scope>`) so JUnit, AssertJ and Jackson get tested versions without typing a number; importing it adds no Spring code.
* **`dependencyManagement` vs `dependencies`** — the first says "if a module asks for library X, use version Y" and downloads nothing; the second actually adds the library to a module. Same split for plugins: `pluginManagement` pins versions, `plugins` runs them.
* **Plugin / goal / execution** — a plugin is a piece of Maven that does one job (compiler, surefire, JaCoCo, enforcer); a goal is one task of it (`jacoco:check`); an execution binds a goal to a point in the build.
* **Phase (build lifecycle)** — Maven runs phases in a fixed order: `validate → compile → test → package → verify`. Asking for `verify` runs everything before it; a failing phase stops the rest, so nothing downstream happens on red.
* **Reactor** — the list of modules Maven builds in one run, parent first (the "Reactor Summary" at the end). `-pl engine` limits it to one module; `-N` builds only the pom you are in.
* **Scope `test`** — a dependency used only to compile and run tests; it never ships with the code.
* **Surefire** — the plugin that runs the JUnit tests and writes `target/surefire-reports/`. Always read the `Tests run:` line: code saved in the wrong folder gives a green build with zero tests.
* **Enforcer** — the plugin that refuses to build on the wrong Java or Maven version (`[25,26)` = 25 or higher, below 26).
* **Coverage gate** — JaCoCo's `check` goal: the build fails if too few code lines ran under a test (60 % now, 90 % from Step 23). The HTML report is `engine/target/site/jacoco/index.html`.
* **Maven flags** — `-B` batch mode (plain output, what CI uses) · `-ntp` no download progress bars · `-pl <module>` only that module · `-N` do not recurse into modules · `-Dtest=BandTest` run one test class · `-v` print the version.
* **`src/main/java` / `src/test/java`** — Maven's folder convention for real code and test code; no configuration names them. Below each, the folders mirror the package name (`com/forgottenheroes/engine`).
* **`target/`** — Maven's build output (classes, jar, reports); gitignored and recreated on every build.
* **Package** — Java's way of grouping classes, like a namespace; the `package` line at the top of a file must match its folder. `package-info.java` is a file that holds only the package's Javadoc.
* **Javadoc** — the `/** … */` comment style that tools turn into documentation.
* **Annotation** — a label on code that tools read, written `@Name`. `@Test` tells JUnit "this method is a test".
* **`import static`** — lets you call a static method by its short name: `assertThat(…)` instead of `Assertions.assertThat(…)`.
* **Smoke test** — the smallest test that proves the machine turns on; `SmokeTest` checks `1 + 1 == 2`. A failing assertion prints `expected: 3 / but was: 2` and the file and line to open.
* **Record** — a class that only carries data: you name the fields once and Java writes the constructor, the getters, `equals`, `hashCode` and `toString` (`record Stats(int attack, int health)`). Two records with the same values are equal.
* **Immutable** — a value that never changes after it is made. To "change" a record you make a new one (`stats.withAttack(5)` returns a new `Stats`; the original is untouched). Safe to share, easy to test, no hidden state.
* **Compact constructor** — the short block inside a record, written `public Stats { … }` with no parameter list, that checks the incoming values before Java stores them (`if (attack < 0) throw new IllegalArgumentException(…)`). It refuses bad values; it never fixes them.
* **`IllegalArgumentException`** — the standard exception for "you called me with a value that makes no sense". Our value types throw it; tests catch it with `assertThatThrownBy(() -> …)`.
* **Enum** — a type with a fixed list of named values and nothing else (`enum Rank { I, II, III, IV, V, VI }`), so `rank = 7` cannot compile. Values can carry data and methods (`Rank.VI.turnUnlocked()`); `values()` is the list in order and `ordinal()` a value's position in it, from 0.
* **Constant (`static final`)** — one shared value that never changes (`Rules.BAND_SIZE = 5`). Every rulebook number lives in `Rules`; nothing else may hard-code them. A constants holder has a private constructor so nobody writes `new Rules()`.
* **Derived value** — something computed from stored data instead of stored beside it: a hero's level is `Xp.level()`, never a second field that could disagree.
* **Nested class** — a class written inside another (`UnitId.Sequence`), used when a helper only makes sense next to its owner.
* **Lambda** — a small piece of code handed over to run later, written `() -> new Stats(-1, 3)`; AssertJ runs it and catches the exception.
* **Ternary `a ? b : c`** — reads "if a then b, else c" in one expression (`this == VI ? VI : values()[ordinal() + 1]`).
* **Parameterized test** — one test method run once per row of a table: `@ParameterizedTest` + `@CsvSource({"3, II", …})`; JUnit converts `"3"` to an `int` and `"II"` to `Rank.II`. The GDD's turn and XP tables are tests this way.
* **Hash / mixer** — a function that turns any input into a fixed-size number that looks random, where the same input always gives the same output (`Hash64.of(runSeed, turn)`). Ours is *splitmix64*: shift, XOR, multiply, repeat; the same mixer `SplittableRandom` uses inside.
* **`long` and hex literals** — `long` is a 64-bit whole number (`int` is 32). `0x9E3779B97F4A7C15L` is one written in hex (base 16); the trailing `L` says "this is a long". An `int` passed where a `long` is expected is widened for free; the other way round does not compile.
* **Bit operators** — `x >>> 30` slides the bits of `x` 30 places to the right; `x ^ y` is XOR (a bit is 1 where exactly one input bit is 1). Only the mixer uses them.
* **Varargs `long... parts`** — a parameter that accepts any number of values (`Hash64.of(42)`, `Hash64.of(42, 5, 7)`); inside the method it is an array, walked with `for (long part : parts)`.
* **Generics `<T>`** — a placeholder type: `<T> T pick(List<T> items)` picks from a list of heroes or of relics and returns that same type, no casting. `List<Integer>` reads "a list of integers".
* **`List` / `ArrayList` / `List.copyOf`** — `List` is Java's ordered collection; `new ArrayList<>(items)` is a growable copy you may change; `List.copyOf(copy)` and `List.of(1, 2, 3)` are lists nobody can change. `Collections.shuffle` mutates the list it is given, so shuffle a copy.
* **Test helper** — a private method inside a test class that several tests share (`draw(random, 1000)` pulls a list of numbers so two sequences can be compared with one `isEqualTo`).
* **Known-answer test** — a test that compares against a value published elsewhere: `Hash64.of(0)` must equal `0xE220A8397B1DCDAF`, splitmix64's first output for seed 0, and `new SplittableRandom(0).nextLong()`. Three sources agreeing rules out a typo in the constants.
* **jshell** — the JDK's interactive Java prompt: it compiles and runs lines as you type them, no class or `main` needed. `jshell -q --class-path engine/target/classes -` reads a script from stdin against the classes Maven just built.
* **Sealed interface** — an interface with a fixed, listed set of implementations; lets the compiler
  check we handled every `Action`/`GameEvent` type (`switch` exhaustiveness).
* **JUnit 5 / AssertJ** — test framework / readable assertions (`assertThat(gold).isEqualTo(7)`).
* **jqwik** — property-based testing: you state a rule ("gold never negative") and it tries hundreds of random inputs. `@Property void name(@ForAll long seed, @ForAll @IntRange(min = 1, max = 120) int steps)` runs the method once per generated input; `@Provide Arbitrary<Band> bands()` builds structured inputs with `Combinators.combine(...)`, `.list()`, `.map(...)`; when a case fails, jqwik **shrinks** it to the smallest failing input and prints the seed to replay it. Ours live in `engine/src/test/java/.../properties/`.
* **ArchUnit** — tests about code structure, written as rules: `noClasses().should().dependOnClassesThat().resideInAnyPackage("org.springframework..", …)`, `noClasses().should().callMethod(Math.class, "random")`. `@AnalyzeClasses(packages = …)` imports the compiled classes once, each `@ArchTest static final ArchRule` field is one test, and `.because("…")` puts the reason in the failure message. The engine's rules: no Spring/AWS/Jakarta, no `java.time`, no `java.net`/`java.nio.file`/`java.sql`, no `System.currentTimeMillis`/`nanoTime`, no `Math.random`, no `java.util.Random`/`SecureRandom`, only `GameRandom` touches `SplittableRandom`, the model knows no JSON, and gold/hearts/crowns are set only by mutators and the model. The sim's rule: nothing from `engine.resolver` or `engine.mutator` except `RuleViolation`.
* **JaCoCo** — measures test coverage; the engine's gate is now 90 % of lines (it stood at 97.9 % when the gate was raised). **PIT** — mutation testing: changes your code on purpose (flips a `<` to `<=`, removes a call) and reports which mutants the tests failed to kill. Optional here: `./mvnw -pl engine -Ppit verify`, report in `engine/target/pit-reports/index.html`; it takes minutes, so it is a profile, never part of `verify`.
* **Jackson** — converts Java objects ⇄ JSON. We use Jackson 3 (`tools.jackson.databind`), the same one Spring Boot 4 uses: `JsonMapper.builder().build()` makes the mapper once, `readTree` parses text into a tree, `treeToValue(node, HeroDefinition[].class)` fills records (a record's components are the JSON field names, no annotations needed), `writeValueAsString` goes the other way.
* **Polymorphic JSON (`@JsonTypeInfo` / `@JsonSubTypes`)** — one JSON field (`"type": "SUMMON"`) tells Jackson which record to build. The annotations sit on the sealed interface (`EffectDefinition`) and list every name ⇄ record pair; a name not in the list fails to load.
* **Getter auto-detection** — Jackson treats any `isSomething()` or `getSomething()` method as a field called `something`, so `TavernSlot.isEmpty()` would show up as `"empty": true` in the JSON. Both engine mappers switch that off (`changeDefaultVisibility(v -> v.withIsGetterVisibility(NONE))`) so a record's components are its only fields; the alternative is `@JsonIgnore` on each such method.
* **Custom (de)serializer** — a tiny class that tells Jackson how to read or write one type: the files say `"rank": 1`, the records hold `Rank.I`, and `RankFromNumber` / `RankToNumber` (inside `ContentLoader`) bridge the two, so `Rank` itself knows nothing about JSON.
* **JSON Schema** — a JSON document that describes the shape another JSON document must have (required fields, allowed values, no unknown fields). `content.schema.json` is checked by `json-schema-validator` before Jackson sees the content, and a typo fails with the path to it: `/heroes/0/ability: property 'triger' is not defined`. `$defs` holds named pieces, `$ref` points at them, `oneOf` says "exactly one of these shapes", `const` pins a field to one value.
* **Classpath resource** — a file packed next to the compiled classes (`src/main/resources/content/heroes.json` lands at `content/heroes.json` in the jar) and read with `getClassLoader().getResourceAsStream(...)`. Test-only files go under `src/test/resources`.
* **Exhaustive `switch` over a sealed type** — `switch (effect) { case Summon s -> …; case GainGold g -> …; }` with one `case` per permitted record and no `default`; add an eleventh effect and every such switch stops compiling until it handles it. Pattern `case Summon s` also gives you the typed value, no cast.
* **`Integer` vs `int`** — `int` is a plain number that always has a value; `Integer` is the boxed object version that can be `null`, which is how a record such as `Amount(Integer flat, Integer perLevel, …)` says "this field was not set".
* **`instanceof` pattern** — `if (effect instanceof Summon summon)` tests the type and, when true, gives you `summon` already cast.
* **Aggregate / aggregate root** — a group of objects that must stay consistent together, reached through one root object. `RunState` is ours: it owns the band, the tavern, gold and hearts, and `assertInvariants()` checks the whole group at once.
* **Mutable class vs record** — `Unit`, `Band`, `Tavern` and `RunState` are ordinary classes whose fields change in place (`unit.takeDamage(2)`), because the mutators edit state step by step and emit an event per change. Records stay at the edges (`Stats`, `TavernSlot`, `BattleSummary`): small values that are replaced, never edited.
* **Invariant** — a fact that must hold after every action (gold 0..50, at most five units, no fallen unit on the band outside a battle). Setters refuse single bad values; `assertInvariants()` checks the facts that span several fields and throws `IllegalStateException`.
* **`IllegalStateException`** — "the object is not in a state where this call makes sense" (adding to a full band), as opposed to `IllegalArgumentException`, "the value you passed makes no sense". `IndexOutOfBoundsException` is the standard one for a bad list index.
* **Deep copy** — a copy whose inner objects are copied too, so changing the copy never touches the original (`RunState.copy()`, `Band.copyForBattle()`). A shallow copy would share the same `Unit` objects.
* **`equals` / `hashCode`** — the two methods that define when two objects count as "the same value". Records get them for free; our mutable classes write them by hand over every field, so a test can say `assertThat(copy).isEqualTo(original)` and the resolver can prove a rejected action changed nothing.
* **`Optional<T>`** — a box that holds a value or nothing, instead of returning `null`: `unit.perk()` is `Optional<Perk>`, read with `isPresent()`, `contains(...)` in tests, or `orElse(...)`.
* **`Objects.requireNonNull(x, "name")`** — throws at once, with the name, if `x` is null; put in constructors so a missing value fails where it was passed, not three calls later.
* **Sentinel value** — one well-known instance that means "nothing here" without using `null`: `TavernSlot.EMPTY` is a slot with no item, so a list of slots never holds nulls and `slot.isEmpty()` reads naturally.
* **Read-only view** — `band.units()` returns `List.copyOf(...)`, a list nobody can change, so callers read the order but only the band's own methods (`insert`, `move`, `remove`) may edit it.
* **Golden test (golden file)** — a test that compares a program's output with a file committed in the repo (`src/test/resources/golden/events-sample.json`). The file is reviewed by eye once and then guards the contract: any accidental change to an event's JSON fails the build. When the change is intended, `-Dgolden.update=true` rewrites the file and the diff is reviewed in the PR.
* **Typed writer (`writerFor(TypeReference)`)** — Java erases the element type of a `List<GameEvent>` at runtime, so Jackson writing a plain list does not know the elements are `GameEvent`s and leaves out their `type` field. `writerFor(new TypeReference<List<GameEvent>>() {})` tells it. `TypeReference` is the trick that carries a generic type to runtime.
* **`SIMPLE_NAME` type id** — `@JsonTypeInfo(use = SIMPLE_NAME)` writes `"type": "PerkGained"`, the record's own name, instead of `GameEvent$PerkGained` (what `NAME` gives a nested record) or the full class name; the names in the JSON then never depend on Java packages.
* **Interface default method / `withSeq`** — every event record implements `withSeq(int)` by returning a copy with the number filled in; records are immutable, so "assigning" the sequence number means making a new record.
* **Unchecked cast / `@SuppressWarnings("unchecked")`** — `(E) event.withSeq(n)` tells the compiler "trust me, this is the same type"; the annotation silences the warning on exactly that line and nowhere else.
* **Pretty printer** — Jackson's indented output. Ours pins the line end to `\n` so the golden file is identical on Windows and Linux (the default uses the platform's line end, which made the first run fail on Windows).
* **Validate, then apply** — a resolver first checks every rule an action could break and throws `RuleViolation` while nothing has changed yet; only then does it call mutators. That is what makes "a rejected action leaves the state exactly as it was" true without undo code.
* **`RuleViolation` / `Code`** — the engine's "not allowed" exception: a stable `code` (`NOT_ENOUGH_GOLD`, `SLOT_EMPTY`, `INVALID_INDEX`, `WRONG_PHASE`, `RUN_OVER`, …) the API and the UI can switch on, plus a human message. `RuleViolation.require(condition, code, message)` keeps the resolver reading like the rulebook.
* **Exhaustive `switch` over actions** — `switch (action) { case StartTurn s -> …; case Roll r -> …; }` with one case per permitted record: when `Sell` is implemented in the next step, the compiler points at this switch, and a new action can never be silently ignored.
* **Static utility class** — a `final` class with a private constructor and only `static` methods (`GoldMutator.spend(...)`): no object to create, just named operations. The mutators are written this way because they hold no state of their own; the state is the `RunState` they are handed.
* **`UnsupportedOperationException`** — Java's "this exists but is not implemented here": the resolver throws it for `Buy`/`Sell`/`Move`/`Merge`/`BuyRelic` until their step lands, which is honest and loud instead of a silent no-op.
* **`switch` with `yield`** — a `case` that needs several statements uses a block and `yield value;` to hand its result to the switch expression (the random-action loop in `TavernEconomyTest`).
* **Boolean return as a signal** — `BandMutator.stack(...)` and `UnitMutator.gainXp(...)` return `true` when the unit crossed a level, so the resolver can open the bonus slot and record the `LEVEL_UP` trigger without re-deriving it.
* **Context object** — one record that carries everything a piece of code needs about "now" (`EffectContext`: the state, the band and side, who fires, who was chosen, the level, the dice, the log, the pending triggers), instead of a method with twelve parameters. A static factory (`EffectContext.forRelic(...)`) builds the common shape.
* **Interpreter (effect applier)** — code that reads data and acts on it: `EffectApplier.apply(effect, context)` switches over the sealed `EffectDefinition`, resolves each `Amount` for the level, asks `Targeting` for the units and calls the mutators. Adding a relic is a JSON row because the interpreter already knows every verb.
* **`Optional.ofNullable` / `orElse(null)`** — the bridges between "may be null" fields and `Optional` return types (`unit.perk()` is an `Optional`; the event stores `perk().orElse(null)` so an absent perk is left out of the JSON).
* **`Integer` parameter that may be null** — `BuyRelic(slot, Integer bandIndex)`: a relic used on "one hero" needs the index, one that picks its own targets must not get one (`TARGET_REQUIRED` / `TARGET_NOT_ALLOWED`); `int` could not express "not given".
* **Queue vs recursion (breadth-first)** — when an ability causes another moment (a summon makes allies react), the dispatcher puts that moment at the back of a `Deque` and keeps going, instead of calling itself. A recursive version would grow the call stack with every cascade and a long one would crash the server with a `StackOverflowError`; a queue uses a list, keeps the order obvious, and lets one counter guard the whole run.
* **Guard (`ENGINE_GUARD`)** — a hard ceiling on work per action (1,000 ability activations). Content can loop (a test hero that summons a 0-health token whenever an ally falls); the guard turns an infinite loop into a `RuleViolation` the API can report instead of a hung Lambda.
* **Comparator chain** — `Comparator.comparingInt(...).reversed().thenComparing(...).thenComparingInt(...)`: the ordering rule of GDD §4.6 as one object (`TriggerDispatcher.ordering()`), used to sort candidates and fallen units alike.
* **Inner class with state (`Run`)** — a class declared inside another that can see its fields; the dispatcher keeps its long-lived parts (content, interpreter) in the outer object and one dispatch's queue and activation count in a fresh `Run`, so two dispatches never share state.
* **Interface as a seam (`Board`)** — the dispatcher works against `Board` (bands by side, maybe a run state, an id source); the tavern and the battle each implement it, so one trigger system serves both without knowing which it is in.
* **Simultaneous damage** — both fronts' hits are computed from the stats *before* either lands (`Damage.attack(a, b)` and `Damage.attack(b, a)` first, then both `UnitMutator.damage` calls), so a unit that dies still deals its full blow. That is why two 1/1 fronts are a draw, not a home win.
* **Hand-calculated golden test** — the expected event list is written by hand from the rulebook *before* running the code, then the test asserts the exact sequence (`containsExactly` over events with `seq` stripped). If the engine and the hand disagree, one of them misread the GDD, and the test says which line.
* **Clash cap** — `Rules.CLASH_CAP = 300`: the loop condition `while (bothAlive && clashes < CLASH_CAP)` is the server's promise never to hang on a content combo that cannot kill anything; the result is a draw with reason `CLASH_CAP`.
* **One code path for every hit** — attacks, splashes and ability damage all go through `Damage.incoming`/`attack` and `Damage.spend`, so a defender's Chainmail, Ward or Aegis works the same whichever way the damage arrives (Flame Oil's splash on a Chainmail unit is 3, not 5). Two code paths would drift apart the first time one of them changed.
* **Spend before the damage lands** — a hit is computed (`Hit`), its once-only perks are reported (`PerkConsumed`), then the damage is applied; the log reads "the ward shatters, 5 damage" instead of the other way round, and the frontend can animate it in that order.
* **Table-driven test / `@TestFactory`** — the test is data: `ScenarioTest` lists every file under `src/test/resources/scenarios` and returns one `DynamicTest` per file, so 59 scenarios are 59 named tests from one runner, and a new hero is a new JSON file, never a new Java method.
* **Subsequence matching** — the scenario's `expectEvents` must appear in order among the actual events, but other events may sit in between; the runner walks a cursor forward and fails with the whole story printed, numbered, so the fix is one look away.
* **Meta-test** — a test about the tests: `everyContentIdHasAScenario` fails the build when a hero, relic or token has no `<id>.json` scenario, which is how "every hero has a scenario" stays true without anyone remembering.
* **Facade** — one class with a few methods that hides a whole subsystem: `GameSession.start`, `apply`, `endTurn`. The backend and the simulator call only these; the resolvers, dispatcher and mutators behind them can change without either noticing (Step 23's ArchUnit rule will enforce it for the simulator).
* **Version (optimistic locking, engine side)** — `RunState.version` goes up by one on every accepted `start`, `apply` and `endTurn`. The backend will store it with the run and refuse a write whose version is stale (409 → the client reloads), so two tabs cannot both spend the same gold.
* **Functional interface / lambda as a strategy** — an interface with one method (`GhostProvider.ghostFor`) can be written as a lambda: `(turn, state, rng) -> state.toSnapshot()` is the mirror provider. `@FunctionalInterface` makes the compiler check it stays single-method.
* **Anonymous class** — a one-off implementation written inline (`new Bot() { ... }` in the test for a bot that only proposes illegal moves); handy in tests, rare in main code.
* **Safety valves** — hard ceilings that keep a loop finite whatever the inputs: `MAX_VIOLATIONS_IN_A_ROW` (a bot that keeps proposing illegal moves gets its turn ended), `MAX_ACTIONS_PER_TURN`, `MAX_TURNS` (a run of draws stops at 60). The runner must finish for the simulator to be trustworthy.
* **picocli** — the library that turns a class into a command line: `@Command(name = "run")` names a sub-command, `@Option(names = "--games")` binds a flag to a field, `Callable<Integer>` returns the exit code. `sim --help` is generated from the annotations.
* **Exit code** — the number a program hands back to the shell: 0 means fine, anything else means trouble. `sim check` exits 1 when a threshold fails and `sim fuzz` exits 1 when it found a bug, so a nightly job can fail on them; `SimMain` calls `System.exit` only on a non-zero code so `exec:java` does not kill Maven on success.
* **`exec-maven-plugin` / `exec:java`** — runs a main class inside Maven with the module's classpath, so no jar has to be built or installed; `scripts/sim.sh` wraps `./mvnw -pl sim -am compile exec:java -Dexec.args=…`. It is configured as skipped in the root pom and switched on only in `sim`, otherwise Maven would try to run it in every module.
* **Multi-module reactor in practice** — `-pl sim -am` ("also make") builds `engine` first from the source tree; without `-am` Maven would look for an installed engine jar in `~/.m2` and not find one.
* **Fuzzing** — throwing random inputs at code to find crashes: `sim fuzz` sends random legal and illegal actions through `GameSession`, treats a `RuleViolation` as a correct refusal and anything else as a bug, and prints the seed plus the actions as a replay file.
* **Percentile** — the value below which a share of the samples fall: the 99.9th percentile of battle length is the clash count only one battle in a thousand exceeds; the median is the 50th.
* **Invariant property vs example test** — an example test proves one case ("this merge gives 3/3"); a property proves a rule for every generated input ("a rejected action leaves the state equal to a copy, for any seed and any action list"). The five engine properties: determinism of the event JSON, invariants after any accepted action (gold 0..50, band ≤ 5, every unit alive, slot counts match the turn, frozen shelves survive a roll), rejected actions change nothing, battles end under the cap for random bands with random perks, and `RunState` → JSON → `RunState` is the identity.
* **DTO (data transfer object)** — a plain record that mirrors an object's fields for storage or transport: `RunStateJson.RunStateDto`, `UnitDto`, `TavernDto`. The mutable model stays Jackson-free; `RunState.restore(...)`, `Unit.restore(...)`, `Tavern.restore(...)`, `Counters.restore(...)` rebuild the objects from the data. This is what the backend will put in DynamoDB.
* **Maven profile** — a named, optional slice of the build: `-Ppit` adds the PIT plugin to `verify`; without the flag the build does not know PIT exists.
* **Generated documentation / markers** — a block of a Markdown file that a script writes from the source, between two HTML-comment markers (`<!-- events:start -->` … `<!-- events:end -->`): `tools/docs/event_catalog.py --write` fills the event catalogue of `engine/README.md` from the Javadoc one-liners in `GameEvent.java`, and `--check` fails when the file is stale, so the reference and the code cannot drift.
* **Clean-clone demo** — the phase-end proof: `git clone` into an empty folder, run the documented commands, watch them work. If it only works on the author's machine, it does not work.
* **`Function<T, R>` / method reference** — a value that is a one-argument function; `HeroDefinition::id` is the method `id()` handed over as one, used by `ContentIndex` to index any list by its id.

## Spring Boot
* **Spring Boot** — the standard Java web framework; auto-configures a web server, JSON, validation.
* **Bean** — an object Spring creates and hands to whoever needs it (**dependency injection**).
* **Controller** — receives HTTP requests (`@RestController`). **Service** — business logic.
  **Repository** — talks to the database. Together: the **layers**.
* **DTO (Data Transfer Object)** — the JSON shape sent over the wire, separate from internal models.
* **Profile** — a named configuration set (`local`, `prod`). Lets local dev skip Cognito safely.
* **ProblemDetail (RFC 9457)** — the standard JSON shape for errors.
* **springdoc / OpenAPI** — generates a machine-readable description of the API; the frontend
  generates TypeScript types from it.
* **MockMvc / `@WebMvcTest`** — test a controller without starting a real server.
* **Spring Initializr** — start.spring.io, a generator for a known-good Spring Boot project. Step 25 downloaded its zip to
  read, then typed the `backend` pom by hand with our root pom as parent.
* **Auto-configuration** — Spring Boot looks at what is on the classpath (a web starter, Jackson, Actuator) and creates the
  beans a sensible app would need, unless you define your own. Start the app with `--debug` to print the "conditions
  report": every auto-configuration and why it matched or did not.
* **Starter** — one dependency that pulls in a whole feature with versions that fit: `spring-boot-starter-webmvc` (Spring MVC
  plus embedded Tomcat plus Jackson). In Spring Boot 4 the old `spring-boot-starter-web` name is gone, and every starter has
  a `-test` twin (`spring-boot-starter-webmvc-test`) with the matching test tools.
* **`@SpringBootApplication`** — the one annotation on the entry class: configuration + auto-configuration + component scan of
  its package and below. `SpringApplication.run(...)` in `main` starts the app.
* **Component scan** — Spring walks the entry class's package and sub-packages and makes a bean of every class marked
  `@RestController`, `@Service`, `@Repository` or `@Component`.
* **Constructor injection** — a class asks for what it needs as constructor parameters (`HealthController(GitProperties git)`)
  and Spring passes the matching beans in; we never write `new` for a collaborator.
* **`application.yaml`** — the settings file Spring reads at startup (`server.port`, `spring.application.name`); the YAML twin of
  `application.properties`. Profile-specific files are `application-<profile>.yaml`.
* **Slice test** — a test that starts only one layer: `@WebMvcTest(HealthController.class)` builds the web layer with that
  controller and nothing else, so it runs in a second. `@SpringBootTest` is the opposite: the whole app, as `spring-boot:run`
  would start it; an empty `contextLoads()` test proves every bean can be created.
* **Mock / `@MockitoBean`** — a fake object scripted for one test (`given(git.getShortCommitId()).willReturn("abc1234")`); it
  stands in for a bean the slice does not build. Replaces Spring Boot 3's `@MockBean`.
* **`MockMvcTester`** — sends a request through Spring MVC without a network or a server and lets AssertJ check the reply:
  `assertThat(mvc.get().uri("/api/health")).hasStatusOk().bodyJson().isLenientlyEqualTo("{...}")`.
* **`GitProperties` / `git-commit-id-maven-plugin`** — the plugin writes `git.properties` (branch, sha, time) into the build
  at compile time; Spring Boot sees that file and auto-configures a `GitProperties` bean, which `/api/health` reads for its
  `version`. `failOnNoGitDirectory=false` keeps a build from a source zip working.
* **Plugin `skip` switch** — a plugin listed in the root `pluginManagement` with `<skip>true</skip>` runs nowhere until a module
  sets `<skip>false</skip>`; `spring-boot-maven-plugin` (backend) and `exec-maven-plugin` (sim) both use it so `-am` builds
  do not try to run the root pom or the engine.
* **Testcontainers** — starts a real database in Docker just for a test, then throws it away. `@Testcontainers` on the
  test class and `@Container` on a `static GenericContainer` field give one box per test class, started before the first
  test and removed after the last; `getMappedPort(8000)` tells you which free port Docker chose on your PC, so the test
  never collides with the `docker compose` box. The jars come from the Spring Boot BOM (`org.testcontainers:testcontainers`
  and `testcontainers-junit-jupiter`, test scope).
* **`@Configuration` / `@Bean`** — a class whose methods build beans by hand (`DynamoConfig`): each `@Bean` method runs
  once at startup and its return value is handed to whoever asks for that type. `proxyBeanMethods = false` tells Spring
  the methods never call each other, which skips a proxy and starts faster.
* **`@Value("${app.dynamodb.endpoint:}")`** — inject one setting from `application.yaml` into a method or constructor
  parameter; the part after the colon is the default when the setting is missing (here the empty string).
* **`@ConditionalOnProperty`** — "create this bean only when a setting has this value": `name = "app.storage",
  havingValue = "dynamo"` on the DynamoDB beans, `havingValue = "memory", matchIfMissing = true` on the in-memory ones,
  so exactly one storage set exists and the default needs no setting at all. The game's `app.storage` switch.
* **Sealed interface** — `sealed interface ActionRequest permits …`: exactly these records implement it, no others. A
  `switch` over it needs no `default` and fails to compile when a case is missing. The engine's `Action` and `GameEvent`
  are sealed too.
* **`@JsonTypeInfo` / `@JsonSubTypes`** — tell Jackson to read a `"type"` field (`"Buy"`) and pick the matching record,
  and to write it back on the way out. How one request shape can be eight.
* **Validation annotations** — `@NotNull`, `@PositiveOrZero`, `@Size` on record components, checked by Spring before the
  controller method runs when the parameter is marked `@Valid`. Package `jakarta.validation`, from
  `spring-boot-starter-validation`.
* **`Integer` vs `int`** — `int` is a primitive and cannot be null; `Integer` is the boxed object and can. A missing
  JSON field becomes `null`, which Jackson 3 refuses to put into an `int` (crash) but happily puts into an `Integer`
  (then `@NotNull` reports it). Use the wrapper for anything a client might leave out.
* **`Optional<T>`** — a box that is either empty or holds a value, instead of a null that bites later;
  `unit.perk().orElse(null)` unboxes it at the edge where JSON wants a plain value.
* **`@RequestMapping` / `@GetMapping` / `@PostMapping`** — which URL and method a controller method answers.
  **`@PathVariable`** reads `{runId}` from the path, **`@RequestParam`** reads `?season=…`, **`@RequestBody`** parses
  the JSON body into a record. **`@ResponseStatus`** sets a status other than 200 (201 for a created run).
* **`-parameters` compiler flag** — keeps method parameter names in the class file. Spring 7 reads `@PathVariable`
  names from them instead of guessing, so the root pom turns it on for every module.
* **`@Service`** — a bean in the service layer: the logic between a controller and the engine or the database.
  **`@Component`** is the generic marker; `@RestController`, `@Service` and `@Repository` are the same thing with a
  role name.
* **`@RestControllerAdvice` / `@ExceptionHandler`** — one class whose methods catch exceptions from every controller and
  turn each into a response. Ours extends `ResponseEntityExceptionHandler` so Spring's own web errors come through it
  too.
* **`ProblemDetail` `code`** — our one extra field on the RFC 9457 body: stable text (`NOT_ENOUGH_GOLD`,
  `VERSION_CONFLICT`) the frontend switches on; `title` and `detail` are for humans and may change.
* **422 vs 400** — 400 means "I could not read or validate your request" (bad JSON, missing field); 422 means "I read
  it fine, but the rules say no" (not enough gold). The client reacts differently: fix the request, or show the rule.
* **`@SpringBootTest` + `@AutoConfigureMockMvc`** — the whole app, with `MockMvcTester` to call it without a network;
  the contract test uses it because springdoc needs every controller to be present.
* **`@ActiveProfiles("prod")`** — run a test with that profile on, so `application-prod.yaml` applies.
* **Swagger UI** — the web page springdoc serves at `/swagger-ui.html`: every route, its shapes, and a "Try it out"
  button. `/v3/api-docs` is the same information as JSON (the OpenAPI document).
* **Snapshot test** — a test that compares a generated file with a committed copy and fails on any difference, so a
  change to the contract is a deliberate, reviewed diff rather than an accident.
* **`JacksonModule` bean** — a Jackson module declared as a Spring bean is added to Spring's JSON mapper automatically;
  `ApiJsonModule` hands the engine's module to Spring that way.
* **Service layer** — the classes that hold use-cases ("start a run", "end a turn"): each one is load → engine → save.
  A controller calls a service; a service calls the engine and the repositories; nothing skips a layer.
* **Repository interface** — `RunRepository` says *what* storage must do (`find`, `save`) and nothing about *how*. The
  service depends on the interface; `InMemoryRunRepository` today and the DynamoDB one in Step 28 both implement it, so
  the service does not change when the storage does. That is why we inject interfaces, not concrete classes.
* **Constructor injection** — all collaborators arrive as constructor parameters into `final` fields; no `@Autowired`
  on fields. A test can build the service by hand with in-memory repositories and a fixed seed, no Spring needed.
* **`@Repository`** — the bean marker for the storage layer (the same as `@Component` with a role name, like
  `@Service`). Spring finds one implementation per interface and injects it.
* **In-memory repository** — a `ConcurrentHashMap` behind the interface: gone when the app stops, fast forever in tests.
  Ours stores runs as JSON text so a caller gets a fresh copy back, exactly like a database would.
* **`CurrentUser`** — the bean that answers "who is calling"; controllers pass its `id()` down so services never read
  a request. A stub says `"dev"` until Step 31 reads the Cognito token.
* **`SecureRandom`** — Java's unpredictable random numbers, used once per run for the seed. The engine itself never
  draws randomness (ADR-4); the backend draws one number and the engine derives everything from it.
* **`UUID.randomUUID()`** — a 128-bit random id (`3f2a…`), unique without a counter or a database round trip.
* **`@SpringBootTest(webEnvironment = RANDOM_PORT)`** — boot the whole app on a real port that is free right now, so
  the test speaks genuine HTTP. **`TestRestTemplate`** is the HTTP client made for it (relative URLs, no exceptions on
  4xx/5xx). In Spring Boot 4 it lives in `spring-boot-resttestclient` and needs `spring-boot-starter-restclient` too.
* **Optimistic locking, step one** — the client sends `expectedVersion`; the service compares it with the saved
  version and answers 409 if they differ. Step 29 adds the second half: the database refuses the write itself.

## AWS
* **Region** — a geographic datacenter group; we use **us-east-1** (N. Virginia).
* **IAM** — identities and permissions. **Role** — a set of permissions something can *assume*
  temporarily. **Least privilege** — grant only what is needed.
* **Lambda** — run code without a server; billed per request and per millisecond. **Cold start** —
  the delay when Lambda must boot a fresh copy. **SnapStart** — Lambda saves a snapshot of the
  booted JVM and restores it fast; free for Java. **Reserved concurrency** — a cap on how many copies may run at once.
* **API Gateway (HTTP API)** — the public HTTPS front door that forwards requests to Lambda and
  can check JWTs. **Stage** — a deployed version with its own URL and throttle settings.
* **DynamoDB** — a key-value/document database; you design around **partition key (PK)** and
  **sort key (SK)**. **Single-table design** — many entity types in one table, distinguished by key
  prefixes. **On-demand** — pay per request. **TTL** — items auto-delete after a timestamp.
  **Conditional write** — "update only if version = 7" (our optimistic locking). **GSI** — a
  secondary index (costs extra writes; we avoid unless needed).
* **Item / attribute** — DynamoDB's row and column, except that every item may have different attributes. Only the two
  key attributes (`PK`, `SK`) are declared when the table is created (`infra/table.json`); everything else is whatever
  the writer sends. Attribute names are case-sensitive: `PK` and `pk` are two different attributes, which is why
  `ProfileItem` carries `@DynamoDbAttribute("PK")`.
* **GetItem / PutItem / UpdateItem / Query** — the four calls the game uses. GetItem: both keys in, one item out (or
  nothing). PutItem: write the whole item, replacing whatever was there (no "insert or update" question, and null fields
  are simply left out). UpdateItem: change some attributes of an item in place (we use it only with a condition, Step 29).
  Query: every item of one partition key, in sort-key order, optionally within a sort-key range.
* **Enhanced Client** — the AWS SDK's translator between a Java class and an item: `@DynamoDbBean` on a public class
  with a no-arg constructor and a getter/setter per attribute, `@DynamoDbPartitionKey` / `@DynamoDbSortKey` on the two
  key getters, `TableSchema.fromBean(ProfileItem.class)` reads that once, and `DynamoDbTable<ProfileItem>` offers
  `getItem` / `putItem` / `query` in Java terms. It sits on top of the plain `DynamoDbClient` (the phone line). Dates
  (`Instant`) are stored as ISO strings, numbers as `N`, booleans as `BOOL`.
* **Composite sort key** — several values in one sort key, most important first, separated by `#`:
  `07#2026-10-16T12:00:00.000Z#r1` is crowns, then time, then run. Because DynamoDB sorts the key as text, one drawer
  then answers "crowns between 6 and 8, newest first" as a range of the sort key. Numbers are zero-padded to a fixed
  width (`07`, `BATTLE#001`) and times written at a fixed length, or `"10"` sorts before `"9"` and `"…26Z"` after
  `"…26.728Z"`.
* **Query / `scanIndexForward`** — read every item of one partition key in sort-key order; `scanIndexForward(false)`
  reads the order backwards (for ghosts: highest crowns first, newest first among equal crowns). The result comes in
  pages; the Enhanced Client's `items()` walks them for you.
* **TTL (`expiresAt`)** — a per-table setting (`update-time-to-live`) naming one attribute; an item whose number in that
  attribute (seconds since 1970, type `N`) is in the past is deleted by DynamoDB itself, for free, usually within a day
  or two. Items without the attribute are kept for ever: a run gets `expiresAt` only once it is over (90 days), ghosts
  30 days after they were saved, battle logs 7 days.
* **JSON attribute** — the run state and the battle events are stored as one text attribute holding the engine's own
  JSON (`RunStateJson`, `EventJson`). The ledger never looks inside; one GetItem returns the whole thing and the engine's
  mapper reads it back exactly. The price: you cannot query by anything inside it, which is why a few numbers are copied
  out next to it.
* **Item size limit** — 400 KB per item, hard. The battle-log repository refuses at 300 KB so the limit is hit in a test,
  not in production (Step 36 compresses the events).
* **DynamoDB Local** — the real DynamoDB engine as a Docker image (`amazon/dynamodb-local`) listening on :8000. Any
  access key works (it checks nothing), `-inMemory` keeps the data in RAM, and `aaronshaf/dynamodb-admin` on :8001 is a
  web page to look inside. **Endpoint override** — telling the SDK to send its requests to that address
  (`app.dynamodb.endpoint`) instead of AWS; without it, the same code talks to the real table.
* **S3** — file storage (buckets of objects). **CloudFront** — the CDN that serves S3 files fast
  worldwide and handles HTTPS. **OAC (Origin Access Control)** — lets only CloudFront read the bucket.
* **Cognito User Pool** — managed sign-up/sign-in; **Managed Login** — its hosted pages;
  **JWT** — the signed token proving who you are; **PKCE** — the safe login flow for browser apps.
* **ACM** — free TLS certificates. **Route 53** — AWS DNS (we use Hostinger DNS instead).
* **CloudFormation** — AWS's infrastructure-as-code language (a stack = one deployed template).
  **SAM** — a friendlier dialect for serverless apps + a CLI (`sam build/deploy/local`).
* **CloudWatch** — logs, metrics, dashboards, alarms. **SNS** — sends notifications (e-mail).
  **AWS Budgets** — e-mail when spend crosses a threshold. **EMF** — a log format that becomes metrics for free.
* **Infrastructure as Code (IaC)** — your cloud setup is a file in git, not clicks in a console.

* **Geo-restriction** — a CloudFront switch that drops requests from countries not on an allow-list (we allow only the US; a VPN whose exit is abroad counts as abroad — Q27). Blocked visitors get our static `blocked.html`. Free. **AWS Shield Standard** — automatic, free protection against network-level floods on CloudFront and API Gateway.
* **Origin-verify header** — a secret header CloudFront adds when it calls our API; the API rejects anything without it, so the API's own URL is useless to attackers.
* **Kill switch** — our small Lambda that, when an alarm or the $20 budget fires, sets the API's concurrency to 0, turns sign-ups off and disables the CDN. The project's "pull the plug" button.
* **Free plan / credits** — how AWS accounts created after July 2025 work: $100–200 of credits for 6 months and no billing beyond them; then upgrade to the Paid plan or the account closes. Creating an AWS Organization ends the Free plan on the spot (Q32). **Always Free** — usage that stays free on both plans (Lambda 1M requests/month, DynamoDB 25 GB, …).
* **SSM Parameter Store** — a free place in AWS to keep small config values and secrets (our origin-verify secret).
* **Root user** — the e-mail + password that created the account. It can do anything and cannot be limited, so it is used only for MFA, billing settings and the few "root-only" tasks; never for daily work or the CLI.
* **MFA (multi-factor authentication)** — a second proof at sign-in: a six-digit code from an authenticator app on your phone that changes every 30 seconds. A stolen password alone is then useless.
* **IAM user / policy / access key** — an IAM user is a named login inside the account (`bryan`). A policy is a JSON list of allowed actions attached to it (`AdministratorAccess` = everything, `SignInLocalDevelopmentAccess` = may use `aws login`). An access key is a permanent password pair for programs; we never create one.
* **`aws login` / profile / `AWS_PROFILE`** — `aws login` opens the browser, lets you sign in as usual and keeps *temporary* credentials (12 h at most) in `~/.aws/login/cache`. A profile is a named block in `~/.aws/config` (`[profile fh]`) holding the region and the sign-in to use; `export AWS_PROFILE=fh` picks it so you never type `--profile`.
* **AWS Organizations / IAM Identity Center** — Organizations groups several accounts under one management account; Identity Center is the single-sign-on service built on top of it (`aws configure sso`). Creating an Organization ends the Free plan (Q32), so both wait for the Paid-plan upgrade.
* **Budget alert: actual vs forecasted** — *actual* fires when money already spent this month crosses the line; *forecasted* fires when AWS predicts the month will cross it. The forecast needs about five weeks of usage history before it says anything.
* **Budget e-mail verification** — a new address gets a verification e-mail from `@aws.com` and receives nothing until the link is clicked while signed in to the account; once per address per account.
* **CloudFormation: template / stack / parameter** — the template is the YAML file (`infra/budgets.yaml`); a stack is one named deployment of it (`forgotten-heroes-budgets`) that CloudFormation creates, updates or deletes as a unit; a parameter is a value typed at deploy time (`Email=…`) so it stays out of git.
* **Alternate contacts** — three extra e-mail entries (billing, operations, security) on the Account page that AWS can write to besides the root e-mail.
* **Cost allocation tag** — a tag key (`project`) that, once *activated* on the Billing → Cost allocation tags page, becomes a filter in Cost Explorer. It can only be activated after a tagged resource has produced a bill line (Step 34).

## React & web
* **SPA (Single-Page App)** — one HTML file; JavaScript swaps screens. Needs the "404 → index.html" trick on CloudFront.
* **Component** — a function that returns UI. **Props** — inputs. **State** — remembered values.
  **Hook** — a `useSomething` function that gives components powers.
* **Zustand store** — a shared state object components subscribe to.
* **React Router** — maps URLs to screens. **Vite** — the dev server and bundler. **TypeScript** —
  JavaScript with types (catches mistakes early). **ESLint/Prettier** — code rules / formatting.
* **Tailwind** — CSS utility classes (`p-4 text-gold`). **Design tokens** — named colours/sizes used everywhere.
* **dnd-kit** — drag-and-drop with keyboard support. **Motion** — animation library.
  **Howler** — audio library; **audio sprite** — one sound file containing many clips.
* **fetch** — the browser's HTTP call. **CORS** — browser rule about which sites may call an API.
  **ETag** — a version tag so the browser can skip re-downloading unchanged data.
* **MSW (Mock Service Worker)** — fakes the API in tests. **Vitest / Testing Library / Playwright** —
  unit / component / end-to-end test tools. **axe** — accessibility checker. **Lighthouse** — performance audit.
* **ARIA / live region** — attributes that tell screen readers what things are and announce changes.
* **`prefers-reduced-motion`** — a user setting we respect by shortening animations.

## Game & engine
* **Auto-battler** — you build a team; the fight resolves automatically.
* **Deterministic** — same inputs → same outputs, always. **Seed** — the number that makes random
  choices reproducible. **RNG** — random number generator. **`GameRandom`** — the engine's only dice:
  `new GameRandom(seed)` hands out the same numbers in the same order every time (`nextInt`, `chance`, `pick`,
  `shuffled`); it wraps the JDK's `SplittableRandom`.
* **Seed derivation** — computing every seed from one stored run seed instead of drawing new ones: `turnSeed(runSeed, turn)`,
  `battleSeed(runSeed, turn)` (same inputs plus the label "battle"), `rollSeed(turnSeed, rollIndex)`. One number replays a
  whole run; a bug report is "seed 42, turn 5".
* **Fork** — a child generator made from a parent's *seed* plus a label (`turn.fork("battle")`). Because it ignores how many
  numbers the parent already drew, one more random call in the shop never reshuffles the battle.
* **Action** — a player intent (Buy, Roll…), a record in the sealed `Action` family with a `type` in JSON. **Resolver** — validates then applies (`TavernResolver.apply(state, action)` returns a `TavernResult`: events plus the `PendingTrigger`s abilities may react to). **Mutator** — one tiny
  state change that appends its event (`GoldMutator`, `TavernMutator`, `RunMutator`). **Event** — a fact about what happened. **Event log** — the ordered list of events;
  the client animates it. **Event sourcing** — storing/transmitting events rather than only final state.
* **Trigger** — the moment an ability fires (Fall, Hurt…). **Target** — who it affects. **Effect** — what it does.
* **DSL (Domain-Specific Language)** — our small JSON vocabulary for abilities: a **trigger** (when), **effects** (what; each has a `type`), **targets** (who; `select` + `count`), **amounts** (how much; `flat`, `perLevel`, `byLevel`, `percentOfAttack`), an optional **limit** (how often) and **condition** (a multiplier against a rank). Adding a hero is data plus a scenario test; the interpreter (Step 16) never changes.
* **Content** — the three data files `heroes.json`, `tokens.json`, `relics.json` under `engine/src/main/resources/content/`; `ContentLoader.load()` validates them and returns a `ContentIndex` (`hero(id)`, `heroesOfRankUpTo(rank)`, `summary()`). The full roster lands in Step 19.
* **Unit / band / tavern / run state** — the engine's board model (Step 11). A `Unit` keeps base stats, a permanent bonus, a temporary bonus and the damage taken this battle; what you see is derived (`attack()`, `health()`). A `Band` is the ordered list of up to five units, index 0 in front, no gaps. The `Tavern` holds hero and relic slots (`TavernSlot`: item id, price, bonus, frozen), the permanent buff future heroes spawn with, and the level-up bonus slot. `RunState` ties them to gold, hearts, crowns, turn, status and version.
* **Temporary vs permanent stats** — a tavern buff (Ration, XP) is permanent; a battle buff and Battle Brew are temporary and vanish after the battle (`clearTemporary()`). Kept apart so the UI can colour them and the engine can drop one without touching the other.
* **Event catalogue** — the 33 facts the engine can emit (Step 12): run and turn (`RunStarted`, `TurnStarted`, `TurnEnded`, `GoldChanged`), tavern (`TavernRolled`, `TavernItemFrozen`/`Unfrozen`, `TavernBuffed`, `RelicStocked`, `HeroBought`, `RelicBought`, `HeroSold`, `HeroesReordered`, `HeroesMerged`, `XpGained`, `LevelUp`), units (`StatsChanged`, `PerkGained`, `PerkConsumed`, `AbilityTriggered`), battle (`BattleStarted`, `Clash`, `DamageDealt`, `Knockout`, `UnitFell`, `UnitRevived`, `UnitSummoned`, `SummonFailed`, `LineShifted`, `BattleEnded`) and progress (`HeartsChanged`, `CrownsChanged`, `RunEnded`). Each carries `seq`, a `type`, and before/after values where they matter. Unit ids are `"u7"`, positions `"home:0"`, ranks numbers.
* **Start of turn (as implemented)** — `StartTurn` is engine-internal: `RunStarted` on turn 1, then `TurnStarted` (turn, gold, rank, slot counts), `GoldChanged` to 10, the mercy `HeartsChanged` on turn 3 when a heart is missing, the slot tables applied, the bonus slot closed, every unit's per-turn ability counter reset, and the automatic `TavernRolled` (roll index 0, free). After it the player still has one free `Roll`.
* **Roll seed** — the shelves of the n-th roll of a turn come from `rollSeed(turnSeed(runSeed, turn), n)` forked as `"tavern"`; the resolver derives it from the state alone, so the same run seed, turn and roll index always offer the same heroes.
* **Merge rule (as implemented)** — dropping a copy onto a hero (from the shelf for 3 gold, from the band for free) keeps the higher attack and the higher health of the two, adds the copy's XP + 1 (so three copies = 2 XP = level 2, six copies or two level-2s = 5 XP = level 3), and every XP point gained is +1/+1 for good; the survivor keeps its perk (or takes the copy's if it had none) and the higher temporary bonus of each stat. A level-3 hero accepts no more copies (`MAX_LEVEL`).
* **Bonus slot (as implemented)** — on a level-up the tavern opens one extra hero slot (index `heroSlots().size()`) holding a hero of the next rank, drawn with the action's own dice (`actionSeed(turnSeed, lastEventSeq)` forked `"bonus"`); while the roster is incomplete it falls back to every hero up to that rank. It closes when bought, on an unfrozen roll, and at the next start of turn.
* **Action seed** — `SeedDerivation.actionSeed(turnSeed, lastEventSeq)`: dice unique to one action, derived from the state alone (the last event number before the action), used for the bonus slot now and for tavern abilities and relics from Step 15.
* **Relic (as implemented)** — `BuyRelic(slot, bandIndex?)`: pay the shelf price, empty the shelf, `RelicBought`, then every effect of the relic runs through `EffectApplier` with the action's dice forked `"relic"`; a chosen hero's `EAT_RELIC` moment is recorded. Instant relics change stats (Ration permanent, Battle Brew temporary, Feast/Banquet/Grand Feast random distinct heroes), give XP (Tome: +1 XP = +1/+1, level-up opens the bonus slot) or buff the tavern (Royal Decree: current shelves and every future hero); perk relics attach one perk, replacing the old one; Poison Vial makes a hero fall in the tavern (`UnitFell`, removed, `FALL` recorded for Step 16).
* **Targeting (v1)** — `SELF`, `CHOSEN`, `TRIGGERING_UNIT`, `ALL_ALLIES` (living band mates except the firing unit) and `RANDOM_ALLY(count)` (a seeded shuffle, first n, so Feast never picks the same hero twice); the enemy and position selectors arrive with the trigger system.
* **Trigger dispatch (as implemented)** — every tavern action ends with a dispatch: the moments it noted (`BUY`, `ALLY_SUMMONED`, `SELL`, `LEVEL_UP`, `EAT_RELIC`, `START_OF_TURN`) are fired in order; for each, the candidates are the unit itself, its living band mates, or everybody, depending on the trigger (GDD §7); they are filtered by the ability's limit (per turn / per battle, counted in `Counters`), sorted by the ordering rule, announced by `AbilityTriggered` and run through `AbilityInterpreter → EffectApplier`. Moments an effect causes are queued.
* **Settle pass / fall processing** — after every moment, units at zero health are processed highest attack first: `UnitFell`, the unit's own Fall ability (it still holds its place, so "nearest allies behind" works), Fairy Charm's Sprite and Phoenix Feather's return, the friends' `ALLY_FALL` / `ALLY_AHEAD_FALLS` abilities, then removal and the deferred summons landing in the slot (`UnitSummoned`, then `ALLY_SUMMONED` and `SUMMONED` moments). Falls caused along the way are picked up by the next pass, so Alchemist's poisoned fall can take an ally with it and that ally's Fall still fires.
* **Tavern damage** — ability damage outside a battle is lost for good (`Unit.reduceMaxHealth`: bonus health first, then base), because there is no battle to heal from; Poison Vial simply drops a hero to zero.
* **Battle (as implemented)** — `BattleResolver.resolve(home, away, seed, turn, playerSide)` copies both bands into a `BattleState` (a `Board` with two sides and ids `s1, s2 …` for summons), emits `BattleStarted` with both line-ups, fires `START_OF_BATTLE`, then loops: `Clash`, `BEFORE_ATTACK` for both fronts, simultaneous `DamageDealt` ×2, `HURT` and `Knockout`/`KNOCKOUT` moments, the dispatcher's settle pass (falls, Fall abilities, summons), `LineShifted` per side that changed, then `ALLY_AHEAD_ATTACKS` and `AFTER_ATTACK` for surviving attackers. Ends with `BattleEnded(homeOutcome, playerOutcome, reason, clashes)`; `BattleResult.playerOutcome()` flips the outcome when the player fought as the away side.
* **Perks in battle (as implemented)** — Iron Blade +3 on attacks; Enchanted Blade +20 on the first attack, then spent; Aegis ignores the first damaging hit entirely, then spent; Ward takes 20 off the first damaging hit, then spent; Chainmail takes 2 off every hit, never below 1; Venom makes any damaging attack lethal (the `DamageDealt` amount is the victim's whole health); Flame Oil also hits the enemy behind the target for 5 as a `SPLASH`, through that unit's own perks, and can knock it out; Fairy Charm leaves a 1/1 Sprite in the slot; Phoenix Feather brings the unit back as a 1/1 once (`PerkConsumed`, `UnitRevived`), and friends' Ally-fall abilities still fire because it did fall. A fallen unit still answers its own `HURT` (a lethal hit is still a hit).
* **End of turn (as implemented)** — `GameSession.endTurn(state, ghost)`: `END_OF_TURN` abilities → `TurnEnded` → the band is snapshotted as this player's ghost → the battle against the given ghost with `battleSeed(runSeed, turn)` (the player is home on odd turns, away on even ones, `BattleStarted.playerSide` says which) → `CrownsChanged` on a win, `HeartsChanged` on a loss, nothing on a draw → `lastBattle` remembered, temporary stats cleared → `RunEnded` at ten crowns or zero hearts, otherwise `StartTurn` for the next turn. Everything comes back as one contiguous event list plus the battle on its own.
* **Ghost / `BandSnapshot`** — a band as other players fight it: for each unit the hero id, the attack and health it fought with (bonuses included), its XP and its perk; no damage, no counters, no run internals. `BandSnapshot.EMPTY` is a legal opponent that loses at once; a mirror ghost is `state.toSnapshot()`.
* **Token** — a unit only an ability can create (Scarecrow, Sprite, Wolf…); never for sale.
* **Ghost** — a saved copy of another player's band used as an opponent. **Matchmaking** — picking it.
* **Lambdalith** — one Lambda running a whole (small) app.
* **Simulator (`sim` module)** — `scripts/sim.sh run --games N --bot greedy --seed S` plays N games against a ghost pool (random-bot bands first, then every finished run adds its own) and writes `target/sim/report.json` + `.md`: run length, runs won, battle win rate, greedy-vs-random, hero pick rate and win rate on band, bands per turn, battle-length and run-length histograms, clash-cap draws, gold per economy hero. `check` compares a report with `sim/thresholds.yaml` (GDD §9) and exits 1 on a miss; `fuzz` hunts crashes; `replay` plays a seed and an action list and prints the story. The first report is `docs/balance/2026-10-report.md`.
* **Simulator / bot** — code that plays the game thousands of times to measure balance. A `Bot` maps the state to the next action (`EndTurn` included); `RandomBot` is the floor (any action, ends the turn one time in five once gold is low), `GreedyBot` the baseline (merge copies, buy the strongest affordable hero, relic on the best fighter, toughest hero in front, roll when nothing is worth buying). `BotRunner.playRun(bot, seed, ghosts)` drives a whole run through `GameSession` and returns a `RunSummary` (status, turns, crowns, hearts, actions, refusals, every battle with the band that fought it, gold earned per hero). `GhostPool` files bands per turn so bots fight bots, like production's ghost table.
* **Scenario test** — a test written as data: starting board, what happens, expected events. Ours: a battle file has `home`/`away` unit lists (`hero`, optional `attack`/`health`/`level`/`xp`/`perk`), `expectEvents`, `expectCounts` and `expectResult`; a tavern file has `turn`, `gold`, `band`, `tavern.heroes`/`relics`, `actions` (any `Action` as JSON, plus `EndOfTurn`) and `expectState` (gold, hearts, bonus slot, tavern buff, relic slots, the band's heroes/stats/levels/perks). Expected events name units by position (`"unit": "home:1"`, `"target": "away:0"`), stat changes as `"attack": "1→2"`, summons by `token` and `stats`.

* **Elements palette** — FinalBossBlues' newer, deeper colour set; all our art is converted into it so sprites match.
* **Cameo** — a hero named after a character from *The Forgotten Kanji* (Aoi, Hotaru, Iwao, Kashi, Kaede, Nagi).

## Operations
* **Environment** — a complete copy of the system (`dev`, `prod`). **Deploy** — put a version live.
  **Rollback** — go back to the previous version. **Smoke test** — a quick "is it alive?" check after deploy.
* **Runbook** — what to do when an alarm fires. **Observability** — logs + metrics + traces so you can see inside.
* **Idempotent** — doing it twice has the same effect as once (safe retries).
* **Optimistic locking** — assume no conflict; detect it with a version number; retry if wrong.
