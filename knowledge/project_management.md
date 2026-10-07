# Project Management — how we run this build

> You asked for a product-management system and for every step to be reviewable and committable.
> This is that system. It is deliberately **boring and small**: GitHub Issues + Projects + PRs.

---

## 1. Where things live

| Thing | Place |
|---|---|
| The plan | `PLAN.md` (index + status) and `plan/phase_*.md` (details) |
| Work tracking | GitHub **Project** board "Forgotten Heroes" (Backlog → Next → In progress → In review → Done) |
| One unit of work | one GitHub **Issue** per step (title `Step NN — <title>`), labels `phase:N`, `area:*`, `type:step` |
| Bugs/ideas | Issues with `type:bug` / `type:idea`, using the templates in `.github/ISSUE_TEMPLATE/` |
| Milestones | one per phase (Phase 0 … Phase 6) |
| Decisions | `docs/adr/NNNN-title.md` (short; copied from `knowledge/architecture.md` ADR table as they are confirmed) |
| Learning journal | `docs/journal.md` — one paragraph per session in your words |
| Reviews | `docs/reviews/phase-N.md` |

Step 04 creates the board, labels, milestones and all 77 issues with a script (`gh` CLI), so the
whole plan is visible on GitHub from day one.

---

## 2. The step ritual (same every time)

```
1. Pick the top card in "Next"  →  move to "In progress"
2. git switch -c step-NN-slug main
3. Read the step in plan/ (Goal → Sensei notes → Do → Verify)
4. Do the work in small commits on the branch (as many as you like)
5. Run Verify locally
6. Open a PR: title "Step NN — Title"; the PR template checklist fills itself in
7. CI goes green; you self-review the diff; write "What I learned" in the PR
8. Squash-merge  →  exactly ONE commit on main per step:  "step-NN: title (#issue)"
9. Move the card to Done; flip the checkbox in PLAN.md (done in the same PR)
```
Time-box: if a step takes more than ~2 sessions, split it (`Step NN-a`, `Step NN-b`) instead of pushing through tired.

---

## 3. Commit & branch conventions
* Branch: `step-NN-short-slug` (e.g. `step-12-tavern-actions`). Bugfixes: `fix-NNN-slug`.
* Squash commit message:
  ```
  step-12: engine — tavern actions part 1 (#45)

  Why: players need roll/freeze/start-turn before anything else can be tested.
  What: StartTurn, Roll, Freeze, Unfreeze actions + TavernResolver + 23 tests.
  Learned: a Resolver validates then mutates; mutators are the only writers.
  ```
* Tags: `phase-N-complete`, releases `vMAJOR.MINOR.PATCH` (prod deploys only from tags).

## 4. PR template (`.github/pull_request_template.md`)
```
## Step NN — Title            Closes #<issue>
### What changed
### How to verify (commands)
### Screenshots / GIF (UI steps)
### Checklist
- [ ] Verify commands pass locally
- [ ] Tests added/updated at the right layer
- [ ] Docs / PLAN.md status updated
- [ ] No fixed-cost AWS resources added (infra steps)
- [ ] Keyboard path works (UI steps)
### What I learned (one sentence, your words)
```

## 5. Labels
`phase:0 … phase:6` · `area:engine` `area:backend` `area:frontend` `area:infra` `area:assets`
`area:docs` `area:sim` · `type:step` `type:bug` `type:idea` `type:chore` · `good-first-step` (steps
that are mostly configuration, good for low-energy days) · `blocked`.

## 6. Cadence suggestions
* **Session** = 2–3 focused hours. Aim for 1 step per session early (setup steps are quick), 1 step
  per 1–2 sessions in the engine/frontend phases.
* **Weekly**: 10 minutes to write the journal paragraph and move cards.
* **Phase end**: the review ceremony (quality doc §10) — demo, metrics, retro, tag.
* Rough total: 77 steps ≈ 240 hours. At your **6–7 h/week** (Q17) that is ≈ 9 months; the **fastest
  path** (skip the 10 `[opt]` steps, defer Phase 4 until the corgi art arrives) is ≈ 210 h ≈ 7–8 months
  and still ships a complete game (Q7: keep it simple, ship fast).

## 7. Teaching format (every step uses it)
* **Sensei notes — What**: the concept in two sentences.
* **Why**: the problem it solves here, in this game.
* **How**: the mechanics, with the exact commands/files.
* **Where**: which folder/file you will see it in afterwards.
* **Check your understanding**: 1–3 questions. Answer them in the PR's "What I learned" or in the
  journal. If an answer is shaky, we pause and go deeper before merging.
* **Glossary links**: every new term appears in `knowledge/glossary_for_beginners.md`.
* **Calibration (Q16 — Java 1 · Spring 1 · React/TS 1 · AWS 1 · Git 3 · SQL/NoSQL 4):** Java, Spring,
  React and AWS steps explain from zero and never assume a term; Git steps are terse (you know branches
  and PRs — we only add squash-merge and tags); data steps lean on your SQL/NoSQL knowledge and teach the
  *differences* (single-table design, conditional writes, TTL) rather than what a key is.
* **Shell:** Git Bash (Q18). Scripts are `.sh`; commands are shown once, in bash.
* **AWS is console-first (Q31):** every AWS resource is first created **by hand in the AWS web console** on
  `dev`, following a detailed numbered walk-through that is written when the step arrives (the console UI
  drifts too fast to write it earlier); then the SAM/CloudFormation code recreates it and the manual copy is
  deleted. Prod is only ever deployed from code.

## 8. Re-planning rules
* Steps may be split, re-ordered within a phase, or marked optional — never silently skipped.
  Record the change in `PLAN.md` under "Changelog of the plan".
* New ideas go to the Backlog as `type:idea`; they are only promoted at a phase review.
* If a tool/version breaks (e.g. a library release), fix forward in a `chore` PR and note it in the
  relevant knowledge file.

## 9. Risk register (reviewed at each phase end)
| Risk | Signal | Response |
|---|---|---|
| Fatigue / "sea of code" | steps take > 2 sessions, journal gaps | split steps; do a `good-first-step`; re-read the map in `PLAN.md` |
| Scope creep | ideas entering "Next" mid-phase | park in Backlog; phase review decides |
| Cost surprise | budget e-mail | follow `docs/runbook.md`; teardown script if needed |
| Tool breakage on Windows | red CI only on local | prefer Git Bash for scripts; Docker Desktop running; WSL2 as fallback |
| Licence doubt | — | keep assets out of git (ADR-13); credits file |
