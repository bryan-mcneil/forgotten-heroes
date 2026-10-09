# infra — infrastructure as code (AWS SAM)

## What lives here

- `template.yaml`: every AWS resource of one environment in one file — the Lambda function, the HTTP API
  with its JWT authorizer, the DynamoDB table, Cognito, the private site bucket and CloudFront, the
  alarms and the cost kill switch (v1 in Step 33, complete in Step 61).
- Around it: `samconfig.toml` (Step 34), `budgets.yaml` (Step 06), `github-oidc.yaml` (Step 62),
  `events/` with sample payloads for local invokes, and `github/branch-protection.json` (Step 05).
- The same template builds two stacks, `forgotten-heroes-dev` and `forgotten-heroes-prod`.

## What must NOT live here

- Anything with a fixed monthly cost: no WAF, NAT, VPC, Route 53 hosted zone or provisioned
  concurrency. The target is $0/month and the guardrails are `knowledge/cost_estimator.md` §5.
- Secrets or `.aws-sam/` build output (gitignored).
- Hand-made resources that stay. AWS is console-first (Q31): each resource is first clicked together on
  `dev` to learn it, then this template recreates it and the manual copy is deleted. Prod is never
  built by hand.

## How do I run it

TODO Step 33: `sam validate --lint` checks the template;
`sam local invoke ApiFunction -e infra/events/health.json` runs the function on your machine.
TODO Step 34: `scripts/deploy-backend.sh dev` deploys the dev stack.
