# Phase 6 — Balance, hardening & launch (Steps 70–77)

**Where you are:** everything deploys automatically; `v0.5.0` is on prod as a rehearsal. **At the
end of this phase:** the game is tuned with data, audited for performance, security and
accessibility, documented for recruiters, and **v1.0.0 is live** — for about $0 a month.

---

## Step 70 — Balance pass with the simulator
**Branch:** `step-70-balance` · **est 4h**

**Goal:** the numbers in `heroes.json` are defended by data, not vibes.

**You will have:** `sim run --games 10000 --bot greedy` and `--bot random` reports in
`docs/balance/<date>-before.md`; a tuning log (`docs/balance/tuning-log.md`: hero, change, reason,
before/after metric); content edits until every GDD §9 target passes `sim check`; scenario tests
updated where stats changed; `sim/thresholds.yaml` committed as the contract; nightly `sim-check` green.

**Sensei notes**
- *What:* balance = no dominant strategy, every hero playable. Metrics: pick rate, win rate when
  on band, battle length, clash-cap draws, economy value.
- *Why bots:* bots are not humans, but a hero that wins 80 % with a greedy bot is broken for humans too.
- *How:* change one number at a time; re-run; record. Prefer changing stats over changing ability text (text changes need frontend copy updates).
- *Where:* `engine/src/main/resources/content/`, `docs/balance/`.

**Verify:** `sim check` passes; coverage and scenarios still green; `docs/balance/<date>-after.md` committed.

**Commit:** `step-70: balance pass with simulator evidence`

**Check your understanding:** Which single metric would most likely reveal a "must-pick" hero?

---

## Step 71 — `[opt]` Content expansion
**Branch:** `step-71-content-expansion` · **est 5h**

**You will have:** the 18 stretch heroes from GDD §5 with scenarios; two new DSL effects
(`REPEAT_ABILITY` for Oracle; `SWALLOW`/`RELEASE` for Mimic Chest) with tests; art picks via the
gallery; a second balance pass; `sim check` still green; CHANGELOG entry.

**Verify:** 54 heroes in `ContentIndex.summary()`; all scenarios green; gallery shows every new hero.

**Commit:** `step-71: 18 stretch heroes and two new effect types`

---

## Step 72 — Ghost pool seeding for production
**Branch:** `step-72-ghost-seeding` · **est 2h**

**Goal:** the first real player always finds opponents.

**You will have:** `sim seed-ghosts --games 500 --out build/ghosts.json` (one snapshot per turn
per simulated run, `bot: true`), `tools/seed-ghosts/upload.sh prod` (batch-writes to the
table with TTL **far future** for bot ghosts — they must not expire — using the SSO profile; or a
`local`-profile backend endpoint for dev), ~200 bot ghosts per turn for turns 1–15; the
`MatchmakingFallback` metric observed on the dashboard (should be mostly `bot` early, then `real`).

**Sensei notes:** SAP solved the cold start with a huge player base; we solve it with bots that
played a fair game. Mark them `bot: true` so the UI can label the opponent "Wandering band" instead of a player name.

**Verify:** on prod, start a run and end turn 1 → opponent found with zero real players; dashboard shows `MatchmakingFallback{bot}`.

**Commit:** `step-72: bot ghost generation and production seeding`

---

## Step 73 — `[opt]` Performance pass
**Branch:** `step-73-performance` · **est 3h**

**You will have:** Lambda Power Tuning results (`aws-lambda-power-tuning` state machine run
once on dev, then deleted) → chosen `MemorySize`; SnapStart priming verified (`RESTORE_REPORT`
durations in logs; first-request latency measured 10× with `curl -w`); decision on a keep-warm
`ScheduleExpression: rate(5 minutes)` rule (free-tier; ~8,640 invocations/month) recorded in
`docs/perf.md`; frontend: `rollup-plugin-visualizer` report, lazy routes verified, images
compressed (`oxipng` on atlases), CloudFront compression on, `size-limit` budgets tightened;
Lighthouse ≥ 90 on prod URL.

**Verify:** p95 warm latency < 150 ms for `POST /actions` on dev (from CloudWatch); first hit after deploy < 1 s with priming (or keep-warm); Lighthouse report committed to `docs/perf/`.

**Commit:** `step-73: lambda tuning, snapstart verification and frontend performance`

**Check your understanding:** Why might *more* memory make a Lambda *cheaper* per request?

---

## Step 74 — Security & licence audit
**Branch:** `step-74-security-licence` · **est 3h**

**Goal:** nothing embarrassing, nothing exploitable, nothing we are not allowed to ship.

**You will have:** `gitleaks` in `security.yml` (history scan once, then per PR); OWASP
dependency-check + `npm audit --audit-level=high` gates; security headers verified with
securityheaders.com (A rating); JWT edge tests (expired, wrong audience, wrong issuer, `alg: none`
→ 401); IAM review (Lambda role has only its table + logs + metrics); API fuzz with jqwik on DTO
validation; Cognito: password policy, MFA optional on, advanced security **off** (cost);
`CREDITS.md` (FinalBossBlues / Time Fantasy, fonts, libraries, audio sources per track —
answering open question #12 — with proof of licence in `docs/licences/` kept private if needed);
`SECURITY.md` (how to report); `docs/security.md` summary.

**Verify:** all scanners green; headers A; a token with a tampered signature → 401; `CREDITS.md` lists every third-party asset actually shipped (script `tools/assets/credits_check.py` compares manifest vs credits).

**Commit:** `step-74: security scans, jwt edge cases, credits and licence verification`

**Check your understanding:** Why is "licence verification" a *launch blocker* for a portfolio project in particular?

---

## Step 75 — `[opt]` Accessibility & UX audit
**Branch:** `step-75-a11y-ux-audit` · **est 3h**

**You will have:** full keyboard-only playthrough recorded (Playwright video) and fixed issues;
NVDA pass over Title/Home/Tavern/Battle; contrast report; reduced-motion playthrough; focus order
check; `docs/a11y.md` with the checklist and known limitations; axe = 0 serious/critical on every
route in CI; three friends' first-run notes (what confused them) → copy/tutorial fixes.

**Verify:** axe clean; the keyboard video shows a full run; three usability notes closed as issues.

**Commit:** `step-75: accessibility and ux audit fixes`

---

## Step 76 — Recruiter-facing documentation
**Branch:** `step-76-recruiter-docs` · **est 3h**

**Goal:** someone with five minutes understands what this is and why it is good.

**You will have:** `README.md` rebuilt: hero GIF (record with Playwright video → `ffmpeg` → GIF
≤ 5 MB), one-paragraph pitch, **"Play it"** link (`https://heroes.bryanmcneil.pro`) + demo credentials, architecture diagram
(Mermaid), "Why these choices" (5 bullets linking ADRs), badges (CI, coverage, Lighthouse),
**"Run it locally in 5 minutes"** (docker compose + 2 commands), repo map, "What I learned"
(your journal highlights), cost receipt screenshot (`docs/costs/`); `docs/` index; link from your
`portfolio` repo site to the game; LinkedIn/GitHub profile pin.

**Sensei notes:** recruiters skim: GIF first, then the architecture picture, then whether tests
exist. Make those three impossible to miss.

**Verify:** a friend who is not a developer can explain what the project is after 60 seconds on the README; the 5-minute local run works on a fresh clone.

**Commit:** `step-76: recruiter-facing readme, gif, diagram and portfolio link`

---

## Step 77 — Launch v1.0.0
**Branch:** `step-77-launch` · **est 2h**

**Goal:** ship it, prove it, and define how it stays shipped.

**Launch checklist** (copy into `docs/reviews/launch.md` and tick):
- [ ] `main` green; `sim check` green; Lighthouse budgets met; axe clean.
- [ ] `CHANGELOG.md` has a `1.0.0` section; `scripts/release.sh 1.0.0` → tag pushed.
- [ ] `deploy-prod` waiting for approval → **approve** → green → GitHub Release created.
- [ ] Smoke: sign in on prod, play one full turn, battle plays with sound, leaderboard loads.
- [ ] Monitoring: dashboard shows the smoke traffic; alarms OK; budget e-mails confirmed.
- [ ] Cost: `aws ce get-cost-and-usage` month-to-date screenshot → `docs/costs/2026-xx.png`.
- [ ] Ghost pool seeded on prod; first-turn opponent found.
- [ ] Demo account works; README "Play it" link points at prod; portfolio updated.
- [ ] Post-launch policy written in `docs/release.md`: *no manual changes in prod — every change is a PR → main (dev) → tag (prod)*; monthly 5-minute cost check; dependency PRs merged monthly.
- [ ] Delete the local reference copy: `rm -rf "Super Auto Pets"` (it was never in git; the real install lives elsewhere) and drop its `.gitignore` line in the same PR.
- [ ] Free plan reminder is in your calendar (credits expire ≈ 6 months after account creation; `knowledge/cost_estimator.md` §1 — decided Q26: *upgrade to the Paid plan* two weeks before they expire).
- [ ] `docs/reviews/phase-6.md` + overall retro; tag `phase-6-complete`.

**Commit:** `step-77: v1.0.0 launch checklist and post-launch policy`

**Check your understanding (final):** Explain the whole system to me in two minutes as if I were a recruiter — then write it in `docs/journal.md`. That paragraph is the real deliverable of this project.
