# Phase 0 — Foundations (Steps 01–06)

**Where you are:** nothing exists yet except research documents. **At the end of this phase:** your
machine is verified, a public GitHub repo exists with CI and a project board holding all 77 steps,
and your AWS account is safe and cannot surprise you with a bill.

Every step follows the same template: *Goal → You will have → Sensei notes (What/Why/How/Where) →
Do this → Verify → Review checklist → Commit → Check your understanding.* Commands are written for
**Git Bash** (your preferred shell, Q18). Make it VS Code's default once: *Terminal → Select Default
Profile → Git Bash*. Windows-only installers (`winget`) run fine from Git Bash too.

---

## Step 01 — Toolchain
**Branch:** `step-01-toolchain` · **est 1h** · depends on: nothing

> ✅ **Already done on 7 Oct 2026 (planning session):** Corretto 25.0.4, AWS CLI 2.37, SAM CLI 1.167,
> ffmpeg 9.0 and the nine VS Code extensions were installed with `winget` / `code`. `JAVA_HOME` was set by
> the Corretto installer and Corretto's `bin` sits before Oracle's in `PATH`. Maven is **not** installed and
> is not needed (see Step 07). This step is therefore: verify in a fresh terminal, write the check script,
> write the setup guide.

**Goal:** prove the machine has the exact tools the whole plan assumes, with a script you can re-run any day.

**You will have:** `scripts/check-env.sh` printing a green table; `docs/setup.md`.

**Sensei notes**
- *What:* a toolchain = the programs that turn source into running software (JDK compiles Java,
  Node runs the frontend tooling, AWS/SAM CLIs talk to the cloud, Docker runs throwaway databases,
  ffmpeg converts audio).
- *Why:* "works on my machine" problems are 90 % version drift. We pin **Java 25** because AWS
  Lambda runs Java 25; your Java 26 would compile code Lambda cannot run. A check script turns
  "I think it's set up" into "it is set up".
- *How:* each tool prints its version; the script compares it with a minimum and exits non-zero on
  any miss, so CI and you see the same table.
- *Where:* `scripts/`, `docs/setup.md`. (`knowledge/environment_audit.md` is the before/after record.)

**Do this**
1. Open a **new** Git Bash terminal (PATH changes only reach new windows) and confirm:
   ```bash
   java -version          # OpenJDK ... Corretto-25...
   echo "$JAVA_HOME"      # C:\Program Files\Amazon Corretto\jdk25.0.4_10
   aws --version && sam --version && ffmpeg -version | head -1
   node -v && npm -v && gh auth status && docker info | grep "Server Version"
   ```
   If `java -version` still says 26, Oracle's `javapath` is earlier in your PATH than Corretto —
   fix the order in *Edit the system environment variables* and open a new terminal.
2. For a fresh machine (recorded for your future self, not needed now):
   ```bash
   winget install --id Amazon.Corretto.25.JDK -e
   winget install --id Amazon.AWSCLI -e
   winget install --id Amazon.SAM-CLI -e
   winget install --id Gyan.FFmpeg -e
   code --install-extension vscjava.vscode-java-pack   # … and the other eight listed in environment_audit.md
   ```
3. Start Docker Desktop and leave it running whenever you work on the backend.
4. Create `scripts/check-env.sh`: for each tool run `<tool> --version`, extract the number with
   `grep -oE`, compare with the minimum, print a table (Java must contain "Corretto" and start with
   25; Docker must report `Server Version`; `gh auth status` must say logged in). `exit 1` if anything
   fails. Make it executable: `chmod +x scripts/check-env.sh` (Git records the bit).
5. Write `docs/setup.md`: the list above, in order, plus "how to switch Java" and "Docker must be running".

**Verify**
```bash
./scripts/check-env.sh    # all rows ✔
echo $?                   # 0
```

**Review checklist:** [ ] check script fails loudly if Docker is stopped (quit Docker, run it, see ✘) ·
[ ] `docs/setup.md` reproducible on a fresh machine · [ ] no tool pinned to a non-LTS version.

**Commit:** `step-01: toolchain check script and setup guide`

**Check your understanding:** Why does `JAVA_HOME` matter if `java` is already on the PATH? What
would break if local Java were 26 and Lambda 25?

---

## Step 02 — Create the repository
**Branch:** work directly on `main` for this one step (the repo does not exist yet) · **est 1h**

**Goal:** a public GitHub repository `bryan-mcneil/forgotten-heroes` containing the knowledge files and sane defaults.

**You will have:** `README.md`, `LICENSE` (MIT, code only), `.gitignore`, `.editorconfig`,
`.gitattributes`, `CREDITS.md` stub; the `Super Auto Pets/` reference copy **ignored by git** (it stays
on disk for research and is deleted in Step 77).

**Sensei notes**
- *What:* Git tracks text; a repo is a folder with history. GitHub hosts it and runs CI.
- *Why public:* recruiters. *Why ignore the Super Auto Pets folder:* it is 473 MB of someone else's
  copyrighted binaries and must never be committed. It is a copy of your real install kept only as
  reference, so ignoring it now and deleting it at launch is enough. The `.gitignore` entry goes in
  **before** `git init`, so there is never a moment where `git add -A` could pick it up.
- *How:* `git init`, `gh repo create --source`. `.gitattributes` forces LF line endings so Windows
  and CI agree; `.editorconfig` tells every editor the indentation rules.
- *Where:* repo root.

**Do this**
1. Create `.gitignore` **first**, with this as its first entry (the trailing slash means "directory"):
   ```
   # Reference copy of a commercial game — never commit (deleted in Step 77)
   Super Auto Pets/
   ```
2. In the project folder: `git init -b main`, then prove the ignore works:
   `git check-ignore -v "Super Auto Pets"` prints the rule; `git status --short | grep -ci "super auto"` prints `0`.
3. Extend `.gitignore` to cover Java (`target/`), Node (`node_modules/`, `dist/`), Python
   (`__pycache__/`, `.venv/`), SAM (`.aws-sam/`), IDE folders, `*.log`, **and** the asset
   outputs: `frontend/public/assets/`, `tools/assets/build/`, `tools/assets/raw/`.
4. `.gitattributes`:
   ```
   * text=auto eol=lf
   *.cmd text eol=crlf
   *.ps1 text eol=crlf
   *.png binary
   *.ogg binary
   *.m4a binary
   ```
5. `.editorconfig`: 2 spaces for JSON/YAML/TS/MD, 4 for Java, LF, final newline, trim whitespace.
6. `LICENSE`: MIT with your name; add a line in `README.md`: "Code is MIT. Art and audio are
   proprietary (Time Fantasy by FinalBossBlues and the author's own) and are not included."
7. `README.md` (short for now): title, one-paragraph pitch, link to `PLAN.md` and `knowledge/`.
8. `git add -A && git status --short | grep -ci "super auto"` must print `0`; then
   `git commit -m "step-02: repository bootstrap"` and
   `gh repo create forgotten-heroes --public --source=. --remote=origin --push --description "Auto-battler built with Java/Spring Boot on AWS Lambda and React"`.

**Verify:** `gh repo view --web` opens the repo; `git status` clean; `git ls-files | grep -ci "super auto"` prints `0`;
`git check-attr eol -- mvnw.cmd` (later) shows crlf.

**Review checklist:** [ ] `git ls-files | grep -i "super auto"` prints nothing (folder ignored, not committed) · [ ] `.gitignore` includes asset
output dirs · [ ] LICENSE scope sentence present.

**Commit:** `step-02: repository bootstrap`

**Check your understanding:** Why do we force LF line endings but keep `.cmd`/`.ps1` as CRLF?

---

## Step 03 — Monorepo skeleton
**Branch:** `step-03-skeleton` · **est 1h**

**Goal:** the folder structure from `knowledge/architecture.md` §2, each with a README that says
what belongs there and what does not.

**You will have:** `engine/ backend/ sim/ frontend/ infra/ tools/ studio/ docs/ knowledge/ plan/ scripts/ .github/`
with `README.md` stubs; `project_goal.md` moved to `docs/project_goal.md`.

**Sensei notes**
- *What:* a monorepo = several projects in one repository with one history.
- *Why:* one PR per step can touch engine + backend + docs together; recruiters see one place.
- *How:* folders + READMEs now, build files later (Maven in Step 07, npm in Step 37). Empty folders
  are not tracked by Git, hence the READMEs.
- *Where:* repo root.

**Do this:** create the folders; each README answers three questions: *What lives here? What must
NOT live here? How do I run it?* (leave "how to run" as TODO with the step number that fills it).
Add a root `docs/README.md` index linking knowledge files.

**Verify:** `find . -maxdepth 2 -not -path './.git*' -not -path './Super Auto Pets*' | sort` shows the structure; every folder has a README.

**Commit:** `step-03: monorepo skeleton with folder READMEs`

**Check your understanding:** Which folder would a new hero's JSON go into, and which folder would its sprite frames go into?

---

## Step 04 — Project management on GitHub
**Branch:** `step-04-project-management` · **est 1.5h**

**Goal:** make the plan visible and trackable on GitHub.

**You will have:** labels, 7 milestones, issue templates (`step.yml`, `bug.yml`, `idea.yml`),
`pull_request_template.md`, a Project board, and **77 issues** created from `PLAN.md` by a script.

**Sensei notes**
- *What:* Issues = units of work; Milestones = phases; Projects = a kanban board; templates make
  every issue/PR look the same.
- *Why:* you asked for a PM system and for every step to be reviewable. A board you can see beats
  a plan you have to remember.
- *How:* the `gh` CLI can create everything from a script, so the plan file stays the source of truth.
  You asked to be walked through this (Q19), so we do it in two passes: **by hand once** (one label, one
  milestone, one issue — you type the commands and read what comes back), **then the script** creates the
  other 76. Every `gh` command is just an HTTP call to GitHub's API; `gh api` shows you the raw version.
- *Where:* `.github/`, `scripts/create-issues.sh`.

**Do this**
1. `gh auth refresh -s project,read:project` (grants the board scope once).
   **By hand first:** `gh label create "phase:0" --color 0E8A16 --description "Phase 0 — Foundations"`, then
   `gh api repos/:owner/:repo/milestones -f title="Phase 0 — Foundations"`, then
   `gh issue create --title "Step 01 — Toolchain" --label "phase:0" --body "See plan/phase_0_foundations.md#step-01"`.
   Open the repo in the browser and find all three. Now you know exactly what the script automates.
2. Labels: `scripts/create-labels.sh` runs `gh label create` for `phase:0..6`, `area:*`, `type:*`, `good-first-step`, `blocked` (colours of your choice).
3. Milestones: `gh api repos/:owner/:repo/milestones -f title="Phase 0 — Foundations"` … for all 7.
4. Templates: copy the PR template from `knowledge/project_management.md` §4; create YAML issue
   forms with fields *Step number, Goal, Verify commands* (step), *Seed, Turn, Actions, Expected, Actual* (bug).
5. Board: `gh project create --owner bryan-mcneil --title "Forgotten Heroes"`; add columns
   Backlog / Next / In progress / In review / Done (the CLI creates a default Status field; rename options in the UI).
6. `scripts/create-issues.sh`: parse lines matching `- [ ] **NN** …` in `PLAN.md`, create one issue
   per step with title `Step NN — <text before " — est">`, body linking to the phase file anchor,
   labels `type:step`, `phase:N`, milestone by phase; then `gh project item-add`. Make it
   **idempotent** (skip if an issue with the same title exists) so re-running is safe.
7. Run it. Move Steps 01–06 to "Next" and close 01–03 as done (they already are).

**Verify:** `gh issue list --limit 100 | wc -l` prints 77; the board shows all cards; opening an issue shows the right labels/milestone.

**Commit:** `step-04: labels, milestones, templates, board and issue generator`

**Check your understanding:** Why make the issue script idempotent? What happens if you add a step 78 to `PLAN.md` later?

---

## Step 05 — CI skeleton & branch protection
**Branch:** `step-05-ci-skeleton` · **est 1h**

**Goal:** every PR runs a workflow, and `main` cannot be changed without a green check.

**You will have:** `.github/workflows/ci.yml` with a `docs` job (markdown lint + link check),
`.github/dependabot.yml`, branch protection requiring the `docs` check and PR reviews disabled
(solo project) but **squash-only merges** enabled.

**Sensei notes**
- *What:* GitHub Actions runs jobs on GitHub's computers when events happen (PR opened, push).
- *Why:* the robot never forgets to run tests. Starting with a trivial job teaches the mechanics
  before real tests exist; Steps 23, 36, 56, 63 add jobs.
- *How:* YAML workflow; `on: pull_request` + `push: branches: [main]`; `gh api` to set protection.
- *Where:* `.github/workflows/`, repo Settings → Branches.

**Do this**
1. `ci.yml`: job `docs` on `ubuntu-latest`: checkout → `npx markdownlint-cli2 "**/*.md" "#node_modules"` → `npx markdown-link-check` on `README.md` and `PLAN.md` (allow-list external domains to keep it fast).
2. `.markdownlint.yaml`: relax line length (MD013 off) and allow inline HTML.
3. `dependabot.yml`: `github-actions` weekly (Maven/npm ecosystems are added in Steps 36/37).
4. Repo settings: allow **squash merge only**; auto-delete head branches; branch protection on
   `main`: require status check `docs`, require linear history, disallow force pushes. Via CLI:
   `gh api -X PUT repos/:owner/:repo/branches/main/protection --input infra/github/branch-protection.json`.
5. Open the PR for this step — it is the first PR that must pass its own check.

**Verify:** the PR shows the `docs` check green; trying `git push origin main` directly is rejected.

**Commit:** `step-05: CI skeleton, Dependabot and branch protection`

**Check your understanding:** What is the difference between a *workflow*, a *job* and a *step* in Actions?

---

## Step 06 — AWS account hardening and budgets
**Branch:** `step-06-aws-account` · **est 1.5h** · ⚠️ do not skip; this is what makes "$0" safe

**Goal:** secure the account, get CLI access without long-lived keys, and install the spending alarms.

**You will have:** root MFA on; IAM Identity Center user with `AdministratorAccess` for you;
`aws configure sso` profile `fh`; `infra/budgets.yaml` deployed (budgets $5 and $20 with e-mail
alerts at 50/80/100 % and forecast); `docs/aws-account-setup.md` including your **Free plan** credits
balance and expiry date.

**Sensei notes**
- *What:* the **root user** owns the account (use it only for billing/MFA). **IAM Identity Center**
  gives you a personal login with short-lived credentials for the CLI. **Budgets** e-mail you.
- *Why:* leaked long-lived keys are the #1 cause of surprise bills on hobby accounts; budgets are the
  last line of defence. Both are free.
- *How:* console for MFA + Identity Center; budgets **by hand in the console first** (Q31), then
  CloudFormation recreates them (so even the alarm is code).
- *Where:* AWS console (once), `infra/budgets.yaml`, `~/.aws/config`.

**Do this**
0. Billing console → **Free Tier** page. Your account was created in Aug/Sep 2026, so it is on the
   credit-based **Free plan** (Q1). Write down: plan type, credits remaining, **expiry date** (6 months
   after creation ≈ Feb/Mar 2027). Put a calendar reminder two weeks before that date: *upgrade to the
   Paid plan* (decided, Q26; `knowledge/cost_estimator.md` §1 explains why). Until then AWS cannot bill
   you beyond the credits — a free safety net while you build.
1. Console → root user → Security credentials → **enable MFA** (authenticator app). Also set
   Billing → "IAM user and role access to Billing" = activate, and alternate contacts.
2. **IAM Identity Center** (us-east-1): Enable → create user `bryan` (your e-mail) → create
   permission set `AdministratorAccess` → assign user to this account. Note the **start URL**.
3. CLI: `aws configure sso` → profile name `fh`, SSO start URL, region `us-east-1`, output `json`.
   Then `echo 'export AWS_PROFILE=fh' >> ~/.bashrc && source ~/.bashrc` and `aws sso login`.
4. **Console first (Q31):** Billing → **Budgets** → Create budget → Customize → Cost budget → monthly, `$5`,
   name it `manual-5`, alerts at 50 / 80 / 100 % actual + 100 % forecasted → e-mail <your-email>;
   repeat for `$20` as `manual-20`. (The click-by-click walk-through is written when you reach this step —
   the console changes too often to write it now.) Then make it code — `infra/budgets.yaml`:
   `AWS::Budgets::Budget` ×2 (monthly cost, `$5` and `$20`) with
   `NotificationsWithSubscribers` (ACTUAL 50 %, 80 %, 100 %; FORECASTED 100 %) to your e-mail.
   Deploy: `aws cloudformation deploy --stack-name forgotten-heroes-budgets --template-file infra/budgets.yaml --parameter-overrides Email=<your-email>`.
   Confirm the SNS/Budgets subscription e-mails, then **delete `manual-5` and `manual-20`** so the template
   is the single source of truth.
5. Billing → Cost allocation tags: note that `project` must be activated **after** the first tagged
   resource exists (Step 34 reminder).
6. Write `docs/aws-account-setup.md` with screenshots-free, numbered instructions and the "which
   free tier am I on" facts from item 0 (plan, credits, expiry, reminder set).

**Verify:** `aws sts get-caller-identity` shows your account and an `AWSReservedSSO_…` ARN;
`aws budgets describe-budgets --account-id <id>` lists two budgets; you received the confirmation e-mail.

**Review checklist:** [ ] root has MFA · [ ] no IAM access keys exist (`aws iam list-access-keys` empty for any user) · [ ] budgets e-mail confirmed · [ ] profile `fh` region is us-east-1.

**Commit:** `step-06: budgets as code and AWS account setup guide`

**Check your understanding:** Why is a *forecasted* budget alert useful in addition to *actual*? Why
is it fine that the Identity Center permission set is AdministratorAccess here but not for the CI robot later (Step 62)?

---

## Phase 0 review (do this before Phase 1)
- [ ] `scripts/check-env.sh` green on your machine.
- [ ] Repo public, board shows 77 cards, 6 done.
- [ ] CI check required on `main`.
- [ ] Budgets exist; `aws sts get-caller-identity` works.
- [ ] `docs/reviews/phase-0.md`: what went well, what to change; sign and date. Tag `phase-0-complete`.
