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

TODO Step 22: `./mvnw -pl sim exec:java -Dexec.args="run --games 1000"` or the shortcut
`scripts/sim.sh run --games 1000`.
