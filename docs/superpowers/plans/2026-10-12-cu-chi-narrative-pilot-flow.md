# Củ Chi Narrative Pilot Data Flow Implementation Plan

**Goal:** Biến Narrative Pilot Củ Chi từ graph tối giản thành một hành trình điều tra có dữ liệu, kiểm chứng, mini-game, ending và reward được backend kiểm soát.

**Architecture:** Giữ `narrative_investigations.graph_json` làm bản đồ flow versioned. Backend validate reachability, observation, chat handoff, evidence, outcome và reward; FE chỉ render graph và gửi action hợp lệ. Các citation/fact được lưu trong graph và hiển thị bằng source cards, còn progress vẫn dùng bảng `user_narrative_investigation_progress` với lock/idempotent ledger.

**Tech Stack:** Spring Boot/JPA/Flyway, PostgreSQL, React/TypeScript, existing quest minigame API, existing Chrono chat route.

---

### Task 1: Author a richer Vietnamese-only Củ Chi graph

**Files:**
- Create: `BE/src/main/resources/db/migration/V65__cu_chi_narrative_flow_v4.sql`
- Create: `BE/src/main/resources/db/migration/V66__narrative_observation_progress.sql`
- Create: `BE/src/main/resources/db/migration/V67__narrative_chat_progress.sql`
- Modify: `BE/src/main/java/com/histar/be/quest/narrative/model/NarrativeQuestGraph.java`
- Modify: `BE/src/main/java/com/histar/be/quest/narrative/model/NarrativeNode.java`

- [x] Add authored node metadata: observation checklist, source IDs, evidence label, reward amount, and optional handoff target while preserving old JSON fields.
- [x] Seed a controlled Vietnamese flow: Tour observation → Chrono interview → source comparison → two evidence cards → Soi nắp handoff → verified/partial outcome.
- [x] Include only facts/citations already approved in the existing Vietnamese pilot source set; no unsafe instructions for traps.
- [x] Keep migration disabled/DRAFT until Admin review approves it.
- [x] Persist Tour 360 checklist selections as controlled observation progress.

### Task 2: Enforce graph actions and reward semantics in backend

**Files:**
- Modify: `BE/src/main/java/com/histar/be/quest/narrative/NarrativeInvestigationService.java`
- Modify: `BE/src/main/java/com/histar/be/quest/narrative/NarrativeActionRequest.java`
- Modify: `BE/src/main/java/com/histar/be/quest/narrative/NarrativeProgressResponse.java`
- Test: `BE/src/test/java/com/histar/be/quest/narrative/NarrativeInvestigationServiceTest.java`

- [x] Return authored metadata and source cards to FE.
- [x] Require a node to be reachable before accepting `VISIT_NODE` and `SELECT_OUTCOME`.
- [x] Require a node to be visited and an evidence node to be collected before advancing; linked mini-games must be completed before collection.
- [x] Allow only graph-declared outcomes and enforce each outcome’s evidence threshold.
- [x] Preserve row locking and ledger idempotency; existing narrative tests remain green.
- [x] Reject undeclared observations and persist Chrono interview completion separately from visited nodes.

### Task 3: Connect Chrono and Soi nắp to the investigation

**Files:**
- Modify: `FE/src/features/gamification/NarrativeInvestigationBoard.tsx`
- Modify: `FE/src/features/gamification/api.ts`
- Modify: `FE/src/pages/MinigamePage.tsx`
- Modify: `FE/src/routes/AppRoutes.tsx`

- [x] Build a Chrono handoff URL carrying quest/location/investigation context.
- [x] Send quest/node/version context to backend and persist the completed Chrono interview in narrative progress.
- [x] Render the prompt as a guided question mission, not a generic next button.
- [x] Link the evidence node to the existing Soi nắp mini-game and return with a completion marker.
- [x] Refresh progress after return and show the evidence state/count.

### Task 4: Upgrade the evidence-board UI

**Files:**
- Modify: `FE/src/features/gamification/NarrativeInvestigationBoard.tsx`
- Modify: `FE/src/styles/globals.css`

- [x] Render observation checklist, fact/interpretation labels, source cards, evidence count, and outcome requirements.
- [x] Keep locked actions visibly locked with reason text and keyboard/focus support.
- [x] Show resume state and API errors without losing the current local view.

### Task 5: Make the flow authorable and observable in Admin Console

**Files:**
- Modify: `FE/src/pages/AdminNarrativePilotsPage.tsx`
- Modify: `FE/src/features/admin/api.ts`
- Modify: `BE/src/main/java/com/histar/be/quest/narrative/NarrativePilotAdminService.java`

- [x] Display graph node/source/evidence/outcome counts and readiness failures.
- [x] Show version and review history before enable.
- [x] Add an optimistic-locking JSON draft editor; save resets review and keeps pilot disabled.
- [x] Keep enable/disable reason and durable audit behavior.

### Task 6: Verify end to end

- [x] Run `BE/mvnw.cmd test` and confirm zero failures/errors (294 tests, 0 failures/errors/skips).
- [x] Run `FE/npm run build` and confirm TypeScript/Vite success.
- [x] Run `git diff --check` in BE and FE.
- [x] Verify migration JSON locally: 7 nodes, 2 evidence cards, 2 sources, 2 outcomes.
- [x] Verify observation/chat progress changes with focused service tests.

