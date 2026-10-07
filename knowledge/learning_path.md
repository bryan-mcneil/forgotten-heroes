# Learning Path — what you will know by the end, and when

> Calibrated to your self-rating (Q16): Java 1 · Spring 1 · React/TS 1 · AWS 1 · Git 3 · SQL/NoSQL 4.
> Everything in the first four columns is taught from zero; Git is assumed; data steps teach the NoSQL
> *differences* from the SQL you already know.

> You said: "think of me as a complete beginner and train me." This file is the syllabus. Each
> phase has learning objectives, the steps that teach them, and the interview-ready sentence you
> will be able to say afterwards. Official docs are linked because they are the source of truth
> when my explanation and reality disagree.

## The Sensei method (how every step teaches)
1. **Map first.** Each step starts by telling you where you are (phase, what exists, what comes next).
2. **One concept per step.** If a step needs two new concepts, it is split.
3. **See it, type it, break it.** You run the command, read the output, then I ask you to change
   one thing and predict what happens *before* running it.
4. **Say it back.** The PR's "What I learned" sentence is the exit ticket.
5. **Glossary, not jargon.** Every new word is in `glossary_for_beginners.md`.

---

## Phase 0 — Foundations (Steps 01–06)
**Objectives:** set up a professional dev machine; understand repo/branch/PR/CI; understand what
an AWS account is and how to keep it safe and cheap.
**You will be able to say:** "I run a public monorepo with branch protection, CI on every PR,
squash-merged steps, and an AWS account hardened with MFA, a least-privilege CLI identity and budget alarms."
Reading: GitHub Flow (docs.github.com/en/get-started/using-github/github-flow) · AWS account
security basics (docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html) · AWS Budgets.

## Phase 1 — Game Engine (Steps 07–24)
**Objectives:** modern Java (records, sealed types, pattern matching, streams); designing a domain
model; Action → Resolver → Mutator → Event; deterministic RNG; data-driven design (JSON DSL);
JUnit 5, property-based testing, ArchUnit; reading coverage reports.
**You will be able to say:** "I built a deterministic, event-sourced rules engine with a JSON
ability DSL, 90 %+ coverage, property tests for invariants and a simulator that plays 10,000
games to tune balance."
Reading: Java records & sealed classes (dev.java/learn) · JUnit 5 User Guide (junit.org/junit5/docs/current/user-guide) ·
jqwik user guide (jqwik.net/docs/current/user-guide.html) · "Event Sourcing" (martinfowler.com/eaaDev/EventSourcing.html).

## Phase 2 — Backend (Steps 25–36)
**Objectives:** Spring Boot layers, dependency injection, profiles, validation, error handling,
OpenAPI; DynamoDB single-table design and conditional writes; Cognito JWTs; Lambda packaging,
SnapStart, SAM templates; structured logging and metrics.
**You will be able to say:** "I run a Spring Boot 4 API on Lambda with SnapStart behind an HTTP API
with a Cognito JWT authorizer, persisting to a single DynamoDB table with optimistic locking,
deployed by SAM."
Reading: Spring Boot reference (docs.spring.io/spring-boot/reference) · DynamoDB single-table
(aws.amazon.com/blogs/compute/creating-a-single-table-design-with-amazon-dynamodb) · Lambda
SnapStart (docs.aws.amazon.com/lambda/latest/dg/snapstart.html) · SAM developer guide
(docs.aws.amazon.com/serverless-application-model) · aws-serverless-java-container README (github.com/aws/serverless-java-container).

## Phase 3 — Frontend (Steps 37–56)
**Objectives:** React components/hooks/state; TypeScript; Vite; Tailwind tokens; routing; a
typed API client generated from OpenAPI; Zustand; drag & drop + keyboard accessibility; CSS
sprite animation; a playback engine; audio on the web; Vitest/RTL/Playwright; Lighthouse budgets.
**You will be able to say:** "I built an accessible React 19 game client with a typed API, a
deterministic event-playback engine, pixel-perfect sprite rendering, keyboard-first controls, and
E2E tests, inside Lighthouse performance budgets."
Reading: react.dev/learn · TypeScript handbook · Vite guide (vite.dev/guide) · Tailwind v4 docs ·
dnd-kit docs (dndkit.com) · Playwright docs (playwright.dev) · web.dev accessibility.

## Phase 4 — Studio & brand (Steps 57–60)
**Objectives:** motion design basics, favicons/OG/PWA manifest, static hosting on Hostinger via Git.
**You will be able to say:** "I ship a branded intro, a landing page on a second host, and
install-ready PWA metadata."

## Phase 5 — Infrastructure & CI/CD (Steps 61–69)
**Objectives:** complete SAM template (CloudFront, S3, Cognito, alarms); GitHub OIDC; dev/prod
environments with manual approval; smoke tests; monitoring; cost guardrails; teardown; runbooks.
**You will be able to say:** "Every push to main deploys dev; tagging a release deploys prod after
my approval; no AWS keys exist in GitHub; alarms and budgets e-mail me; one script tears everything down."
Reading: configure-aws-credentials OIDC (github.com/aws-actions/configure-aws-credentials) ·
CloudFront + S3 OAC · GitHub Environments.

## Phase 6 — Balance, content & launch (Steps 70–77)
**Objectives:** use data to tune a game; performance profiling; security review; accessibility
audit; writing for recruiters; a real launch checklist.
**You will be able to say:** "v1.0.0 is live for about $0/month; here is the balance report, the
Lighthouse report, the coverage report and the architecture diagram."

---

## Questions recruiters ask → where the answer lives in the repo
| Question | Evidence |
|---|---|
| "Tell me about a system you designed." | `knowledge/architecture.md`, ADR table, diagram in README |
| "How do you test?" | `engine/src/test` scenarios + property tests; CI badges; `knowledge/quality_and_testing_strategy.md` |
| "Have you used AWS in production?" | `infra/template.yaml`, workflows, `docs/costs/` receipts, dashboard screenshot |
| "How do you handle concurrency?" | ADR-5, `RunRepository` conditional writes, the 409 test |
| "What was hard?" | `docs/journal.md` (your own words), SnapStart tuning notes |
| "Show me frontend skills." | live demo, Lighthouse report, Playwright a11y run |

## Study habits that make this stick
* Keep `docs/journal.md` open; one paragraph per session, even "today I only installed things".
* When something breaks, write the error *before* fixing it — the fix means more afterwards.
* Re-read the previous step's Sensei notes before starting the next one (5 minutes).
* Once per phase, explain the phase to a rubber duck (or to me) without looking.
