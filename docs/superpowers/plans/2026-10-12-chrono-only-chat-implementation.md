# Chrono-only Chat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Chrono the sole chat agent returned for each location without deleting legacy characters or chat history.

**Architecture:** Add an explicit database availability flag to characters, expose a chat-specific service query, seed Chrono per location in V62, then normalize FE persona routing to Chrono. The full bootstrap generator consumes migrations automatically, so regenerating the SQL makes V62 part of a fresh local database.

**Tech Stack:** Spring Boot 4, JPA, Flyway/PostgreSQL, React, TypeScript, Vitest/Vite.

---

### Task 1: Chat-agent persistence and API

**Files:**
- Modify: `src/main/java/com/histar/be/character/entity/CharacterEntity.java`
- Modify: `src/main/java/com/histar/be/character/repository/CharacterRepository.java`
- Modify: `src/main/java/com/histar/be/character/service/CharacterService.java`
- Modify: `src/main/java/com/histar/be/character/service/impl/CharacterServiceImpl.java`
- Modify: `src/main/java/com/histar/be/character/controller/CharacterController.java`
- Create: `src/test/java/com/histar/be/character/service/impl/CharacterServiceImplTest.java`

- [ ] Write a failing service test that expects `findChatAgentsByLocationId` to call the enabled-only repository query, while `findById` remains available.
- [ ] Run the test and confirm it fails because the API does not exist.
- [ ] Add `chatEnabled`, repository query, service method, and route delegation with no change to direct lookup/deletion methods.
- [ ] Run the focused test and then the character package tests.

### Task 2: Versioned database and bootstrap

**Files:**
- Create: `src/main/resources/db/migration/V62__chrono_only_chat_agents.sql`
- Modify: `docs/sql/bootstrap/TimeLens_full_bootstrap.sql`
- Test: `docs/scripts/Test-HistarBootstrapSql.ps1`

- [ ] Write a failing structural assertion that generated bootstrap must contain V62.
- [ ] Run the structural test and confirm it fails before V62 exists.
- [ ] Add V62: add `chat_enabled` default false, disable existing records, idempotently insert enabled Chrono for every location.
- [ ] Regenerate bootstrap using `New-HistarFullBootstrapSql.ps1` and rerun structural test.

### Task 3: Chrono-only frontend route and labels

**Files:**
- Modify: `FE/src/features/chat/chatRoute.ts`
- Modify: `FE/src/pages/ChatPage.tsx`
- Create: `FE/src/features/chat/chatRoute.test.ts`

- [ ] Write a failing test for default and legacy persona values resolving to `chrono`.
- [ ] Run it and confirm failure.
- [ ] Replace legacy persona union/matching with a single Chrono route helper and make `ChatPage` select the enabled Chrono API result.
- [ ] Update only user-visible voice/chat wording that still calls Chrono a historical character.
- [ ] Run the focused frontend test and production build.

### Task 4: End-to-end verification

**Files:**
- Modify: this plan to mark verification results.

- [ ] Run Maven focused tests and package/compile verification.
- [ ] Run Flyway/bootstrap structural tests and inspect generated V62 section.
- [ ] Run FE focused tests and `npm run build`.
- [ ] Do not reset `timelens_full`; document the separate, destructive repair command for the user after code verification.
