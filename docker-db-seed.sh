#!/bin/sh
set -e

export PGCLIENTENCODING=UTF8

echo "Waiting for Postgres..."
until pg_isready -h postgres -U "${PGUSER}" -d "${PGDATABASE}" >/dev/null 2>&1; do
  sleep 2
done

# The Postgres init directory is skipped when an existing named volume is
# reused. Ensure RAG's vector type exists for both fresh and existing volumes.
psql -v ON_ERROR_STOP=1 -c 'CREATE EXTENSION IF NOT EXISTS vector;'

run_sql() {
  file="$1"
  echo "Running ${file}..."
  psql -v ON_ERROR_STOP=1 -f "/seed/${file}"
}

if [ ! -f /seed-manifest.txt ]; then
  echo "Missing /seed-manifest.txt"
  exit 1
fi

while IFS= read -r line || [ -n "$line" ]; do
  line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
  case "$line" in
    ''|'#'*) continue ;;
    *) run_sql "$line" ;;
  esac
done < /seed-manifest.txt

echo "Postgres seed completed."
