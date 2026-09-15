#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "Error: .env file not found: $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${ALERT_EMAIL_TO:?ALERT_EMAIL_TO is not set in .env}"

PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"

RESULT=$(
  curl -fsS -G "${PROMETHEUS_URL}/api/v1/query" \
    --data-urlencode 'query=network_device_up{device!=""} == 0'
)

ALERTS=$(
  printf '%s' "$RESULT" |
    jq -r '
      .data.result[] |
      "Device: \(.metric.device // "unknown")\nHost: \(.metric.host // "unknown")"
    '
)

if [ -n "$ALERTS" ]; then
  MESSAGE="A monitored TCP device is DOWN.

${ALERTS}

Time: $(date '+%Y-%m-%dT%H:%M:%S%z')
"

  printf '%s\n' "$MESSAGE" |
    mail -s "[ALERT] TCP device down" "$ALERT_EMAIL_TO"

  echo "Alert email sent to ${ALERT_EMAIL_TO}."
else
  echo "No TCP device is down."
fi