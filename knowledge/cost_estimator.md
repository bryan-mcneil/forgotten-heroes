# Cost Estimator — Forgotten Heroes on AWS

> Goal: **as close to $0/month as possible**, with hard guardrails so a bug or a surprise traffic
> spike cannot produce a scary bill. Prices are **us-east-1, October 2026**, from the official
> pricing pages (links at the bottom). Everything is on-demand; nothing runs 24/7.

---

## 1. Your AWS account: the credit-based Free plan (answered 7 Oct 2026)

AWS changed the Free Tier on **15 July 2025**. Your account was created **≈ Aug/Sep 2026** (Q1), so it is
on the new model:

| Fact | What it means for us |
|---|---|
| **$100 credits** at sign-up, up to **$100 more** for onboarding activities | our usage is pennies; the credits will barely move |
| The **Free plan** ends when the credits run out **or after 6 months** (≈ Feb/Mar 2027); then a **90-day grace** period; then the account is **closed and its resources deleted** unless you upgrade | the one unavoidable production action: **upgrade to the Paid plan two weeks before expiry** (decided, Q26). Step 06 sets the calendar reminder; Step 77 checks it is there |
| On the Free plan AWS **cannot bill you beyond the credits**; a few expensive services are blocked (Marketplace, dedicated hardware) — nothing we use | until expiry you have a hard spending cap for free. After the upgrade the **kill switch** (Step 67) and the budgets take over that job |
| **Always Free** tiers (Lambda 1M requests, DynamoDB 25 GB, CloudFront 1 TB, Cognito 10k MAU…) exist on **both** plans | the ≈ $0.03/month estimate below holds after the upgrade too |

Check it any time: Billing console → **Free Tier** page (plan, credits left, expiry date).

---

## 2. Price list for every service we touch

| Service | Unit price | Always-free allowance (per month, forever) |
|---|---|---|
| **Lambda** (x86) | $0.20 per 1M requests · $0.0000166667 per GB-second | **1M requests + 400,000 GB-s** |
| **Lambda SnapStart (Java)** | **$0** extra (Python/.NET pay; Java does not) | — |
| **API Gateway HTTP API** | **$1.00 per 1M** calls (first 300M) | none permanent (1M/month for 12 months only on new accounts) |
| **DynamoDB on-demand** | **$0.625 per 1M write units** (1 unit = 1 KB) · **$0.125 per 1M read units** (1 unit = 4 KB eventually-consistent reads are half) · $0.25 per GB-month | **25 GB storage** |
| **Cognito** (Essentials/Lite) | $0.015 per MAU above the free tier | **10,000 monthly active users** |
| **S3 Standard** | $0.023 per GB-month · $0.005 per 1,000 PUT · $0.0004 per 1,000 GET | none permanent (5 GB for 12 months on new accounts) |
| **CloudFront** | $0.085/GB beyond free · ~$0.01 per 10,000 requests beyond free | **1 TB transfer + 10M requests** |
| **CloudFront geo-restriction · AWS Shield Standard** | $0 — included with CloudFront / API Gateway | — |
| **SSM Parameter Store** (standard parameters, e.g. the origin-verify secret) | $0 | — |
| **Data transfer out (regions)** | $0.09/GB beyond free | **100 GB** |
| **CloudWatch** | Logs $0.50/GB ingested, $0.03/GB stored · metrics $0.30 each beyond free · alarms $0.10 each beyond free | **5 GB logs, 10 custom metrics, 10 alarms, 3 dashboards, 1M API requests** |
| **AWS Budgets** | $0 | first 2 action-enabled budgets free; plain alert budgets free |
| **SNS** (email alerts) | $0 | 1,000 email notifications |
| **ACM certificate** | **$0** (public certs are free) | — |
| **SSM Parameter Store** (standard) | **$0** | 10,000 parameters |
| **GitHub** (public repo) | **$0** | unlimited Actions minutes on public repos |
| **Hostinger Business** | already paid | studio site hosting + DNS |

Things we **deliberately avoid** because they have a fixed monthly cost:

| Avoided | Why / cost |
|---|---|
| Route 53 hosted zone | $0.50/month per zone → use Hostinger DNS instead |
| AWS WAF | $5/month per Web ACL + $1/rule → use API Gateway throttling |
| NAT Gateway / VPC | ~$32/month + per-GB → Lambda stays outside any VPC |
| RDS / Aurora Serverless v2 | ≥ $13–45/month idle → DynamoDB |
| Application Load Balancer | ~$16/month → API Gateway |
| Lambda Provisioned Concurrency | ~$10+/month per 1 GB instance → SnapStart (free) |
| Secrets Manager | $0.40/secret/month → SSM Parameter Store standard tier |
| ECR container images | storage $0.10/GB + slower cold starts → zip deployment |
| DynamoDB Point-in-Time Recovery | $0.20/GB-month → on-demand backups only if ever needed |
| Extra CloudWatch dashboards | $3 each beyond 3 → one dashboard |
| X-Ray tracing on every request | beyond 100k traces/month → off, or 5% sampling |
| Cognito **Plus** tier (advanced security) | $0.05/MAU, no free tier → Essentials |

---

## 3. Usage model (what one game costs the cloud)

Assumptions (measured later in Step 22's simulator, tuned in Phase 6):

| Quantity | Estimate |
|---|---|
| Turns per run | 12 (10 wins needed, 5 lives) |
| API calls per turn | 15 (roll ×3, buy ×3, sell ×1, reorder ×3, freeze ×1, get state ×3, end turn ×1) |
| API calls per run | **~180** |
| Lambda duration per call (warm, 1024 MB) | 0.08 s → **0.08 GB-s** |
| Lambda duration per cold restore (SnapStart) | ~0.5 s → 0.5 GB-s, maybe once per visitor session |
| Run state item size | ~8 KB → **8 write units per save**, 2 read units per load |
| DynamoDB units per run | ~1,500 writes, ~400 reads |
| Static download per new visitor | ~4 MB (JS 0.4 MB + atlases 1.5 MB + audio sprite 2 MB) |
| Log volume per call | ~1 KB |

---

## 4. Scenarios

### Scenario A — Portfolio reality (≈50 visitors, 20 complete runs per month)
| Service | Usage | Cost |
|---|---|---|
| Lambda | 3,600 requests · ~320 GB-s | **$0** (free tier) |
| API Gateway | 3,600 calls | **$0.004** |
| DynamoDB | 30k writes · 8k reads · <0.1 GB | **$0.02** |
| Cognito | ~30 MAU | **$0** |
| S3 | 60 MB stored · ~200 PUTs on deploys | **$0.002** |
| CloudFront | 200 MB · 20k requests | **$0** |
| CloudWatch | 4 MB logs · 1 dashboard · 4 alarms | **$0** |
| **Total** | | **≈ $0.03 / month** (round to $0) |

### Scenario B — A good month (1,000 players, 5 runs each = 5,000 runs)
| Service | Usage | Cost |
|---|---|---|
| Lambda | 900k requests · ~75k GB-s | **$0** (still inside 1M / 400k) |
| API Gateway | 900k calls | **$0.90** |
| DynamoDB | 7.5M writes ($4.69) · 2M reads ($0.25) · 1 GB | **$4.94** |
| Cognito | 1,000 MAU | **$0** |
| CloudFront | 4 GB · 2M requests | **$0** |
| CloudWatch | 0.9 GB logs | **$0** |
| **Total** | | **≈ $6 / month** |

Note what dominates: **DynamoDB writes**, because every action saves an 8 KB item. Two cheap
optimizations if this ever matters: (1) keep the state item compact (short keys, no nulls) to
~3 KB → cost drops ~60%; (2) SAP's "commit the whole shop phase at once" pattern → 1 write per
turn instead of 15.

### Scenario C — Viral spike (20,000 MAU, 100k runs) — the "what's my worst day" case

> With the Step 61/67 guardrails this scenario cannot actually run its course: the sign-up canary trips at
> 200 sign-ups/day and the kill switch stops compute, sign-ups and the CDN. The numbers below are the
> *unguarded* worst case, kept so you can see why the guardrails exist.
| Service | Usage | Cost |
|---|---|---|
| Lambda | 18M requests ($3.40) · 1.5M GB-s ($18) | **$21** |
| API Gateway | 18M calls | **$18** |
| DynamoDB | 150M writes ($94) · 40M reads ($5) · 10 GB | **$99** |
| Cognito | 20k MAU → 10k billable × $0.015 | **$150** |
| CloudFront | 80 GB · 40M requests | **~$3** |
| CloudWatch | 18 GB logs | **~$7** |
| **Total** | | **≈ $300 / month** — and Cognito is half of it |

Scenario C will not happen to a portfolio piece, but the guardrails below make sure that even if
it did, you would get e-mails at $5 and the API would **throttle itself** long before $300.

---

## 5. Guardrails (built into the plan, not optional)

1. **AWS Budgets** (Step 06): monthly budget **$5** with alerts at 50 % / 80 % / 100 % and a
   *forecasted* alert; a second budget at **$20** as the "something is wrong" alarm. E-mail via SNS to
   the account owner's e-mail (kept out of the public repo).
2. **Free plan cap** (Q1): until ≈ Feb/Mar 2027 AWS cannot bill beyond your credits at all. Step 06 records
   the expiry and sets the reminder to upgrade; after the upgrade the items below are the cap.
3. **Lambda reserved concurrency = 10** (Step 67): caps simultaneous executions, which caps both
   Lambda and DynamoDB spend and protects against runaway loops.
4. **API Gateway stage throttling** (Step 36): 20 requests/second steady, burst 50. A human player
   makes ~1 request every few seconds.
5. **CloudFront in front of the API + US-only geo-restriction + origin-verify header** (Step 61, Q4): the
   only public entry point is the CloudFront domain; traffic from outside the US (VPN exits included — Q27) is
   answered at the edge with the static `blocked.html`, for free; the raw API URL answers 403. **AWS Shield Standard** (free, automatic) absorbs network-layer floods.
6. **Cost kill switch** (Step 67): CloudWatch alarms (Lambda invocations > 50k/day, Cognito sign-ups >
   200/day) and the $20 budget all notify one SNS topic; a tiny Lambda sets API concurrency to **0**, turns
   sign-ups **off** and **disables** the CloudFront distribution, then e-mails you. Worst case is bounded to a
   few hours of a 10-concurrency Lambda plus ≤ 200 new Cognito users — single-digit dollars.
7. **DynamoDB**: on-demand mode, **TTL** on ghosts (30 days) and battle logs (7 days), no GSIs
   unless a documented access pattern needs one, no PITR.
8. **CloudWatch Logs retention 14 days**, request-summary logging only (no debug logs in prod).
9. **Zero fixed-cost resources**: the SAM template is reviewed against the "avoid" list above
   before every prod deploy (checklist in Step 77).
10. **`scripts/teardown.sh`**: one command (`sam delete` for both stacks + empty buckets) returns the
    account to $0 if you ever shelve the project.
11. **Cost allocation tag** `project=forgotten-heroes` on every resource → Cost Explorer filter.
12. **Monthly 5-minute ritual**: open Billing → Bills, confirm < $1, screenshot into `docs/costs/`.
    (Recruiters like seeing a real "this runs for $0.03/month" receipt.)

---

## 6. One-time / non-AWS costs

| Item | Cost |
|---|---|
| Domain (if not already included with Hostinger) | ~$10–15/year, optional |
| Time Fantasy assets | already owned |
| Audio library | already owned (verify licences — question #6) |
| Developer tools (JDK, Node, Docker Desktop personal, VS Code, SAM, AWS CLI) | $0 |
| GitHub | $0 (public repo) |

**Bottom line:** expect **$0–$1 per month** while this is a portfolio project, with e-mail alerts
long before anything could reach $5.

---

## 7. Sources
* Lambda pricing & free tier: https://aws.amazon.com/lambda/pricing/ · SnapStart (Java no charge): https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html
* API Gateway pricing: https://aws.amazon.com/api-gateway/pricing/
* DynamoDB on-demand: https://aws.amazon.com/dynamodb/pricing/on-demand/
* Cognito pricing: https://aws.amazon.com/cognito/pricing/
* CloudFront free tier: https://aws.amazon.com/free/networking/ · S3 pricing: https://aws.amazon.com/s3/pricing/
* CloudWatch pricing: https://aws.amazon.com/cloudwatch/pricing/ · Budgets pricing: https://aws.amazon.com/aws-cost-management/aws-budgets/pricing/
* AWS Free Tier changes (Jul 2025): https://aws.amazon.com/free/ · announcement: https://aws.amazon.com/about-aws/whats-new/2025/07/aws-free-tier-credits-month-free-plan/ · hands-on: https://dev.classmethod.jp/en/articles/try-new-aws-free-tier-2025/ and https://dev.to/aws-builders/aws-free-tier-2025-how-to-get-200-in-credits-and-what-changed-3cj0
* WAF pricing: https://aws.amazon.com/waf/pricing/ · GitHub Actions pricing: https://docs.github.com/en/billing/managing-billing-for-github-actions/about-billing-for-github-actions
