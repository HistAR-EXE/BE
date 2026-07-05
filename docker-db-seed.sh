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
run_sql "2026-06-17_cu_chi_artifacts_story_admin.sql"
run_sql "2026-06-18_cu_chi_only_cleanup.sql"
run_sql "2026-06-19_heritage_sites_from_dataset.sql"
run_sql "2026-06-20_fix_den_hung_vuong_name.sql"
run_sql "2026-06-21_heritage_quests_seed.sql"
run_sql "2026-06-22_fix_quest_progress_current_step.sql"
run_sql "2026-06-23_quest_mission_keys.sql"
run_sql "2026-06-24_quest_visual_artifact_keys.sql"
run_sql "2026-06-25_quest_mixed_visual_pipeline.sql"
run_sql "2026-06-26_heritage_quest_discovery_points.sql"
run_sql "2026-06-27_heritage_p2_checkin_bonus.sql"
run_sql "2026-06-28_cu_chi_streetview_panoramas.sql"
run_sql "2026-06-29_fix_cu_chi_panorama_utf8.sql"
run_sql "2026-06-29_cu_chi_panorama_jpg_ext.sql"
run_sql "2026-06-30_cu_chi_supplementary_assets.sql"
run_sql "2026-07-01_cu_chi_real_panoramas.sql"
run_sql "2026-07-04_location_unlock.sql"
run_sql "2026-07-04_profile_tier.sql"
run_sql "2026-07-04_org_members_seed.sql"
run_sql "2026-07-05_ensure_admin_accounts.sql"
run_sql "2026-07-06_org_rbac_and_groups.sql"

echo "Postgres seed completed."
