# FK / ON DELETE cascade audit (HistAR CP4 resilience)

> **Scope:** document current schema behavior for org/group teardown. **No delete-org/group API** exists in BE at CP4 — integration delete tests deferred until product adds endpoints.

## organization_members

| FK | ON DELETE | Source |
|----|-----------|--------|
| `organization_id → organizations(id)` | **NO ACTION** (default) | `2026-06-12_value_layer_upgrade.sql` |
| `user_id → profiles(id)` | **NO ACTION** (default) | same |

**Implication:** deleting an `organizations` row without clearing members first will fail at DB level. Deleting a `profiles` row with active membership likewise fails.

## study_groups / study_group_members

| FK | ON DELETE | Source |
|----|-----------|--------|
| `study_groups.created_by → profiles(id)` | **NO ACTION** | `2026-07-06_org_rbac_and_groups.sql` |
| `study_group_members.group_id → study_groups(id)` | **CASCADE** | same |
| `study_group_members.user_id → profiles(id)` | **NO ACTION** | same |

**Implication:** deleting a group cascades to members; deleting creator profile does not auto-delete groups.

## user_quest_progress

| FK | ON DELETE | Source |
|----|-----------|--------|
| `user_id → profiles(id)` | **CASCADE** | `TimeLens_DB_Schema.sql` |
| `quest_id → quests(id)` | **CASCADE** | same |

## lms_assignments

No dedicated `lms_assignments` table in current migrations. LMS progress is modeled via `user_quest_progress`, group quest assignment flags on `study_groups`, and roster APIs. **Deferred:** add FK audit row when LMS assignment table lands.

## profiles.org_id

| FK | ON DELETE | Source |
|----|-----------|--------|
| `profiles.org_id → organizations(id)` | **NO ACTION** | `2026-07-06_org_rbac_and_groups.sql` |

## Test policy

| Scenario | Status |
|----------|--------|
| Delete org API + FK integrity | **Manual / Deferred** — API not implemented |
| Delete group API + member cascade | **Manual / Deferred** — API not implemented |
| Concurrent seat cap (trial) | `OrgMembershipConcurrencyTest` (CAP-CONC-01) |
| Concurrent group code uniqueness | `GroupCodeConcurrencyTest` (CAP-CONC-02) |

## Recommended future work (post-CP4)

1. When `DELETE /api/org/{id}` ships: assert `organization_members` cleaned or blocked explicitly before org delete.
2. When `DELETE /api/groups/{id}` ships: assert `study_group_members` removed via CASCADE.
3. Consider `ON DELETE SET NULL` on `profiles.org_id` if soft-delete org is preferred over hard delete.
