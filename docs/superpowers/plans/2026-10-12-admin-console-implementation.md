# Admin Console Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a standalone `/admin` console that relocates existing administrative pages, adds audited soft-delete/trash workflows, and safely reviews, versions, publishes, pauses, and resumes narrative pilots.

**Architecture:** Spring Boot owns durable audit, versioned narrative state, soft-delete gates, and all authorization. React owns a single administrative shell, URL-driven lists, forms, readiness explanations, and explicit confirmation flows; it never enables a deployment feature flag itself. Narrative graph edits are structured draft-version edits; public learners continue to read only the immutable published version.

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA, Flyway, PostgreSQL, React 19, TypeScript, React Router, Axios, Tailwind CSS, Playwright.

---

## File map

### Backend

- Create: `BE/src/main/resources/db/migration/V61__admin_console_audit_soft_delete_and_narrative_versions.sql` — durable audit table, soft-delete/audit/version columns, versioned narrative storage and constraints.
- Create: `BE/src/main/java/com/histar/be/admin/audit/AdminAuditEvent.java`, `AdminAuditEventRepository.java`, `AdminAuditService.java`, `AdminAuditEventResponse.java` — append-only general audit stream and paginated read model.
- Create: `BE/src/main/java/com/histar/be/admin/trash/AdminTrashController.java`, `AdminTrashService.java`, DTOs — cross-content trash listing and restore entry points.
- Create: `BE/src/main/java/com/histar/be/quest/narrative/entity/NarrativeInvestigationVersion.java`, `NarrativeInvestigationVersionRepository.java` — published/candidate graph versions.
- Create: `BE/src/main/java/com/histar/be/quest/narrative/admin/NarrativePilotAdminController.java`, `NarrativePilotAdminService.java`, request/response DTOs — pilot list/detail/readiness/draft/rollout/audit endpoints.
- Modify: `BE/src/main/java/com/histar/be/quest/narrative/entity/NarrativeInvestigation.java`, `NarrativeInvestigationReview.java`, review repository/service/controller, public narrative service/repository — retain compatibility while moving approval and public reads to explicit versions.
- Modify: discovery/artifact/quest entities, repositories, `AdminContentService`, and the three admin content controllers — soft delete, impact check, restore, audit and optimistic locking.
- Create tests under `BE/src/test/java/com/histar/be/admin/**` and `BE/src/test/java/com/histar/be/quest/narrative/admin/**`.

### Frontend

- Create: `FE/src/components/admin/AdminConsoleLayout.tsx`, `AdminConsoleSidebar.tsx`, `AdminConsoleHeader.tsx`, `AdminDataTable.tsx`, `AdminConfirmReasonDialog.tsx` — shared dark administrative workspace and accessible primitives.
- Create: `FE/src/pages/AdminDashboardPage.tsx`, `AdminNarrativePilotsPage.tsx`, `AdminNarrativePilotDetailPage.tsx`, `AdminTrashPage.tsx`, `AdminAuditPage.tsx`.
- Create: `FE/src/features/admin/narrativePilotApi.ts`, `trashApi.ts`, `auditApi.ts`, and domain types.
- Modify: `FE/src/routes/AppRoutes.tsx`, `FE/src/components/admin/AdminSubNav.tsx`, `FE/src/pages/AdminContentPage.tsx`, `FE/src/features/admin/api.ts`, and existing admin page wrappers to use the shell and soft-delete actions.
- Create: `FE/tests/api/admin-console.spec.ts`, `FE/tests/ui/admin-console.spec.ts`, `FE/tests/e2e/admin-narrative-pilot.spec.ts`.

## Task 1: Establish a red backend contract for durable audit and content deletion

**Files:**
- Create: `BE/src/test/java/com/histar/be/admin/audit/AdminAuditServiceTest.java`
- Create: `BE/src/test/java/com/histar/be/admin/trash/AdminTrashServiceTest.java`
- Modify: `BE/src/main/java/com/histar/be/admin/service/AdminContentService.java`
- Modify: discovery/artifact/quest entities and repositories listed in the file map.

- [ ] **Step 1: Write the failing audit-service test.**

  Assert that a mutation writes an immutable record containing actor, entity type/id, action, old/new row version, reason, idempotency key and timestamp:

  ```java
  @Test
  void appendsImmutableAuditEventForSoftDelete() {
      audit.append(new AdminAuditCommand(actorId, "QUEST", questId, "SOFT_DELETED",
          4L, 5L, "Sai nguồn", "delete-quest-1", Map.of("title", "Củ Chi")));

      var event = captor.getValue();
      assertThat(event.getAction()).isEqualTo("SOFT_DELETED");
      assertThat(event.getReason()).isEqualTo("Sai nguồn");
      assertThat(event.getOccurredAt()).isNotNull();
  }
  ```

- [ ] **Step 2: Run the audit test and verify it fails because `AdminAuditService` does not exist.**

  Run: `cd BE; .\mvnw.cmd -Dtest=AdminAuditServiceTest test`

  Expected: compilation failure naming the missing audit types.

- [ ] **Step 3: Write the failing soft-delete test.**

  Test that a quest with an active narrative pilot returns a business-rule error, and an unreferenced draft quest becomes hidden from the default list but remains restorable:

  ```java
  @Test
  void softDeleteBlocksQuestWithActivePilot() {
      when(pilotGate.hasActivePilot(QUEST_ID)).thenReturn(true);
      assertThatThrownBy(() -> service.softDeleteQuest(QUEST_ID, actor, deleteRequest("Lỗi kỹ thuật", 3L)))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("pilot active");
  }
  ```

- [ ] **Step 4: Run the soft-delete test and verify it fails because the service method does not exist.**

  Run: `cd BE; .\mvnw.cmd -Dtest=AdminTrashServiceTest test`

  Expected: compilation failure naming `softDeleteQuest`.

- [ ] **Step 5: Add migration and minimal persistence model.**

  Add `admin_audit_events`; add `deleted_at`, `deleted_by`, `delete_reason`, and `row_version` to `discovery_points`, `artifacts`, and `quests`; add indexes on `deleted_at`. Add a partial/unique-safe draft constraint only where PostgreSQL supports it. Map the row version with `@Version Long rowVersion` and use `Instant`/`UUID` fields for deletion metadata.

- [ ] **Step 6: Implement `AdminAuditService` and the minimal soft-delete/restore services.**

  Use a single append-only method:

  ```java
  void append(AdminAuditCommand command);
  ```

  The content service must: load the entity, reject stale `rowVersion`, calculate impact, reject active references, set deletion metadata, save, then append `SOFT_DELETED`. Restore must reject a deleted parent with `PARENT_DELETED`, resolve slug/unique conflicts before save, clear only deletion columns, and append `RESTORED`.

- [ ] **Step 7: Run focused tests and refactor only after green.**

  Run: `cd BE; .\mvnw.cmd -Dtest=AdminAuditServiceTest,AdminTrashServiceTest test`

  Expected: `BUILD SUCCESS`.

## Task 2: Expose audited trash and content mutation endpoints

**Files:**
- Modify: `BE/src/main/java/com/histar/be/admin/controller/AdminDiscoveryPointController.java`
- Modify: `BE/src/main/java/com/histar/be/admin/controller/AdminArtifactController.java`
- Modify: `BE/src/main/java/com/histar/be/admin/controller/AdminQuestController.java`
- Create: `BE/src/main/java/com/histar/be/admin/trash/AdminTrashController.java`
- Create: `BE/src/test/java/com/histar/be/admin/trash/AdminTrashControllerIT.java`

- [ ] **Step 1: Write controller integration tests before adding routes.**

  Cover `DELETE /api/admin/quests/{id}` with `{ reasonPreset, reasonDetail, rowVersion }`, `POST /restore`, paginated `GET /api/admin/trash`, and a non-admin `403`.

- [ ] **Step 2: Run the controller test and verify it fails with route-not-found.**

  Run: `cd BE; .\mvnw.cmd -Dtest=AdminTrashControllerIT test`

  Expected: endpoint mapping assertion fails with `404`.

- [ ] **Step 3: Add ADMIN-only routes and validation.**

  Require one of `Sai nguồn`, `Lỗi kỹ thuật`, `Trùng nội dung`, `Yêu cầu pháp lý`, or `Khác`; reject `Khác` with fewer than ten non-whitespace characters. Keep normal lists excluding deleted records and allow `includeDeleted=true` only to admins.

- [ ] **Step 4: Re-run controller tests.**

  Run: `cd BE; .\mvnw.cmd -Dtest=AdminTrashControllerIT test`

  Expected: `BUILD SUCCESS`.

## Task 3: Build versioned narrative pilot domain and readiness gates

**Files:**
- Create: `BE/src/test/java/com/histar/be/quest/narrative/admin/NarrativePilotAdminServiceTest.java`
- Create: `BE/src/main/java/com/histar/be/quest/narrative/entity/NarrativeInvestigationVersion.java`
- Create: `BE/src/main/java/com/histar/be/quest/narrative/NarrativeInvestigationVersionRepository.java`
- Modify: `BE/src/main/resources/db/migration/V61__admin_console_audit_soft_delete_and_narrative_versions.sql`
- Modify: narrative entities/repositories/services listed in the file map.

- [ ] **Step 1: Write failing service tests for the state machine.**

  Include exactly these cases: a second open draft returns `DRAFT_ALREADY_EXISTS`; approving does not publish; `enable(v1)` requires approved `vi`, a valid graph and full citation metadata; `publishDraft(v2)` atomically replaces active v1; `pause` requires a valid reason; `resume` only restores the paused published version; an incorrect draft revision returns conflict.

  ```java
  @Test
  void publishApprovedDraftAtomicallySwapsActiveVersion() {
      service.publishDraft(QUEST_ID, actor, new PublishDraftRequest(2, 7L, "publish-2"));
      assertThat(investigation.getPublishedVersion()).isEqualTo(2);
      assertThat(investigation.getRolloutState()).isEqualTo(RolloutState.ACTIVE);
      verify(audit).append(argThat(e -> e.action().equals("PUBLISHED_DRAFT")));
  }
  ```

- [ ] **Step 2: Run the service tests and verify they fail because versioned types/actions do not exist.**

  Run: `cd BE; .\mvnw.cmd -Dtest=NarrativePilotAdminServiceTest test`

  Expected: compilation failure for `NarrativePilotAdminService`/`PublishDraftRequest`.

- [ ] **Step 3: Add version storage and migrate existing published graphs.**

  Make `narrative_investigation_versions` store investigation id, numeric version, graph JSON, review status/locales/reviewer/timestamp, draft/published lifecycle, and row version. Migrate each existing `narrative_investigations.graph_json` into its published version. Preserve the public table as the compatibility pointer until all reads use `published_version`.

- [ ] **Step 4: Implement pilot state transitions and readiness output.**

  Implement methods shaped as:

  ```java
  NarrativePilotDetail createDraft(UUID questId, UUID actor, CreateDraftRequest request);
  NarrativePilotDetail updateDraft(UUID questId, int draftVersion, UUID actor, UpdateNarrativeDraftRequest request);
  NarrativeReadinessResponse readiness(UUID questId, Integer draftVersion);
  NarrativePilotDetail review(UUID questId, UUID actor, NarrativeReviewRequest request);
  NarrativePilotDetail enable(UUID questId, UUID actor, RolloutCommand request);
  NarrativePilotDetail pause(UUID questId, UUID actor, PausePilotRequest request);
  NarrativePilotDetail resume(UUID questId, UUID actor, RolloutCommand request);
  NarrativePilotDetail publishDraft(UUID questId, UUID actor, PublishDraftRequest request);
  ```

  Each mutation must check its idempotency key, lock/check its row version, append one general audit event, and leave public reads on the prior published graph until publish succeeds.

- [ ] **Step 5: Update public narrative reads.**

  `NarrativeInvestigationService.forQuest` must load only `ACTIVE` published versions, preserve user progress by published version, and never read an open draft.

- [ ] **Step 6: Run service and existing narrative tests.**

  Run: `cd BE; .\mvnw.cmd -Dtest=NarrativePilotAdminServiceTest,NarrativeContentReviewServiceTest,NarrativeInvestigationServiceTest test`

  Expected: `BUILD SUCCESS`.

## Task 4: Expose secure pilot, readiness, and general audit APIs

**Files:**
- Create: `BE/src/main/java/com/histar/be/quest/narrative/admin/NarrativePilotAdminController.java`
- Create: `BE/src/main/java/com/histar/be/admin/audit/AdminAuditController.java`
- Create: `BE/src/test/java/com/histar/be/quest/narrative/admin/NarrativePilotAdminControllerIT.java`
- Create: `BE/src/test/java/com/histar/be/admin/audit/AdminAuditControllerIT.java`

- [ ] **Step 1: Write failing MVC tests for all public contracts.**

  Assert `GET /api/admin/narrative-investigations` honors `reviewStatus`, `rolloutState`, `q`, `page`, `size`, `sort`; readiness accepts `draftVersion`; all mutations return `403` to non-admin; audit history is paginated and contains only general audit events once.

- [ ] **Step 2: Run the MVC tests and confirm mappings are absent.**

  Run: `cd BE; .\mvnw.cmd -Dtest=NarrativePilotAdminControllerIT,AdminAuditControllerIT test`

  Expected: mapping/assertion failures.

- [ ] **Step 3: Implement ADMIN-only controller contracts.**

  Add list/detail/readiness/draft/review/enable/pause/resume/publish/audit endpoints from the design spec. Preserve the pre-existing `/reviews` URL by delegating it to the new version-aware service. Use `Idempotency-Key` request header for rollout mutations.

- [ ] **Step 4: Re-run MVC tests.**

  Run: `cd BE; .\mvnw.cmd -Dtest=NarrativePilotAdminControllerIT,AdminAuditControllerIT test`

  Expected: `BUILD SUCCESS`.

## Task 5: Add the standalone Admin Console shell and relocate existing admin routes

**Files:**
- Create: `FE/src/components/admin/AdminConsoleLayout.tsx`
- Create: `FE/src/components/admin/AdminConsoleSidebar.tsx`
- Create: `FE/src/components/admin/AdminConsoleHeader.tsx`
- Create: `FE/src/pages/AdminDashboardPage.tsx`
- Modify: `FE/src/routes/AppRoutes.tsx`
- Modify: `FE/src/components/admin/AdminSubNav.tsx`
- Modify: existing `AdminUsersPage.tsx`, `AdminOrganizationsPage.tsx`, `AdminBillingPage.tsx`, `AdminAnalyticsPage.tsx`, `AdminContentPage.tsx`
- Create: `FE/tests/ui/admin-console.spec.ts`

- [ ] **Step 1: Write a failing Playwright UI test.**

  Authenticate as admin using the existing E2E setup, visit `/admin`, and assert grouped navigation labels, environment/role header badges, dashboard KPIs, and a visible mobile menu button at a mobile viewport.

- [ ] **Step 2: Run the test and confirm `/admin` is not available.**

  Run: `cd FE; npm run test:e2e:ui -- tests/ui/admin-console.spec.ts`

  Expected: route assertion fails or reaches 404.

- [ ] **Step 3: Implement shell routes and relocation.**

  Nest `/admin`, `/admin/content`, `/admin/narrative-pilots`, `/admin/users`, `/admin/organizations`, `/admin/billing`, `/admin/analytics`, `/admin/audit`, and `/admin/trash` beneath `AdminRoute`. Replace the old tab strip with the shell sidebar. Existing users/organizations/billing/analytics retain their data contracts; only layout/route framing changes. Preserve old URLs through `Navigate replace` redirects where a route name changes.

- [ ] **Step 4: Implement accessible shell behavior.**

  Sidebar groups are Vận hành, Quản lý and Giám sát; drawer closes after navigation on mobile; selected link has text, icon and semantic active state; dialogs/focus/toasts remain keyboard-accessible.

- [ ] **Step 5: Re-run the UI test and build.**

  Run: `cd FE; npm run test:e2e:ui -- tests/ui/admin-console.spec.ts; npm run build`

  Expected: the focused UI test and TypeScript/Vite build pass.

## Task 6: Build narrative pilot management UI

**Files:**
- Create: `FE/src/features/admin/narrativePilotApi.ts`
- Create: `FE/src/pages/AdminNarrativePilotsPage.tsx`
- Create: `FE/src/pages/AdminNarrativePilotDetailPage.tsx`
- Create: `FE/src/components/admin/NarrativeReadinessChecklist.tsx`
- Create: `FE/src/components/admin/NarrativeGraphDraftEditor.tsx`
- Create: `FE/src/components/admin/AdminConfirmReasonDialog.tsx`
- Create: `FE/tests/api/admin-console.spec.ts`
- Create: `FE/tests/e2e/admin-narrative-pilot.spec.ts`

- [ ] **Step 1: Write failing API and E2E tests.**

  API tests must exercise list filters and an unauthorized mutation. E2E must show a readiness failure explaining the failed gate, keep Enable disabled, create a draft, save a structured node edit, submit a review note of at least ten characters, and show a pause confirmation requiring a reason.

- [ ] **Step 2: Run the tests to prove red.**

  Run: `cd FE; npm run test:e2e:api -- tests/api/admin-console.spec.ts; npm run test:e2e:full -- tests/e2e/admin-narrative-pilot.spec.ts`

  Expected: missing API client/page route selectors.

- [ ] **Step 3: Implement typed API clients.**

  Query list filters through `URLSearchParams`; send `Idempotency-Key` on enable/pause/resume/publish; map `409` stale version to a reload notice, `401` to preserved-form login recovery, and `403` to preserved-form insufficient-permission feedback.

- [ ] **Step 4: Implement pilot list/detail UI.**

  Keep filter/page/size/sort in the URL. Detail tabs are Readiness, Graph, Sources & citations, Reward & outcomes, History. The graph editor exposes node code/type, choices and citations as structured controls; raw JSON is export-only. The global flag gate appears as a read-only deployment banner.

- [ ] **Step 5: Re-run focused API/E2E tests.**

  Run: `cd FE; npm run test:e2e:api -- tests/api/admin-console.spec.ts; npm run test:e2e:full -- tests/e2e/admin-narrative-pilot.spec.ts`

  Expected: both commands pass.

## Task 7: Complete trash, audit and content UI flows

**Files:**
- Create: `FE/src/features/admin/trashApi.ts`
- Create: `FE/src/features/admin/auditApi.ts`
- Create: `FE/src/pages/AdminTrashPage.tsx`
- Create: `FE/src/pages/AdminAuditPage.tsx`
- Modify: `FE/src/features/admin/api.ts`
- Modify: `FE/src/pages/AdminContentPage.tsx`
- Create: `FE/tests/e2e/admin-content-lifecycle.spec.ts`

- [ ] **Step 1: Write a failing lifecycle E2E test.**

  The test deletes an unreferenced content item with a preset reason, confirms it disappears from its normal list, appears in Trash with actor/reason/date, restores it, and verifies a deleted-parent response is rendered without losing the dialog input.

- [ ] **Step 2: Run the test and verify it fails.**

  Run: `cd FE; npm run test:e2e:full -- tests/e2e/admin-content-lifecycle.spec.ts`

  Expected: missing Delete/Trash controls.

- [ ] **Step 3: Implement content deletion, trash, and audit views.**

  Before confirmation, request/display impact summary. Use the shared reason dialog and restore conflict display. Render audit as a paginated, filterable event table. Do not display raw learner chat/memory, and render aggregate suppression instead of small counts.

- [ ] **Step 4: Re-run lifecycle E2E and frontend build.**

  Run: `cd FE; npm run test:e2e:full -- tests/e2e/admin-content-lifecycle.spec.ts; npm run build`

  Expected: lifecycle test and build pass.

## Task 8: Full verification and documentation handoff

**Files:**
- Modify: `BE/docs/superpowers/specs/2026-10-12-admin-console-design.md` only if implementation reveals a required, user-approved contract clarification.
- Create: `BE/docs/ADMIN_CONSOLE_OPERATIONS.md` — role matrix, rollout/pause/resume operational steps, audit/trash retention statement, and deployment-flag limitation.

- [ ] **Step 1: Run backend regression.**

  Run: `cd BE; .\mvnw.cmd test`

  Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run frontend quality gates.**

  Run: `cd FE; npm run ci; npm run test:e2e:api; npm run test:e2e:ui; npm run test:e2e:full`

  Expected: all configured suites pass. If environment-dependent suites cannot run, record the exact prerequisite and do not claim release readiness.

- [ ] **Step 3: Review migration safety and API privacy.**

  Verify Flyway migration is append-only and supports existing Củ Chi data; check that all admin mutations create exactly one `admin_audit_events` row; verify analytics hides groups where `n < 10`.

- [ ] **Step 4: Commit verified work in focused commits.**

  Use separate commits for: backend schema/audit, narrative lifecycle, frontend shell, frontend pilot/trash UI, and documentation. Do not include unrelated existing changes.

## Coverage review

- Admin routes, shell, grouped/sidebar/mobile accessibility: Tasks 5–7.
- Existing administration relocation without business redesign: Task 5.
- Soft deletion, impact, restore, reason validation, audit: Tasks 1–2 and 7.
- Per-version narrative review, active swap, one open draft, readiness, pause/resume, idempotency and optimistic locking: Tasks 3–4 and 6.
- Structured graph/citation UI and transparent gate explanations: Task 6.
- Generic audit, no duplicate review timeline, privacy threshold: Tasks 1, 4 and 7.
- Regression/build/E2E and documentation: Task 8.

## Plan self-review

- No requirement in the approved spec is unassigned.
- Public learner narrative reads remain isolated from candidate drafts.
- Every feature task starts with a failing test and names an exact verification command.
- Existing dirty/unrelated work is excluded from the focused commits.
