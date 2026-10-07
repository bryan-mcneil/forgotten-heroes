# Open Questions — all answered (1–19 and 26–30 on 7 Oct 2026; 20–25 by default)

> You asked me to grill you. Questions 1–19 and 26–30 are **answered** (the log at the bottom says what
> changed and where); 20–25 keep their defaults. Nothing is open. New questions go at the bottom of section F
> as the build raises them.

## A. Accounts & money
1. **When was your AWS account created — before or after 15 July 2025?** (Billing console →
   "Free Tier" page.) Changes nothing architecturally; it changes whether you may be asked to
   "upgrade to the Paid plan" after 6 months. *Default: assume legacy account; design is $0 either way.*
2. **Do you have a domain (e.g. included with Hostinger Business)? Which one?** *Default: ship on
   the `*.cloudfront.net` URL first; add a custom domain in Step 66.* → **Answered: `heroes.bryanmcneil.pro`.**
3. **Which e-mail should receive AWS alarm/budget e-mails?** *Default: the account owner's e-mail (kept out of the public repo).*
4. **Region preference?** *Default: us-east-1 (cheapest, required for CloudFront certificates).*

## B. Scope & product
5. **Is "Forgotten Heroes" the final name?** It decides the repo name, package names, and the
   Cognito domain prefix. *Default: yes → repo `forgotten-heroes`, package `com.forgottenheroes`.*
6. **Public GitHub repo from day one?** Recruiters need it public; assets stay out of git (ADR-13).
   *Default: public.*
7. **36 heroes + 16 relics for v1.0 — enough?** (Stretch list of 18 more exists.) *Default: yes.*
8. **Tone: lighthearted tavern (current) or darker fantasy?** Affects copy and music picks.
   *Default: lighthearted, warm.*
9. **Login: e-mail sign-up via Cognito + a shared demo account for recruiters — OK?** Guest play
   and Google sign-in are stretch items. *Default: yes.*
10. **Desktop-first, tablet OK, phones later — agreed?** *Default: yes.*
11. **Do you want Versus (live 8-player) ever?** It changes nothing now, but if "yes, eventually"
    I will keep the WebSocket door open in the API design. *Default: no for v1; door left open anyway.*

## C. Assets & licences
12. **The audio library in `derived/Audio` — who made each BGM track?** (Self-made? purchased pack?
    royalty-free site?) I need to know the licence terms to publish them on a public website.
    *Default: treat as yours, keep out of git, verify before launch (Step 74).*
13. **Palette: original Time Fantasy (needed for the animated SV battlers) vs your Elements
    recolours?** *Default: original TF everywhere; Elements-only creatures (hydra, dragons) are
    converted or skipped.*
14. **Corgi Space Cadet art — when can you provide it, and in what form?** (See spec in
    `asset_inventory_and_pipeline.md` §6.) *Default: placeholder until then; Step 58 swaps it in.*
15. **Any hero names/archetypes you definitely want (or hate)?** Your previous game had named
    characters (aoi, kaede…) — want cameos? *Default: the GDD roster as written.*

## D. You (to calibrate the teaching)
16. **Rate yourself 0–5 on: Java · Spring · React/TypeScript · AWS · Git/GitHub · SQL/NoSQL ·
    testing.** *Default: 1 everywhere except Git (2) — every step explains from scratch.*
17. **How many hours per week can you give this, realistically?** *Default: 6–8 → ~6 months;
    optional steps can be skipped to shorten.*
18. **Do you prefer PowerShell or Git Bash for day-to-day commands?** Scripts ship in both.
    *Default: PowerShell shown first, Bash in a collapsible.* → **Answered: Git Bash everywhere.**
19. **Should I create the GitHub issues/board automatically with the `gh` CLI in Step 04?** *Default: yes.*

## E. Engineering choices you can veto
20. **One Lambda running Spring Boot (Lambdalith) + SnapStart** vs many small functions. *Default: Lambdalith.*
21. **SAM** (YAML) vs **CDK** (TypeScript/Java code) for infrastructure. *Default: SAM; CDK as a later upgrade.*
22. **Server-authoritative per-action API** (simple) vs SAP's **batch-and-hash** (snappier, more
    code). *Default: per-action; documented upgrade path.*
23. **DOM/CSS rendering** vs **PixiJS canvas**. *Default: DOM/CSS; `BattlePlayer` isolates the choice.*
24. **Mutation testing (PIT) and Lighthouse CI** — keep as optional nightly jobs? *Default: yes, optional.*
25. **Keep-warm ping** (a 5-minute scheduled invocation, free-tier) to hide SnapStart's first-hit
    delay? *Default: measure first (Step 73), decide then.*

## F. Questions raised by your answers (answered 7 Oct 2026)
26. **Free plan → Paid plan.** Your credits expire ≈ 6 months after account creation (≈ Feb/Mar 2027); AWS
    then closes Free-plan accounts after a 90-day grace. Upgrading keeps the Always-Free tiers and our
    ≈ $0.03/month, but removes the "cannot be billed" safety net — the kill switch in Step 67 replaces it.
    *Default: upgrade two weeks before expiry; Step 06 sets the reminder.* → **Answered: default — upgrade.**
27. **US-only geo-restriction** also blocks VPN users and anyone abroad — including a recruiter travelling.
    *Default: on for prod; one template parameter (`AllowedCountries`) widens it.* → **Answered: yes, even
    though it blocks VPN users — so Step 61 now shows them a "US only" page instead of a broken app.**
28. **Cameo slots.** I placed the six Forgotten Kanji heroes by element and ability: Aoi = Rank I (Squire
    slot), Hotaru = Rank I (Acolyte), Iwao = Rank III (Paladin), Kashi = Rank IV (Templar), Kaede = Rank IV
    (Pyromancer), Nagi = Rank VI (Shadow Blade). Happy with that? *Default: yes; the Step 40 gallery is the
    last cheap moment to swap.* → **Answered: yes.**
29. **"Private host-to-host rooms"** — a friend plays the same seed and you compare (a challenge link, no new
    infrastructure), or true live rooms (WebSocket API)? *Default: same-seed challenge link first.* → **Answered: default — challenge link first; live rooms stay a door.**
30. **Folder rename** `fantasy_heroes` → `forgotten_heroes` before Step 02? *Default: yes — command in
    `README.md`; Claude Code's project memory was copied to the new path.* → **Answered: done — the folder is
    `forgotten_heroes`.**
31. **AWS setup — by hand or by code?** (Raised by you after the plan, 7 Oct 2026.) → **Answered: console
    first, code second.** You want to set AWS services up **manually in the AWS web console**, with detailed
    step-by-step instructions, when the time comes. So every step that creates AWS resources (06, 34, 61, 62,
    66, 67, 68) starts with a numbered click-by-click **Console walk-through** on `dev`, written in full when
    the step arrives (the console UI changes too often to write it now); the step's SAM/CloudFormation then
    recreates the same resources as code and the hand-made copies are deleted. Prod is never built by hand —
    that keeps your own "never touch production after setup" goal intact.

## Answers log
| # | Your answer | Date | Files updated |
|---|---|---|---|
| 1 | Created ≈ Aug/Sep 2026 → credit-based **Free plan** | 2026-10-07 | cost_estimator §1/§5, phase_0 Step 06, phase_6 Step 77, environment_audit §3, glossary |
| 2 | Domains gadgetdrop.tech and **bryanmcneil.pro**; use bryanmcneil.pro | 2026-10-07 | architecture §1/§9, tech_stack §7, phase_5 Step 66 (now required), PLAN, README |
| 3 | the account owner's e-mail (kept out of the public repo) | 2026-10-07 | phase_0 Step 06, cost_estimator §5 |
| 4 | us-east-1; **US-only**; think about DDoS cost | 2026-10-07 | architecture §1/§9/§12 + ADR-15, cost_estimator §2/§5, phase_5 Steps 61 & 67, phase_2 Step 36 note, glossary |
| 5 | **Forgotten Heroes** (matches *The Forgotten Kanji*) — agreed | 2026-10-07 | every file; repo `forgotten-heroes`, package `com.forgottenheroes`, folder rename in README |
| 6 | Public | 2026-10-07 | default kept |
| 7 | 36 + 16 is enough; keep it simple, ship fast | 2026-10-07 | PLAN "fastest path", GDD §10, project_management §6 |
| 8 | Lighthearted, warm | 2026-10-07 | GDD header, ux doc §copy |
| 9 | Yes (Cognito e-mail sign-up + demo account) | 2026-10-07 | default kept |
| 10 | Yes (desktop-first) | 2026-10-07 | default kept |
| 11 | No Versus; at most single player + private host-to-host rooms later; door open | 2026-10-07 | GDD §3/§10, architecture §4.4 |
| 12 | Royalty-free: DRAGON-STUDIO (ko-fi.com/dragonstudio), Helton Yan, Freesound_Community (pixabay), OxidVideos, HorrorSFXFree | 2026-10-07 | asset_inventory §5, README licence, phase_6 Step 74 (unchanged — it records per track) |
| 13 | **Elements palette**; convert what is needed inside `forgotten_kanji_app` so both games share it | 2026-10-07 | asset_inventory §1/§2/§4, architecture ADR-14, phase_3 Steps 39–40, GDD §5, ux doc palette |
| 14 | Corgi art later; can wait until the end | 2026-10-07 | PLAN (Phase 4 deferred), phase_4 intro, GDD §11 |
| 15 | Yes — Forgotten Kanji names (Aoi, Nagi, Iwao, Hotaru, Kashi, Kaede) | 2026-10-07 | GDD §5 cameo rows + §7 example, quality doc scenario, plan/ux mentions |
| 16 | Java 1 · Spring 1 · React/TS 1 · AWS 1 · Git 3 · SQL/NoSQL 4 | 2026-10-07 | project_management §7, learning_path |
| 17 | 6–7 h/week | 2026-10-07 | project_management §6, PLAN totals |
| 18 | Git Bash | 2026-10-07 | all plan files (bash commands, `.sh` scripts), environment_audit, tech_stack risks |
| 19 | Yes — walk me through it | 2026-10-07 | phase_0 Step 04 (by hand first, then the script) |
| 20–25 | not answered → defaults stand (Lambdalith, SAM, per-action API, DOM/CSS, optional PIT/Lighthouse, measure before keep-warm) | — | — |
| 26 | Default — **upgrade to the Paid plan** two weeks before the credits expire | 2026-10-07 | phase_0 Step 06 (reminder now says *upgrade*, not *decide*), cost_estimator §1, phase_6 Step 77 checklist |
| 27 | **Yes** — US-only stays on even though it blocks VPN users | 2026-10-07 | phase_5 Step 61 (SPA fallback on 404 only, CloudFront 403 → `blocked.html`), architecture §1/§9/§12/ADR-15, cost_estimator §5, ux doc §11 (copy), glossary, PLAN Step 61 row, README |
| 28 | **Yes** — cameo slots as placed | 2026-10-07 | GDD §5 note |
| 29 | Default — **same-seed challenge link** first; live rooms stay a door | 2026-10-07 | GDD §3, architecture §4.4 |
| 30 | **Done** — folder renamed to `forgotten_heroes` | 2026-10-07 | README "Before Step 01", PLAN rule 6 removed, Claude Code project memory |
| 31 | Set AWS services up **by hand in the web console**, with detailed step-by-step instructions when each step arrives | 2026-10-07 | PLAN rule 6 + changelog, project_management §7, phase_0 Step 06, phase_2 Step 34, phase_5 intro, tech_stack §8, CLAUDE.md, Claude Code memory |
