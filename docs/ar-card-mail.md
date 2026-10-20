# AR selected-image email

See [the FE AR home/email guide](https://github.com/HistAR-EXE/FE/blob/master/docs/ar-home-ux-email.md) for the complete UX and test guide.

API: authenticated GET /api/ar/cards/email/status and POST /api/ar/cards/email, JSON body contains exactly requestId (UUID) and artifactId (server manifest ID). Recipients come exclusively from the verified account. HTML contains exactly one color recognition photo, the selected artifact name, and its photo/print/camera links. Accepted responses echo artifactId. GET status includes unavailableReason (MAIL_DISABLED, FEATURE_DISABLED, TEST_MODE, PUBLIC_URL_REQUIRED, or null).

Deploy V89__ar_card_mail_attempts.sql and V90__ar_card_mail_artifact.sql via Flyway. V90 adds nullable artifact_id; historical bulk-email rows retain NULL. Reusing an old UUID for another artifact or a historical NULL row returns 409 REQUEST_ID_REUSED. Set FRONTEND_URL to the public HTTPS FE origin, MAIL_ENABLED=true and the existing MAIL_PROVIDER/MAIL_FROM with verified provider credentials. HISTAR_AR_CARD_MAIL_ENABLED=false disables only this feature. Enabled test hooks always disable it. Local AR launcher intentionally keeps MAIL_ENABLED=false.

Durable UUID idempotency, 60-second cooldown, three attempts per rolling 24 hours shared across all artifacts. UNKNOWN never auto-retries; a user can explicitly start a new request after checking their mailbox and waiting for quota. Provider acceptance does not guarantee Inbox delivery. Bounded HTML methods leave existing OTP HTML methods unchanged. @Qualifier("emailSender") selects the configured provider among concrete sender beans.

Keep src/main/resources/ar/photo-cards.tsv synchronized using FE/scripts/sync-ar-mail-cards.mjs --check (omit --check to update). Deploy corresponding FE photos/catalog/bundle and BE manifest together.

Tests: ArCardMailServiceTest, ArCardMailControllerTest, ArCardMailWiringTest, ArCardHtmlProviderTest. Opt-in ArCardMailPostgresTest uses RUN_AR_CARD_POSTGRES_IT=true and AR_TEST_DB_URL/AR_TEST_DB_USER/AR_TEST_DB_PASSWORD; it creates/drops only a random ar_card_test_* schema and mocks the sender.

Localhost itself does not prevent provider calls. The local launcher intentionally disables them. Production requires a working configured provider, verified sender and public HTTPS frontend assets; deployment alone does not guarantee delivery. Email uses one remote inline photo plus direct links, not an attachment. No external email is sent by automated tests.

Production now defaults FLYWAY_ENABLED=true so migrations execute before Hibernate schema validation. If a host explicitly sets FLYWAY_ENABLED=false, apply V88 (team Vietnamese repair), V89 (AR attempts) and V90 (selected artifact) before starting this build, or remove that override. Do not baseline an existing shared database to skip these migrations.

The old local timelens_ar_local database retains the unpublished V88/V89 prototype history. The release launcher defaults to separate timelens_ar_release bootstrapped with the canonical V88/V89/V90 chain. Keep the old database as a backup; never repair shared Flyway history to match prototype version numbers.

Release verification 2026-10-21: full BE suite 588 tests, zero failures/errors (three optional PostgreSQL tests skipped); AR mail and team V88 repair PostgreSQL tests subsequently passed. Clean packaged JAR includes the ten-card manifest and canonical V88/V89/V90 chain. Full FE verification is recorded in [the release report](https://github.com/HistAR-EXE/FE/blob/master/docs/ar-release-verification.md). No external mail sent.
