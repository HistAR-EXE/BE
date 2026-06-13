#!/bin/sh
set -e

echo "Waiting for Postgres..."
until pg_isready -h postgres -U "${PGUSER}" -d "${PGDATABASE}" >/dev/null 2>&1; do
  sleep 2
done

run_sql() {
  file="$1"
  echo "Running ${file}..."
  psql -v ON_ERROR_STOP=1 -f "/seed/${file}"
}

# Schema + compat: docker-entrypoint-initdb.d (01, 02) trên volume mới.
# Script này chạy migration/seed còn lại (idempotent).
run_sql "2026-06-02_fe_compat_indexes_seed.sql"
run_sql "2026-06-02_fe_compat_data_topup.sql"
run_sql "2026-week3_update_panoramas_cu_chi.sql"
run_sql "2026-06-11_cp3_gamification_upgrade.sql"
run_sql "2026-06-11_admin_seed.sql"
run_sql "2026-06-11_unlock_rules.sql"
run_sql "2026-06-12_photo_scene_unlock_keys.sql"
run_sql "2026-06-12_discovery_artifact_links.sql"
run_sql "2026-06-12_quest_completion_trigger.sql"
run_sql "2026-06-12_value_layer_upgrade.sql"
run_sql "2026-06-13_phase_b_upgrade.sql"
run_sql "2026-06-14_phase1_hardening.sql"
run_sql "2026-06-15_visit_session_event_snapshot.sql"
run_sql "2026-06-16_analytics_event_metadata.sql"

echo "Postgres seed completed."
