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
if echo "$DB_URL" | grep -qE '@dpg-[a-z0-9-]+([:/]|$)' \
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

exec java -jar \
  -Dserver.port="${PORT:-8080}" \
  -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE:-prod}" \
  app.jar
