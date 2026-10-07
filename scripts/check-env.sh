#!/usr/bin/env bash
# scripts/check-env.sh — prove this machine has the toolchain the whole plan assumes.
#
# Usage:  ./scripts/check-env.sh
# Exit:   0 when every row is ✔, 1 when at least one row is ✘ (so CI and you see the same table).
#
# How it works: for each tool we run "<tool> --version", pull the number out with grep -oE,
# compare it with the minimum, and print a row. Nothing here installs anything; docs/setup.md
# explains how to fix every ✘.

set -u   # treat an unset variable as a bug. (Not -e: a missing tool must print ✘, not abort.)

# ---- colours, but only when printing to a terminal, so CI logs stay plain text ---------------
if [ -t 1 ]; then
  GREEN=$'\e[32m'; RED=$'\e[31m'; BOLD=$'\e[1m'; RESET=$'\e[0m'
else
  GREEN=''; RED=''; BOLD=''; RESET=''
fi
OK="${GREEN}✔${RESET}"
BAD="${RED}✘${RESET}"

FAILURES=0
HINTS=""

# ---- helpers ---------------------------------------------------------------------------------

# have CMD → true when CMD can be found on the PATH.
have() { command -v "$1" >/dev/null 2>&1; }

# version_ge FOUND MIN → true when FOUND >= MIN. "sort -V" compares version numbers the way
# humans do (2.37.10 > 2.20), unlike plain text sorting (where "2.20" > "2.37.10").
version_ge() { [ "$(printf '%s\n%s\n' "$2" "$1" | sort -V | head -n1)" = "$2" ]; }

# semver TEXT → the first thing in TEXT that looks like 1.2 or 1.2.3
semver() { grep -oE '[0-9]+\.[0-9]+(\.[0-9]+)?' | head -n1; }

# row NAME FOUND REQUIRED ok|bad [HINT] → prints one table row; a bad row is counted and its hint kept.
row() {
  local name=$1 found=$2 required=$3 status=$4 hint=${5:-}
  if [ "$status" = ok ]; then
    printf '%-10s %-30s %-16s %s\n' "$name" "$found" "$required" "$OK"
  else
    printf '%-10s %-30s %-16s %s\n' "$name" "$found" "$required" "$BAD"
    FAILURES=$((FAILURES + 1))
    [ -n "$hint" ] && HINTS="${HINTS}  ${name}: ${hint}"$'\n'
  fi
}

# ---- the checks ------------------------------------------------------------------------------

check_java() {
  local out build major
  if ! have java; then
    row java "not found" "Corretto 25" bad "install Amazon Corretto 25 (docs/setup.md)"; return
  fi
  out=$(java -version 2>&1)                               # java prints its version on stderr
  major=$(printf '%s\n' "$out" | grep -oE 'version "[0-9]+' | grep -oE '[0-9]+')
  build=$(printf '%s\n' "$out" | grep -oE 'Corretto-[0-9.]+' | head -n1 | cut -d- -f2)
  if [ -n "$build" ] && [ "$major" = 25 ]; then
    row java "Corretto $build" "Corretto 25" ok
  else
    row java "$(printf '%s\n' "$out" | head -n1 | grep -oE '"[^"]+"' | tr -d '"') (not Corretto 25)" \
        "Corretto 25" bad "Corretto's bin must come before Oracle's javapath in PATH (docs/setup.md → Switching Java)"
  fi
}

check_java_home() {
  local major folder
  if [ -z "${JAVA_HOME:-}" ]; then
    row JAVA_HOME "unset" "Corretto 25 dir" bad "set JAVA_HOME to the Corretto 25 folder (docs/setup.md)"; return
  fi
  major=$("$JAVA_HOME/bin/java" -version 2>&1 | grep -oE 'version "[0-9]+' | grep -oE '[0-9]+')
  folder=${JAVA_HOME//\\//}                               # C:\a\b → C:/a/b, so the next line works on Windows too
  folder=${folder##*/}                                    # keep only the last folder name
  if [ "$major" = 25 ]; then
    row JAVA_HOME "$folder (Java $major)" "Corretto 25 dir" ok
  else
    row JAVA_HOME "$folder (Java ${major:-?})" "Corretto 25 dir" bad "JAVA_HOME points at Java ${major:-?}, not 25: $JAVA_HOME (docs/setup.md)"
  fi
}

check_node() {
  local v
  if ! have node; then row node "not found" ">=22" bad "install Node.js LTS (docs/setup.md)"; return; fi
  v=$(node -v | semver)
  if version_ge "$v" 22; then row node "$v" ">=22" ok; else row node "$v" ">=22" bad "upgrade Node.js to 22 or newer"; fi
}

check_npm() {
  if ! have npm; then row npm "not found" "any" bad "npm ships with Node.js; reinstall Node"; return; fi
  row npm "$(npm -v | semver)" "any" ok
}

check_docker() {
  local server
  if ! have docker; then row docker "not found" "running" bad "install Docker Desktop (docs/setup.md)"; return; fi
  server=$(docker info --format '{{.ServerVersion}}' 2>/dev/null)   # empty when the daemon is stopped
  if [ -n "$server" ]; then
    row docker "$server (running)" "running" ok
  else
    row docker "installed, daemon stopped" "running" bad "start Docker Desktop and wait for the whale icon to settle"
  fi
}

check_aws() {
  local v
  if ! have aws; then row aws "not found" ">=2.20" bad "install the AWS CLI v2 (docs/setup.md)"; return; fi
  v=$(aws --version 2>&1 | grep -oE 'aws-cli/[0-9.]+' | cut -d/ -f2)
  if version_ge "$v" 2.20; then row aws "$v" ">=2.20" ok; else row aws "$v" ">=2.20" bad "upgrade the AWS CLI"; fi
}

check_sam() {
  local v
  if have sam; then
    v=$(sam --version 2>&1 | semver)
    if [ -z "$v" ]; then
      row sam "found, but 'sam --version' failed" ">=1.130" bad "run 'sam --version' and read the error (docs/setup.md → SAM CLI in Git Bash)"
    elif version_ge "$v" 1.130; then
      row sam "$v" ">=1.130" ok
    else
      row sam "$v" ">=1.130" bad "upgrade the SAM CLI"
    fi
  elif have sam.cmd; then
    # Windows: the installer ships only sam.cmd, and Git Bash does not find a .cmd by its bare name.
    row sam "sam.cmd only" ">=1.130" bad "Git Bash can't run bare 'sam' — create the ~/bin/sam shim (docs/setup.md → SAM CLI in Git Bash)"
  else
    row sam "not found" ">=1.130" bad "install the AWS SAM CLI (docs/setup.md)"
  fi
}

check_ffmpeg() {
  if ! have ffmpeg; then row ffmpeg "not found" "any" bad "install ffmpeg (docs/setup.md)"; return; fi
  row ffmpeg "$(ffmpeg -version 2>&1 | head -n1 | semver)" "any" ok
}

check_python() {
  local py="" v pillow
  for candidate in python python3; do                     # Windows installs "python", Linux/macOS "python3"
    if have "$candidate" && "$candidate" --version >/dev/null 2>&1; then py=$candidate; break; fi
  done
  if [ -z "$py" ]; then row python "not found" ">=3.11 + Pillow" bad "install Python 3.11+ (docs/setup.md)"; return; fi
  v=$("$py" --version 2>&1 | semver)
  pillow=$("$py" -c 'import PIL; print(PIL.__version__)' 2>/dev/null)
  if ! version_ge "$v" 3.11; then
    row python "$v" ">=3.11 + Pillow" bad "upgrade Python to 3.11 or newer"
  elif [ -z "$pillow" ]; then
    row python "$v, no Pillow" ">=3.11 + Pillow" bad "$py -m pip install Pillow"
  else
    row python "$v + Pillow $pillow" ">=3.11 + Pillow" ok
  fi
}

check_gh() {
  local v account
  if ! have gh; then row gh "not found" "logged in" bad "install GitHub CLI, then gh auth login"; return; fi
  v=$(gh --version 2>&1 | head -n1 | semver)
  if gh auth status >/dev/null 2>&1; then                  # exit 0 only when a login exists
    account=$(gh auth status 2>&1 | grep -oE 'account [A-Za-z0-9-]+' | head -n1 | cut -d' ' -f2)
    row gh "$v (${account:-logged in})" "logged in" ok
  else
    row gh "$v (not logged in)" "logged in" bad "run: gh auth login"
  fi
}

check_git() {
  if ! have git; then row git "not found" "any" bad "install Git for Windows"; return; fi
  row git "$(git --version | semver)" "any" ok
}

# ---- run everything and print the table ------------------------------------------------------

printf '%s%-10s %-30s %-16s %s%s\n' "$BOLD" "Tool" "Found" "Required" "OK" "$RESET"
check_java
check_java_home
check_node
check_npm
check_docker
check_aws
check_sam
check_ffmpeg
check_python
check_gh
check_git

echo
if [ "$FAILURES" -eq 0 ]; then
  echo "${GREEN}Toolchain OK${RESET} — every row is ✔."
  exit 0
else
  echo "${RED}${FAILURES} check(s) failed.${RESET} How to fix:"
  printf '%s' "$HINTS"
  exit 1
fi
