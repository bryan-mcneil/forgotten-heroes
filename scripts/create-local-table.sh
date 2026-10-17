#!/usr/bin/env bash
# Creates the game's table in the DynamoDB that docker compose started. Safe to run twice.
set -euo pipefail
cd "$(dirname "$0")/.."
export AWS_ACCESS_KEY_ID=local AWS_SECRET_ACCESS_KEY=local
dynamo() { aws dynamodb --endpoint-url http://localhost:8000 --region us-east-1 "$@"; }
table=forgotten-heroes-local

if dynamo describe-table --table-name "$table" >/dev/null 2>&1; then
  echo "table $table already exists"
else
  dynamo create-table --cli-input-json file://infra/table.json >/dev/null
  echo "table $table created"
fi
