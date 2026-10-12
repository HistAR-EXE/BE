# Củ Chi Vietnamese-Only Pilot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the Củ Chi narrative pilot be reviewed in Vietnamese only while retaining source, version, citation, graph-structure, and reward-safety gates.

**Architecture:** The content-review service will treat `vi` as the sole required pilot locale. The seeded graph will retain only verifiable Vietnamese government-source metadata and wording constrained to observation and source comparison; no operational trap instructions are introduced. The review packet will declare the Vietnamese-only scope and preserve explicit human sign-off for historical wording and reward policy.

**Tech Stack:** Spring Boot, Java, JUnit 5, Flyway SQL, Jackson JSON graph metadata, Markdown documentation.

---

### Task 1: Prove the Vietnamese-only review rule before changing production code

**Files:**
- Create: `BE/src/test/java/com/histar/be/quest/narrative/NarrativeContentReviewServiceTest.java`
- Modify: `BE/src/main/java/com/histar/be/quest/narrative/NarrativeContentReviewService.java`

- [x] **Step 1: Write the failing test**

```java
@Test
void approvesAVietnameseOnlyGraphWhenEverySourceHasVietnameseCitation() {
    var response = service.review(QUEST_ID, "reviewer@histar.vn",
            new NarrativeReviewRequest(NarrativeReviewDecision.APPROVED, Set.of("vi"), "Reviewed"));

    assertThat(response.approvedLocales()).containsExactly("vi");
}
```

- [x] **Step 2: Run the focused test and confirm RED**

Run: `./mvnw test -Dtest=NarrativeContentReviewServiceTest`

Expected: the request fails because the current required locale set also demands `en`, `ko`, and `zh-CN`.

- [x] **Step 3: Write the minimal implementation**

```java
private static final Set<String> REQUIRED_LOCALES = Set.of("vi");
```

Keep `validateApprovedGraph` unchanged so each approved locale must still have nonblank `sourceId`, `version`, `locale`, and `citation`.

- [x] **Step 4: Run the focused test and confirm GREEN**

Run: `./mvnw test -Dtest=NarrativeContentReviewServiceTest`

Expected: `BUILD SUCCESS`.

### Task 2: Replace the Củ Chi seed metadata with public, Vietnamese, reviewable citations

**Files:**
- Create: `BE/src/main/resources/db/migration/V60__cu_chi_vietnamese_only_pilot_citations.sql`
- Modify: `BE/docs/content-review/CU_CHI_NAP_HAM_PILOT_REVIEW_PACKET.md`

- [x] **Step 1: Write migration assertions before data changes**

Add a test that parses the intended graph JSON and asserts every `sources` item includes `sourceId`, `version`, `locale = "vi"`, a nonblank citation, and an HTTPS official-government URL.

- [x] **Step 2: Run the assertion and confirm RED**

Run: `./mvnw test -Dtest=NarrativeContentReviewServiceTest`

Expected: the current seed contains an unverifiable internal-only source description.

- [x] **Step 3: Create the forward-only migration**

Update only the Củ Chi graph row. Keep `enabled = FALSE`, `review_status = 'DRAFT'`, and `approved_locales_json = '[]'`. Replace source metadata with the Vietnamese Cổng Di sản văn hóa / Sở Văn hóa và Thể thao TP.HCM URLs and access-date versions. Keep prompts bounded to observation, historical context, and source comparison.

- [x] **Step 4: Update the review packet**

Replace four-locale obligations with `vi` as the pilot locale. State that future translations require a new review, rather than being silently enabled.

- [x] **Step 5: Run migration/content assertions and confirm GREEN**

Run: `./mvnw test -Dtest=NarrativeContentReviewServiceTest`

Expected: `BUILD SUCCESS`.

### Task 3: Verify review behavior and preserve rollout separation

**Files:**
- Modify: `BE/src/test/java/com/histar/be/quest/narrative/NarrativeContentReviewServiceTest.java`

- [x] **Step 1: Add a failing rejection test**

```java
@Test
void rejectsApprovalWhenVietnameseCitationMetadataIsMissing() {
    assertThatThrownBy(() -> service.review(QUEST_ID, "reviewer@histar.vn",
            new NarrativeReviewRequest(NarrativeReviewDecision.APPROVED, Set.of("vi"), "Reviewed")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("source");
}
```

- [x] **Step 2: Run the focused tests and confirm RED/GREEN as appropriate**

Run: `./mvnw test -Dtest=NarrativeContentReviewServiceTest`

Expected: valid Vietnamese graph approves; malformed graph is rejected; approval does not change `enabled` to true.

- [x] **Step 3: Run verification**

Run: `./mvnw test -Dtest=NarrativeContentReviewServiceTest,MinigameViewResponseTest`

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Do not commit unrelated dirty changes**

Only stage the files introduced or changed by this plan after verification. The existing V14/V46-V59 and minigame work are unrelated and remain untouched.
