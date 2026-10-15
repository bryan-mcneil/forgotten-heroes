#!/usr/bin/env bash
# The balance simulator from the repo root. Examples:
#   scripts/sim.sh run --games 1000 --bot greedy --seed 1       # report in target/sim/report.{json,md}
#   scripts/sim.sh check                                        # exit 1 when a GDD §9 target is missed
#   scripts/sim.sh fuzz --actions 200000 --seed 7               # any exception but a RuleViolation is a bug
#   scripts/sim.sh replay --file bug.json                       # watch a reported bug line by line
# Compiles engine + sim first (quietly); the Maven wrapper needs no global Maven.
set -euo pipefail
cd "$(dirname "$0")/.."
# exec.args is one string that Maven splits on spaces; quoting each argument keeps "a title with spaces" together.
args=""
for arg in "$@"; do
  args+=" \"${arg//\"/\\\"}\""
done
./mvnw -q -B -ntp -pl sim -am -DskipTests -Djacoco.skip=true compile exec:java -Dexec.args="${args# }"
