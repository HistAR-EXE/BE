#!/bin/sh
set -e

# Render linked Postgres injects DATABASE_URL (internal). Prefer over manual DB_URL.
if [ -n "$DATABASE_URL" ]; then
  case "$DATABASE_URL" in
    jdbc:*)
      export DB_URL="$DATABASE_URL"
      ;;
    postgresql://*|postgres://*)
      export DB_URL="jdbc:$DATABASE_URL"
      ;;
  esac
fi

case "$DB_URL" in
  jdbc:*|"") ;;
  postgresql://*|postgres://*)
    export DB_URL="jdbc:$DB_URL"
    ;;
esac

if [ -z "$DB_URL" ]; then
  echo "[ERROR] DB_URL hoặc DATABASE_URL chưa cấu hình."
  exit 1
fi

# Internal Render (dpg-xxx-a): no SSL. External (*.postgres.render.com): sslmode=require.
if echo "$DB_URL" | grep -qE '(^|[@/])dpg-[a-z0-9-]+([:/]|$)' \
  && ! echo "$DB_URL" | grep -q 'postgres.render.com'; then
  export DB_URL="$(echo "$DB_URL" | sed -E 's/[?&]sslmode=[^&]*//g; s/\?&/?/g; s/\?$//')"
  echo "[INFO] Postgres: Render internal (private network, no SSL)"
elif echo "$DB_URL" | grep -q 'postgres.render.com'; then
  case "$DB_URL" in
    *sslmode=*) ;;
    *\?*)
      export DB_URL="${DB_URL}&sslmode=require"
      ;;
    *)
      export DB_URL="${DB_URL}?sslmode=require"
      ;;
  esac
  echo "[INFO] Postgres: Render external (SSL required)"
else
  echo "[INFO] Postgres: custom host"
fi

DB_HOST="$(echo "$DB_URL" | sed -E 's|^jdbc:postgresql://([^/@]+@)?([^:/]+).*|\2|')"
echo "[INFO] Database host: ${DB_HOST}"

# Render free/starter (~512MB)
JAVA_OPTS="${JAVA_OPTS:--XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -Xss512k}"
echo "[INFO] JAVA_OPTS=${JAVA_OPTS}"

# shellcheck disable=SC2086
exec java ${JAVA_OPTS} -jar \
  -Dserver.address=0.0.0.0 \
  -Dserver.port="${PORT:-8080}" \
  -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE:-prod}" \
  app.jar
