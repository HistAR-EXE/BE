# Admin Dashboard and Bootstrap Gaps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Correct the Admin Dashboard metric contract and empty-revenue behavior, then regenerate and verify the full bootstrap through V76.

**Architecture:** Keep counting semantics in the BE Dashboard service and expose the existing additive response fields to FE. Treat a revenue trend containing only zero buckets as absent at the financial-service boundary, and make the FE null/empty states explicit. Generate bootstrap SQL exclusively from the existing PowerShell generator.

**Tech Stack:** Java 17, Spring Boot, JUnit 5/AssertJ/Mockito, React 19, TypeScript 6, Node test runner, PowerShell.

---

### Task 1: Dashboard metric contract

**Files:**
- Modify: `src/test/java/com/histar/be/admin/dashboard/AdminDashboardServiceTest.java`
- Modify: `src/main/java/com/histar/be/admin/dashboard/AdminDashboardService.java`
- Modify: `../FE/scripts/admin-pages.test.mjs`
- Modify: `../FE/src/features/admin/api.ts`
- Modify: `../FE/src/pages/AdminDashboardPage.tsx`

- [ ] Add BE assertions and Mockito expectations proving all-open drafts differ from pending-review drafts.
- [ ] Run the focused BE test and confirm it fails because `countByOpenDraftTrue()` is not used.
- [ ] Query all open drafts for `draftCount` while retaining the `DRAFT` query for review-pending fields.
- [ ] Run the focused BE test and confirm it passes.
- [ ] Add FE source-contract assertions for `openContentReports`, independent labels/routes, and no misuse of `contentReportsLast30Days`.
- [ ] Run the FE Admin page test and confirm it fails against the current contract.
- [ ] Extend `AdminDashboardSummary` and bind Dashboard cards to the truthful fields and labels.
- [ ] Run the FE Admin page test and confirm it passes.

### Task 2: Empty revenue series and unavailable MoM

**Files:**
- Modify: `src/test/java/com/histar/be/billing/service/AdminFinancialServiceTest.java`
- Modify: `src/main/java/com/histar/be/billing/service/AdminFinancialService.java`
- Modify: `../FE/scripts/admin-pages.test.mjs`
- Modify: `../FE/src/pages/AdminDashboardPage.tsx`

- [ ] Add a BE test proving an all-zero requested period returns an empty revenue trend and null MoM.
- [ ] Run the focused BE test and confirm the trend-size assertion fails.
- [ ] Return an empty immutable trend when every generated bucket is zero.
- [ ] Run focused financial and UI DTO tests and confirm they pass.
- [ ] Add FE assertions requiring explicit unavailable MoM copy and forbidding `mom ?? 0` synthetic width.
- [ ] Run the FE Admin page test and confirm it fails.
- [ ] Render unavailable MoM without a progress bar while preserving real positive, zero, and negative MoM values.
- [ ] Run the FE Admin test suite and confirm it passes.

### Task 3: Generated bootstrap through V76

**Files:**
- Generate: `docs/sql/bootstrap/TimeLens_full_bootstrap.sql`

- [ ] Run `docs/scripts/New-HistarFullBootstrapSql.ps1`.
- [ ] Confirm generated output reports Flyway V76.
- [ ] Run `docs/scripts/Test-HistarBootstrapSql.ps1`.
- [ ] Run `docs/scripts/Test-HistarBootstrapRunner.ps1`.

### Task 4: Final verification

**Files:** No additional files.

- [ ] Run focused BE Dashboard/billing tests.
- [ ] Run the complete FE Admin test suite.
- [ ] Run the FE production build.
- [ ] Re-run both bootstrap tests.
- [ ] Inspect scoped diffs and confirm no alert-threshold or scheduled-export implementation was added.
