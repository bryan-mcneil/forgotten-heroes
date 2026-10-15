# sim — the balance simulator (Java command line)

## What lives here

- A command-line tool that plays thousands of runs with bots (`RandomBot`, `GreedyBot`, Step 21) through
  the engine and writes a balance report in Markdown and JSON (Step 22).
- Four sub-commands: `run` (play N games), `fuzz` (throw random legal and illegal actions at
  `GameSession`; any exception other than a rule violation is a bug), `replay` (reproduce a seed and an
  action list) and `check` (fail when a balance target from the GDD §9 is missed).
- `thresholds.yaml`, the balance targets as numbers (Step 70 tunes them; a nightly job runs `check`).

## What must NOT live here

- Rules. The simulator only calls the engine; it never decides what is legal.
- Spring, databases or network access. It runs offline from the terminal and inside CI.
- Generated reports. Build output is not committed; the reports worth keeping go to `docs/balance/`.

## How do I run it

From the repo root (the script compiles `engine` and `sim` first; the Maven wrapper needs no global Maven):

```bash
scripts/sim.sh run --games 1000 --bot greedy --seed 1    # report in target/sim/report.json and report.md
scripts/sim.sh check                                     # compares the report with sim/thresholds.yaml; exit 1 on a miss
scripts/sim.sh fuzz --actions 200000 --seed 7            # random actions; any exception but a RuleViolation is a bug
scripts/sim.sh replay --file bug.json                    # {"seed": …, "actions": […]}: the file fuzz prints
./mvnw -pl sim -am -B -ntp verify                        # the module's own tests (8) and its 70 % coverage gate
```

`run` fills a ghost pool with random-bot bands, then plays the measured bot against the pool, every finished run
adding its own bands, so bots fight bands of the same age; with the greedy bot it also plays 200 games against
random-only ghosts for the GDD §9 "greedy vs random" number. Reports worth keeping are copied to `docs/balance/`.
