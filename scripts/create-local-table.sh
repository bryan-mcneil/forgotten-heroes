#!/usr/bin/env bash
# Creates the game's table in the DynamoDB that docker compose started and switches TTL on. Safe to run twice.
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

# TTL: DynamoDB deletes an item on its own once the number in expiresAt (seconds since 1970) has passed.
if [ "$(dynamo describe-time-to-live --table-name "$table" --query TimeToLiveDescription.TimeToLiveStatus --output text)" = "ENABLED" ]; then
  echo "TTL on expiresAt already enabled"
else
  dynamo update-time-to-live --table-name "$table" --time-to-live-specification "Enabled=true,AttributeName=expiresAt" >/dev/null
  echo "TTL on expiresAt enabled"
fi
