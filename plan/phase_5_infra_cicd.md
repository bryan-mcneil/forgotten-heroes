# Phase 5 — Infrastructure & CI/CD to production (Steps 61–69)

**Where you are:** game playable on `dev`; deploys are manual commands from your laptop. **At the
end of this phase:** pushing to `main` deploys `dev` automatically; tagging `v*` deploys `prod`
after you click Approve; alarms e-mail you; one script deletes everything. **After Step 65 you
never need to touch production by hand again** — exactly what you asked for.

Blueprint: `knowledge/architecture.md` §9–§11. Cost rules: `knowledge/cost_estimator.md` §5.

**Console first (Q31):** Steps 61, 62, 66, 67 and 68 each create AWS resources. For each, you first build the
thing **by hand in the AWS web console** on `dev`, following a numbered click-by-click walk-through that is
written when the step arrives (the console UI changes too often to write it now). Then the template or script
recreates it as code and the hand-made copy is deleted. Budget ≈ 1–2 extra hours per step. **Prod is never
built by hand** — that is the whole point of this phase.

---

## Step 61 — Complete SAM template: frontend hosting
**Branch:** `step-61-sam-frontend-hosting` · **est 4h**

**Goal:** the website *and* the API are served by CloudFront from the same stack — to US visitors only (Q4). Everyone
else, VPN exits abroad included (Q27), gets a small page that says so instead of a broken app.

**You will have (in `infra/template.yaml`):** `SiteBucket` (private, `BucketEncryption`,
`PublicAccessBlock` all true), `SiteOAC` (`AWS::CloudFront::OriginAccessControl`),
`SiteDistribution` (default root `index.html`; origin = bucket via OAC; `ViewerProtocolPolicy
redirect-to-https`; `Compress: true`; cache policy `CachingOptimized`; **custom error responses**
404 → `/index.html` 200 (SPA routing) and **403 → `/blocked.html` 403** (what a geo-blocked visitor sees);
`SiteBucketPolicy` grants the distribution `s3:GetObject` **and `s3:ListBucket`**, so a missing file is a
404 from S3, never a 403; `frontend/public/blocked.html` (static, no JS, copy in
`knowledge/ux_visual_sound_design.md` §11); `ResponseHeadersPolicy` with security headers
(HSTS, X-Content-Type-Options, Referrer-Policy, a CSP allowing self + Cognito domain + API
origin); `PriceClass_100`; optional `Aliases`/`ViewerCertificate` behind a condition; **a second origin = the HTTP
API** with a `/api/*` cache behaviour (`CachingDisabled` + `AllViewerExceptHostHeader` origin-request
policy, all methods) carrying an **origin custom header** `x-origin-verify: <secret>` (secret in SSM
Parameter Store, free tier, read by the template); **geo restriction** `Whitelist` from parameter
`AllowedCountries` (default `US`)), a Spring `OriginVerifyFilter` that returns 403 when the header is
missing or wrong (prod profile only — local and direct dev access unchanged),
`SiteBucketPolicy` (allow only this distribution), Cognito `CallbackURLs`/`LogoutURLs` built from
the distribution domain (+ localhost in dev), HTTP API CORS `AllowOrigins` from the same;
`Parameters: Env, SiteDomain (default ""), CertificateArn (default "")`; `Outputs: SiteUrl,
SiteBucketName, DistributionId, ApiUrl, UserPoolId, UserPoolClientId, UserPoolDomain`;
`scripts/deploy-frontend.sh dev` (reads outputs → writes `frontend/.env.production` → `npm run build`
→ `aws s3 sync dist/ s3://<bucket> --delete` with `--cache-control` long for hashed files and
`no-cache` for `index.html` → `aws cloudfront create-invalidation --paths "/index.html" "/manifest.json"`).

**Sensei notes**
- *What:* CloudFront is the CDN in front of a private S3 bucket. **OAC** signs CloudFront's
  requests so nothing else can read the bucket. The **SPA fallback** makes `/tavern` load `index.html`.
- *Why in the same template:* one stack = one deploy = one teardown; outputs feed the frontend build.
- *Why the API behind CloudFront:* one domain (no CORS in prod), the geo-restriction and AWS **Shield
  Standard** (free, automatic) now cover the API too, and the raw `execute-api` URL becomes useless to
  anyone who finds it (missing header → 403). Geo-restriction is a country filter on the viewer's IP —
  a US recruiter on a VPN exit in Europe is blocked too. You accepted that (Q27), so the block must explain
  itself: CloudFront's own 403 is swapped for `blocked.html`. That only works if **403 means nothing else**
  — hence the SPA fallback listens on **404 only** (S3 answers 404 instead of 403 once the OAC may
  `s3:ListBucket`), and the API never returns 403 (it uses 401/404/409/422). Widen `AllowedCountries` if the
  block ever bites.
- *How:* `sam deploy` (CloudFront takes ~5 minutes the first time); hashed assets get
  `max-age=31536000, immutable`, `index.html` gets `no-cache`.
- *Where:* `infra/template.yaml`, `scripts/deploy-frontend.sh`.

**Do this:** write resources; `sam validate --lint`; deploy dev; run the frontend deploy script
(first pull assets from the private bucket); update the dev pool callback URLs (template does it); test sign-in on the CloudFront URL.

**Verify:** `https://<id>.cloudfront.net` loads the title screen; deep link `/tavern` works after refresh;
`curl -I` shows the security headers; `curl -I https://<id>.cloudfront.net/api/health` → 200 while
`curl -I https://<api-id>.execute-api.us-east-1.amazonaws.com/api/health` → 403 (direct access blocked);
S3 console: bucket is not public; Cognito sign-in round-trips; `curl -I https://<id>.cloudfront.net/nope.js`
→ 200 with `index.html` (the fallback came from a 404, not a 403). *Optional:* from a VPN exit outside the
US (or one dev deploy with `--parameter-overrides AllowedCountries=CA`, reverted straight after) the site
answers **403 with `blocked.html`**.

**Commit:** `step-61: cloudfront + s3 hosting, api behaviour, geo-restriction + blocked page, headers, cognito urls and frontend deploy script`

**Check your understanding:** Why must `index.html` be `no-cache` while everything else can be cached for a year?
Why does the SPA fallback listen on 404 only, now that 403 means "blocked"?

---

## Step 62 — GitHub OIDC roles (no AWS keys in GitHub)
**Branch:** `step-62-github-oidc` · **est 3h**

**Goal:** GitHub Actions can deploy with temporary credentials scoped to this repo.

**You will have:** `infra/github-oidc.yaml` (CloudFormation, deployed once by you with your SSO
profile): `AWS::IAM::OIDCProvider` for `token.actions.githubusercontent.com`; `DeployRoleDev`
trusting `repo:bryan-mcneil/forgotten-heroes:ref:refs/heads/main` (and `pull_request` for
read-only jobs if needed); `DeployRoleProd` trusting `repo:…:environment:production`;
permissions policy: CloudFormation on stacks named `forgotten-heroes-*`, the SAM artifact bucket,
Lambda/API GW/DynamoDB/Cognito/CloudFront/S3/Logs/CloudWatch/SNS/IAM `PassRole` limited to roles
the stack creates (`iam:CreateRole` with a permissions boundary) — start from `sam pipeline
bootstrap`'s generated policy and trim; read-only on `forgotten-heroes-assets-*`; `docs/ci-cd.md`.

**Sensei notes**
- *What:* **OIDC**: GitHub issues a signed token stating "workflow X in repo Y on branch Z";
  AWS verifies it and hands out 1-hour credentials for a role whose trust policy matches those claims.
- *Why:* leaked long-lived keys are the classic disaster; with OIDC there is nothing to leak.
- *How:* `aws cloudformation deploy --template-file infra/github-oidc.yaml --capabilities CAPABILITY_NAMED_IAM`;
  in workflows `permissions: id-token: write, contents: read` and `aws-actions/configure-aws-credentials@v4` with `role-to-assume`.
- *Where:* `infra/github-oidc.yaml`, GitHub repo → Settings → Secrets and variables → **Variables** (`AWS_ROLE_DEV`, `AWS_ROLE_PROD`, `AWS_REGION` — variables, not secrets: role ARNs are not sensitive).

**Verify:** a throwaway workflow `oidc-test.yml` (manual trigger) runs `aws sts get-caller-identity` and prints the dev role; delete it afterwards (or keep as `workflow_dispatch` diagnostic).

**Commit:** `step-62: github oidc provider and deploy roles`

**Check your understanding:** What claim in the token stops a fork of your repo from assuming your role?

---

## Step 63 — Complete `ci.yml`
**Branch:** `step-63-ci-complete` · **est 3h**

**Goal:** every PR proves the whole system still works.

**You will have:** jobs `java` (setup-java corretto 25 with Maven cache; `./mvnw -B -ntp verify`;
Testcontainers works on `ubuntu-latest` out of the box; upload JaCoCo + surefire reports as
artifacts; publish test summary with `dorny/test-reporter`), `frontend` (setup-node 26 with npm
cache; `npm ci`; lint; typecheck; `api:types` freshness check; unit tests with coverage; build;
bundle size check `size-limit`), `lambda-smoke` (`sam build` + `sam local invoke` health),
`docs` (from Step 05), `e2e` (needs Docker: `docker compose up -d`, backend in background,
Playwright; runs when PR has label `e2e`, on `main`, and nightly `schedule`), `lighthouse`
(on `frontend/**` changes, `lhci autorun` against `npm run preview`, budgets asserted),
`sim-check` (nightly: `sim run --games 5000` + `sim check`, report uploaded); `concurrency`
groups cancel superseded runs; required checks updated in branch protection (`java`, `frontend`, `lambda-smoke`, `docs`).

**Sensei notes:** fast required checks on every PR, slow checks nightly or by label — that is how
real teams keep CI under 10 minutes. Caching Maven and npm is the single biggest time saver.

**Verify:** a PR shows all required checks green in < 10 min; add label `e2e` → the E2E job runs and passes.

**Commit:** `step-63: complete ci pipeline with caches, e2e, lighthouse and nightly sim check`

**Check your understanding:** Why do we not require the `e2e` job on every PR?

---

## Step 64 — `deploy-dev.yml` (push to main → dev)
**Branch:** `step-64-deploy-dev` · **est 3h**

**Goal:** merging a step deploys it.

**You will have:** workflow on `push: branches: [main]` with `concurrency: deploy-dev` (no
parallel deploys): checkout → setup Java/Node → `./mvnw -B -ntp -DskipTests package` →
configure-aws-credentials (dev role) → `sam build` → `sam deploy --config-env dev --no-confirm-changeset --no-fail-on-empty-changeset`
→ read stack outputs → `pull` assets from the private bucket → `npm ci && npm run build` with
`VITE_*` from outputs → `aws s3 sync` + invalidation (reuse `scripts/deploy-frontend.sh`) →
**smoke tests**: `curl` health must be 200, and a Playwright `smoke.spec` against the live dev
URL (title loads, dev login not available → check public pages + content endpoint) → on failure the
job fails loudly (CloudFormation auto-rolls back a failed stack update); Slack/e-mail not needed — GitHub e-mails failures.

**Sensei notes:** the deploy script you run locally and the workflow run the **same scripts**;
the workflow only adds credentials and ordering. If the workflow and your laptop disagree, the scripts are wrong, not the workflow.

**Verify:** merge a trivial change → Actions shows a green deploy → the dev site shows the change within ~3 minutes; break health on purpose on a branch → PR CI fails before it can deploy.

**Commit:** `step-64: automatic dev deployment on main`

**Check your understanding:** What does CloudFormation do if the Lambda update succeeds but the HTTP API change fails halfway?

---

## Step 65 — `deploy-prod.yml` (tag → approval → prod)
**Branch:** `step-65-deploy-prod` · **est 2h**

**Goal:** production deploys are deliberate, reviewed, and reproducible.

**You will have:** GitHub **Environment `production`** with *Required reviewers: you* and
deployment branches/tags limited to `v*`; `samconfig.toml` `prod` env (`stack_name
forgotten-heroes-prod`, `Env=prod`); workflow on `push: tags: ['v*']` → job `deploy` with
`environment: production` (pauses until you approve in the Actions UI) → same steps as dev with
the prod role → smoke tests against the prod URL → creates a **GitHub Release** from the tag with
auto-generated notes; `scripts/release.sh 1.0.0` (checks clean `main`, updates
`CHANGELOG.md`, tags, pushes); `docs/ci-cd.md` updated with the rollback recipe: re-run the
previous tag's workflow (`gh workflow run deploy-prod.yml -f tag=v0.9.0`) — the workflow accepts a
`workflow_dispatch` input for exactly this.

**Sensei notes:** prod = dev pipeline + a human gate + immutable tags. Rollback = redeploy an old
tag; because infra and code live in the same tag, the rollback is complete.

**Verify:** tag `v0.1.0` → workflow waits → approve → prod stack created → prod URL works; run the rollback recipe once to prove it.

**Commit:** `step-65: production deployment with manual approval and releases`

**Check your understanding:** Why deploy from tags rather than from the `main` branch head for production?

---

## Step 66 — Custom domain `heroes.bryanmcneil.pro` at $0
**Branch:** `step-66-custom-domain` · **est 2h**

**Goal:** the game lives at a URL that belongs to you (Q2: `bryanmcneil.pro`, your portfolio domain).
The studio page (Phase 4) later sits at `corgi.bryanmcneil.pro` on Hostinger; whatever your root domain
serves today is untouched.

**You will have:** ACM certificate in **us-east-1** for `heroes.bryanmcneil.pro` (DNS validation:
add the CNAME ACM gives you in **Hostinger DNS**); template parameters `SiteDomain`,
`CertificateArn` set for prod; CloudFront alias + certificate; CNAME `heroes` → distribution
domain in Hostinger DNS; Cognito callback URLs updated by the template; optional Cognito custom
domain skipped (needs another cert and a parent A record — not worth it).

**Verify:** `https://heroes.bryanmcneil.pro` serves the game with a valid certificate; sign-in works; old CloudFront URL still works.

**Commit:** `step-66: custom domain via acm and hostinger dns`

**Check your understanding:** Why does the certificate have to live in us-east-1 even if nothing else does?

---

## Step 67 — Monitoring and cost guardrails
**Branch:** `step-67-guardrails` · **est 4h**

**Goal:** the system tells you when something is wrong, and cannot run away with money.

**You will have (template):** `ReservedConcurrentExecutions: 10` on `ApiFunction`; stage
throttling confirmed (20/50); `LogGroup` retention 14 d for Lambda **and** API Gateway access
logs (JSON format, request id, status, latency, route); alarms from Step 35 reviewed + `ApiGateway
5xx` and a **"cost canary"** alarm on `Lambda Invocations > 50,000/day`; a **sign-up canary** alarm on Cognito
`SignUpSuccesses > 200/day` (Cognito bills per monthly active user above 10,000 — fake sign-ups are the
one way this project could get expensive); a **cost kill switch**: `KillSwitchFunction` (tiny Python
Lambda, free tier) subscribed to the alarm topic *and* to the $20 budget's SNS topic, which on `ALARM`
(1) sets `ApiFunction` reserved concurrency to **0**, (2) sets the user pool to
`AllowAdminCreateUserOnly: true` (sign-ups off), (3) disables the CloudFront distribution, and e-mails
you what it did; parameter `AutoKill` (default `true` for prod, `false` for dev); `scripts/unkill.sh <env>`
to restore all three; the SNS topic subscription
confirmed for prod; dashboard includes both envs; `docs/runbook.md` complete (per alarm: meaning,
first checks, how to pause: set reserved concurrency to 0 = instant off switch); Budgets from
Step 06 re-verified; `docs/costs/README.md` with the monthly screenshot ritual.

**Sensei notes:** reserved concurrency is a *ceiling*, not a reservation you pay for; setting it to
0 is the fastest way to stop a misbehaving function. Access logs let you answer "who hit what" without touching the app.
The kill switch is the answer to "I don't want to be charged because of a DDoS" (Q4): budgets lag by hours,
CloudWatch alarms fire in minutes, and the three API calls above stop *every* metered path — compute,
identity and bandwidth. Re-enabling is one script in the runbook.

**Verify:** `aws lambda get-function-concurrency` shows 10; on **dev** invoke the kill switch with a fake
alarm payload → concurrency 0, sign-ups off, distribution disabled → `scripts/unkill.sh dev` restores all
three; trigger an alarm deliberately (e.g. set the 5xx threshold to 0 for a minute) → e-mail arrives → restore.

**Commit:** `step-67: reserved concurrency, access logs, alarms, cost kill switch, runbook and cost ritual`

**Check your understanding:** Which guardrail bounds the *maximum possible* monthly bill, and roughly what is that bound?
Why is a sign-up alarm more important here than a request-count alarm?

---

## Step 68 — Data lifecycle, backups, teardown
**Branch:** `step-68-data-lifecycle` · **est 2h**

**You will have:** TTL verified in the console (`expiresAt` enabled; a ghost from dev older than
30 days gone); `scripts/export-table.sh` (`aws dynamodb export-table-to-point-in-time` needs
PITR — instead use a scan-to-JSON script for our tiny table, stored to the private bucket);
`DELETE /api/me` (deletes profile, runs, battle logs; anonymizes ghosts/leaderboard rows to
"Retired hero") with a confirm dialog in settings; `scripts/teardown.sh dev|prod|all`
(empties site/assets/SAM artifact buckets → `sam delete` → deletes budgets stack if `all` → prints
`aws ce` cost for the month) with a typed confirmation prompt; `docs/data.md` (what we store, for how long, how to delete).

**Sensei notes:** "right to delete" is basic hygiene even for a hobby project; the teardown
script is your guarantee that abandoning the project costs $0.

**Verify:** run teardown against a scratch env `test` (deploy `Env=test` first, then tear it down); `aws cloudformation list-stacks` shows it gone.

**Commit:** `step-68: ttl verification, export, account deletion and teardown`

---

## Step 69 — Release process docs + Phase 5 review
**Branch:** `step-69-release-docs` · **est 3h**

**You will have:** `CHANGELOG.md` (Keep a Changelog format), semver policy in `docs/release.md`,
`docs/architecture.md` with Mermaid diagrams (system, end-turn sequence, table design) rendered on
GitHub, `docs/adr/0001…0013.md` copied from the ADR table (status: Accepted), `docs/ci-cd.md`
complete (pipelines, environments, rollback, secrets policy = none), `docs/reviews/phase-5.md`;
tag `phase-5-complete` and release `v0.5.0` to prod as the dress rehearsal.

**Verify:** a fresh reader can follow `docs/ci-cd.md` to ship a change to prod without asking you anything.

**Commit:** `step-69: release process, architecture docs, adrs and phase 5 review`

**Check your understanding:** What is the one-sentence promise your CI/CD makes to future-you?
