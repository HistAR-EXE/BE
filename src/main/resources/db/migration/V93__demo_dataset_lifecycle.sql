-- Demo lifecycle metadata deliberately lives in the demo database only at runtime.
-- The functions below refuse arbitrary profile IDs: targets must be active rows in
-- demo_account_registry, and the actor must be an active ADMIN profile.

CREATE TABLE demo_dataset_state (
    id VARCHAR(50) PRIMARY KEY,
    admin_id UUID,
    generation INTEGER NOT NULL DEFAULT 1,
    initialized_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_demo_dataset_id CHECK (id = 'timelens-demo-v1')
);

CREATE TABLE demo_account_registry (
    user_id UUID PRIMARY KEY,
    dataset_id VARCHAR(50) NOT NULL REFERENCES demo_dataset_state(id),
    persona SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_demo_persona CHECK (persona BETWEEN 1 AND 10),
    CONSTRAINT uq_demo_dataset_persona UNIQUE (dataset_id, persona)
);

CREATE TABLE demo_organization_registry (
    organization_id UUID PRIMARY KEY,
    dataset_id VARCHAR(50) NOT NULL REFERENCES demo_dataset_state(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE demo_management_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_user_id UUID,
    reason VARCHAR(500) NOT NULL DEFAULT 'System demo lifecycle operation',
    detail JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION demo_delete_by_ids(
    p_table TEXT,
    p_column TEXT,
    p_ids UUID[]
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    affected BIGINT := 0;
BEGIN
    IF to_regclass('public.' || p_table) IS NULL OR NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = p_table AND column_name = p_column
    ) THEN
        RETURN 0;
    END IF;
    EXECUTE format('DELETE FROM %I WHERE %I = ANY ($1)', p_table, p_column)
        USING p_ids;
    GET DIAGNOSTICS affected = ROW_COUNT;
    RETURN affected;
END;
$$;

CREATE OR REPLACE FUNCTION demo_delete_children(
    p_child_table TEXT,
    p_child_fk TEXT,
    p_parent_table TEXT,
    p_parent_pk TEXT,
    p_parent_user_column TEXT,
    p_ids UUID[]
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    affected BIGINT := 0;
BEGIN
    IF to_regclass('public.' || p_child_table) IS NULL
       OR to_regclass('public.' || p_parent_table) IS NULL THEN
        RETURN 0;
    END IF;
    EXECUTE format(
        'DELETE FROM %I child USING %I parent '
        || 'WHERE child.%I = parent.%I AND parent.%I = ANY ($1)',
        p_child_table, p_parent_table, p_child_fk, p_parent_pk, p_parent_user_column)
        USING p_ids;
    GET DIAGNOSTICS affected = ROW_COUNT;
    RETURN affected;
END;
$$;

CREATE OR REPLACE FUNCTION demo_operational_row_count(p_target_user_id UUID DEFAULT NULL)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    ids UUID[];
    total BIGINT := 0;
    item RECORD;
    amount BIGINT;
BEGIN
    SELECT array_agg(user_id ORDER BY persona) INTO ids
    FROM demo_account_registry
    WHERE deleted_at IS NULL
      AND (p_target_user_id IS NULL OR user_id = p_target_user_id);
    IF ids IS NULL THEN
        RETURN 0;
    END IF;

    FOR item IN SELECT * FROM (VALUES
        ('profiles', 'id'),
        ('checkins', 'user_id'),
        ('user_discoveries', 'user_id'),
        ('user_artifacts', 'user_id'),
        ('user_badges', 'user_id'),
        ('user_quest_progress', 'user_id'),
        ('conversations', 'user_id'),
        ('visit_sessions', 'user_id'),
        ('analytics_events', 'user_id'),
        ('xp_ledger', 'user_id'),
        ('b2c_payment_transactions', 'user_id'),
        ('org_payment_transactions', 'requester_user_id'),
        ('assignments', 'teacher_id'),
        ('assignment_submissions', 'student_id')
    ) AS rows(table_name, column_name)
    LOOP
        IF to_regclass('public.' || item.table_name) IS NOT NULL AND EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = item.table_name
              AND column_name = item.column_name
        ) THEN
            EXECUTE format('SELECT count(*) FROM %I WHERE %I = ANY ($1)',
                           item.table_name, item.column_name)
                INTO amount USING ids;
            total := total + amount;
        END IF;
    END LOOP;
    RETURN total;
END;
$$;

CREATE OR REPLACE FUNCTION purge_demo_dataset(
    p_actor_id UUID,
    p_target_user_id UUID,
    p_reason TEXT
) RETURNS TABLE(deleted_accounts INTEGER, deleted_rows BIGINT)
LANGUAGE plpgsql
AS $$
DECLARE
    ids UUID[];
    org_ids UUID[];
    expected_count INTEGER;
    affected BIGINT := 0;
BEGIN
    IF COALESCE(length(trim(p_reason)), 0) < 10 THEN
        RAISE EXCEPTION 'A purge reason of at least 10 characters is required';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM profiles
        WHERE id = p_actor_id
          AND UPPER(TRIM(COALESCE(role, ''))) = 'ADMIN'
          AND COALESCE(account_status, 'ACTIVE') = 'ACTIVE'
    ) THEN
        RAISE EXCEPTION 'Only an active admin may purge demo data';
    END IF;

    SELECT array_agg(user_id ORDER BY persona), count(*)
      INTO ids, expected_count
    FROM demo_account_registry
    WHERE deleted_at IS NULL
      AND (p_target_user_id IS NULL OR user_id = p_target_user_id);
    IF ids IS NULL OR expected_count = 0 THEN
        RAISE EXCEPTION 'No active demo account matched the requested target';
    END IF;

    -- Organizations are removed only for a full dataset purge. Deleting one
    -- persona must not remove a shared classroom or institution.
    IF p_target_user_id IS NULL THEN
        SELECT array_agg(organization_id) INTO org_ids
        FROM demo_organization_registry
        WHERE dataset_id = 'timelens-demo-v1' AND deleted_at IS NULL;
    END IF;

    -- Children that do not carry a direct user_id must go first.
    affected := affected + demo_delete_children('messages', 'conversation_id', 'conversations', 'id', 'user_id', ids);
    affected := affected + demo_delete_children('visit_session_events', 'visit_session_id', 'visit_sessions', 'id', 'user_id', ids);
    affected := affected + demo_delete_children('assignment_submissions', 'assignment_id', 'assignments', 'id', 'teacher_id', ids);
    affected := affected + demo_delete_children('field_trip_report_snapshots', 'field_trip_id', 'field_trips', 'id', 'created_by', ids);
    affected := affected + demo_delete_children('study_group_members', 'group_id', 'study_groups', 'id', 'created_by', ids);
    affected := affected + demo_delete_children('squad_members', 'squad_id', 'squads', 'id', 'leader_user_id', ids);

    -- Direct operational ownership columns. Missing optional tables/columns are safe.
    affected := affected + demo_delete_by_ids('assignment_submissions', 'student_id', ids);
    affected := affected + demo_delete_by_ids('assignments', 'teacher_id', ids);
    affected := affected + demo_delete_by_ids('study_group_members', 'user_id', ids);
    affected := affected + demo_delete_by_ids('study_groups', 'created_by', ids);
    affected := affected + demo_delete_by_ids('squad_members', 'user_id', ids);
    affected := affected + demo_delete_by_ids('squads', 'leader_user_id', ids);
    affected := affected + demo_delete_by_ids('field_trips', 'created_by', ids);

    FOR deleted_rows IN SELECT sum(demo_delete_by_ids(table_name, column_name, ids))
    FROM (VALUES
        ('refresh_tokens', 'user_id'), ('email_verification_tokens', 'user_id'),
        ('password_reset_challenges', 'user_id'), ('device_push_tokens', 'user_id'),
        ('ar_card_mail_attempts', 'user_id'), ('passport_mail_attempts', 'user_id'),
        ('analytics_events', 'user_id'), ('visit_sessions', 'user_id'),
        ('checkins', 'user_id'), ('user_discoveries', 'user_id'),
        ('user_artifacts', 'user_id'), ('user_badges', 'user_id'),
        ('user_quest_progress', 'user_id'), ('user_creations', 'user_id'),
        ('user_secret_unlocks', 'user_id'), ('user_heritage_onsite_bonus', 'user_id'),
        ('xp_ledger', 'user_id'), ('explore_attempts', 'user_id'),
        ('station_minigame_scores', 'user_id'), ('user_game_results', 'user_id'),
        ('quest_reward_ledger', 'user_id'), ('journey_memories', 'user_id'),
        ('user_narrative_investigation_progress', 'user_id'),
        ('org_active_sessions', 'user_id'), ('organization_members', 'user_id'),
        ('site_memberships', 'user_id'), ('site_onboarding_requests', 'requester_id'),
        ('organization_ar_cards', 'user_id'), ('organization_certificates', 'user_id'),
        ('teacher_ai_drafts', 'teacher_id'), ('pilot_events', 'user_id'),
        ('referral_visits', 'user_id'), ('referral_codes', 'user_id'),
        ('cooperative_leaderboard_access_audit', 'user_id'),
        ('content_report_cases', 'created_by'), ('narrative_investigation_reviews', 'reviewer_id'),
        ('usage_quotas', 'user_id'), ('b2c_renewal_reminders', 'user_id'),
        ('b2c_payment_transactions', 'user_id'), ('b2c_visit_entitlements', 'user_id'),
        ('b2c_subscriptions', 'user_id'), ('subscriptions', 'user_id'),
        ('org_payment_transactions', 'requester_user_id'),
        ('conversations', 'user_id')
    ) AS direct_rows(table_name, column_name)
    LOOP
        affected := affected + COALESCE(deleted_rows, 0);
    END LOOP;

    -- Nullable audit ownership is retained without retaining a demo identity.
    UPDATE media_transcode_jobs SET user_id = NULL WHERE user_id = ANY (ids);
    GET DIAGNOSTICS deleted_rows = ROW_COUNT;
    affected := affected + deleted_rows;
    UPDATE site_content_versions SET submitted_by = NULL WHERE submitted_by = ANY (ids);
    UPDATE site_content_versions SET reviewed_by = NULL WHERE reviewed_by = ANY (ids);
    UPDATE site_content_versions SET published_by = NULL WHERE published_by = ANY (ids);
    UPDATE site_workspace_settings SET updated_by = NULL WHERE updated_by = ANY (ids);

    -- Demo-created drafts are isolated copies and may be removed; approved base content
    -- is owned by the real admin during seeding and therefore is not selected here.
    affected := affected + demo_delete_by_ids('site_content_versions', 'created_by', ids);

    IF org_ids IS NOT NULL THEN
        affected := affected + demo_delete_by_ids('field_trip_report_snapshots', 'field_trip_id',
            ARRAY(SELECT id FROM field_trips WHERE org_id = ANY (org_ids)));
        affected := affected + demo_delete_by_ids('assignment_submissions', 'assignment_id',
            ARRAY(SELECT id FROM assignments WHERE org_id = ANY (org_ids)));
        affected := affected + demo_delete_by_ids('assignments', 'org_id', org_ids);
        affected := affected + demo_delete_by_ids('study_group_members', 'group_id',
            ARRAY(SELECT id FROM study_groups WHERE org_id = ANY (org_ids)));
        affected := affected + demo_delete_by_ids('study_groups', 'org_id', org_ids);
        affected := affected + demo_delete_by_ids('field_trips', 'org_id', org_ids);
        affected := affected + demo_delete_by_ids('org_active_sessions', 'org_id', org_ids);
        affected := affected + demo_delete_by_ids('org_payment_transactions', 'organization_id', org_ids);
        affected := affected + demo_delete_by_ids('organization_ar_cards', 'organization_id', org_ids);
        affected := affected + demo_delete_by_ids('organization_certificates', 'organization_id', org_ids);
        affected := affected + demo_delete_by_ids('organization_contracts', 'organization_id', org_ids);
        affected := affected + demo_delete_by_ids('organization_members', 'organization_id', org_ids);
        affected := affected + demo_delete_by_ids('subscriptions', 'organization_id', org_ids);
        affected := affected + demo_delete_by_ids('usage_quotas', 'organization_id', org_ids);
        affected := affected + demo_delete_by_ids('heritage_digitization_inquiries', 'organization_id', org_ids);
    END IF;

    DELETE FROM profiles WHERE id = ANY (ids);
    GET DIAGNOSTICS deleted_rows = ROW_COUNT;
    IF deleted_rows <> expected_count THEN
        RAISE EXCEPTION 'Profile deletion count mismatch: expected %, got %', expected_count, deleted_rows;
    END IF;
    affected := affected + deleted_rows;

    IF org_ids IS NOT NULL THEN
        DELETE FROM organizations WHERE id = ANY (org_ids);
        GET DIAGNOSTICS deleted_rows = ROW_COUNT;
        affected := affected + deleted_rows;
        UPDATE demo_organization_registry
           SET deleted_at = NOW()
         WHERE organization_id = ANY (org_ids) AND deleted_at IS NULL;
    END IF;

    UPDATE demo_account_registry
       SET deleted_at = NOW()
     WHERE user_id = ANY (ids) AND deleted_at IS NULL;

    INSERT INTO demo_management_audit(actor_id, action, target_user_id, reason, detail)
    VALUES (
        p_actor_id,
        CASE WHEN p_target_user_id IS NULL THEN 'PURGE_ALL' ELSE 'PURGE_ONE' END,
        p_target_user_id,
        trim(p_reason),
        jsonb_build_object('deletedAccounts', expected_count, 'deletedRows', affected)
    );

    deleted_accounts := expected_count;
    deleted_rows := affected;
    RETURN NEXT;
END;
$$;
