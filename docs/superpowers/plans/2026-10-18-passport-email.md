# Passport Email Implementation Plan

> **For agentic workers:** Use subagent-driven-development to implement the independent mail transport and passport endpoint units; integrate and verify frontend in this session.

**Goal:** Send the complete existing 1200×1500 souvenir PNG to the authenticated verified owner, retaining two final actions: email and download.

**Architecture:** Multipart image is validated/reencoded server-side; a DB-backed reservation prevents concurrent repeats and enforces 60-second/three-per-rolling-24h limits. Mail transport accepts a per-message From and PNG attachment without changing existing OTP senders. No reward writes. No auto retries of ambiguous deliveries.

**Tech Stack:** Spring/JPA/JDBC/PostgreSQL, Java ImageIO/Jakarta Mail/WebClient, React/TypeScript/canvas, JUnit/Playwright/local SMTP capture.

## Tasks
- [x] Add failing mail attachment tests; implement SMTP/Brevo/Resend attachment support and truthful disabled behavior. Verify provider contracts using official docs; run existing mail tests.
- [x] Add failing passport email tests; implement owner-only status/send endpoints, strict PNG sanitizer, persistent quota/idempotency, no-store and explicit error codes. Add additive migration after current maximum; verify with rollback/test database.
- [x] Add failing UI cases; preserve current passport artwork and controls. Replace share with email, rename download, remove invented fallback email. Use one prepared blob for both actions; expose verified recipient and sending/cooldown/error states; guard unmounted/account-switched responses.
- [x] Run all leaderboard UI tests and focused BE/mail regression suites; verify attachment through local SMTP capture without sending real external mail, verify data unchanged. Build/lint and inspect exported PNG plus desktop/mobile dialogs.
- [x] Document production sender configuration, limits, operational unknown-delivery behavior and exact verification scope. Do not push/deploy or send external test messages automatically.
