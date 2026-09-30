#!/bin/sh
set -e

# Render linked Postgres injects DATABASE_URL (internal). Ưu tiên hơn DB_URL manual.
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

# Chuẩn hoá prefix JDBC
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

# Internal Render (dpg-xxx-a, không có .postgres.render.com): KHÔNG dùng SSL
# External Render (*.postgres.render.com): bắt buộc sslmode=require
# Match cả jdbc:...@dpg-... và jdbc:...//dpg-... (user/pass tách env)
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

# Log host để debug trên Render (không in password)
DB_HOST="$(echo "$DB_URL" | sed -E 's|^jdbc:postgresql://([^/@]+@)?([^:/]+).*|\2|')"
echo "[INFO] Database host: ${DB_HOST}"

# Migrate schema BEFORE Spring Boot so Hibernate validate doesn't race / timeout on free tier.
# Disable with FLYWAY_PRE_MIGRATE=false on Render if needed.
if [ "${FLYWAY_PRE_MIGRATE:-true}" = "true" ] && [ -x /opt/flyway/flyway ]; then
  echo "[INFO] Flyway pre-migrate (baseline 13)..."
  FLYWAY_ARGS="-url=${DB_URL} -locations=filesystem:/app/db/migration -baselineOnMigrate=true -baselineVersion=13 -connectRetries=15"
  if [ -n "${DB_USER:-}" ]; then
    FLYWAY_ARGS="$FLYWAY_ARGS -user=${DB_USER}"
  fi
  if [ -n "${DB_PASSWORD:-}" ]; then
    FLYWAY_ARGS="$FLYWAY_ARGS -password=${DB_PASSWORD}"
  fi
  # shellcheck disable=SC2086
  /opt/flyway/flyway $FLYWAY_ARGS migrate
  echo "[INFO] Flyway pre-migrate done"
fi

# Render free/starter (~512MB): không set -Xmx dễ OOM (exit 137) trước khi bind PORT
# Override bằng JAVA_OPTS trên Dashboard nếu nâng plan
JAVA_OPTS="${JAVA_OPTS:--XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -Xss512k}"
echo "[INFO] JAVA_OPTS=${JAVA_OPTS}"

# shellcheck disable=SC2086
exec java ${JAVA_OPTS} -jar \
  -Dserver.address=0.0.0.0 \
  -Dserver.port="${PORT:-8080}" \
  -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE:-prod}" \
  app.jar
