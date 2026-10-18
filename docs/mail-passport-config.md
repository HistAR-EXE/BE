# Passport email configuration and verification

The passport feature uses the existing mail provider selected by `MAIL_PROVIDER` (`smtp`, `brevo`, or `resend`). Production configuration defaults to Brevo. Keep the provider configured for the deployment; Render deployments should not be switched to Gmail SMTP merely to use a Gmail From address.

The passport sender is configured separately with `HISTAR_PASSPORT_MAIL_FROM`, defaulting to `TimeLens <histar.timelens@gmail.com>`. This does not change `MAIL_FROM` for OTP or account recovery. Set `HISTAR_PASSPORT_MAIL_ENABLED=false` to disable the feature. Global mail disablement and enabled test hooks also make passport delivery unavailable, and must never report a successful send.

Before enabling production delivery, the deployment operator must verify that the selected provider authorizes this exact sender and accepts the deployment's configuration. A configured From address alone does not prove provider verification or inbox delivery. Store provider credentials in deployment secrets, never in source control or chat. Brevo uses `BREVO_API_KEY`; Resend uses `RESEND_API_KEY`; SMTP uses the existing Spring mail configuration.

The server selects the authenticated account's verified email. Clients cannot choose another recipient or arbitrary mail headers. The attached souvenir is the validated and reencoded 1200 x 1500 PNG; delivery does not change achievements. Limits are one request per 60 seconds and three attempts per rolling 24 hours. A persisted request ID prevents duplicate delivery. An ambiguous transport result is retained and must not be automatically retried.

`ACCEPTED` means the configured provider accepted the request. It does not promise Inbox placement; recipients should also check Spam. Disabled configuration, rejection, and timeout must not be presented as successful delivery. HTTP attachment requests and SMTP connection/read/write operations have bounded timeouts.

Automated verification includes a real SMTP conversation captured exclusively on an ephemeral `127.0.0.1` listener, addressed to `QA@example.invalid`. The test checks envelope/MIME sender and recipient, PNG bytes, content type, and the complete image dimensions. It sends no external email. Provider payload tests use mocked API boundaries. These checks do not establish production sender authorization, external delivery, or Inbox placement. Record a successful automated test run separately; a production mailbox check still requires valid provider configuration and an authorized user send.

Provider attachment formats: [Brevo transactional email reference](https://developers.brevo.com/reference/send-transac-email) and [Resend send email reference](https://resend.com/docs/api-reference/emails/send-email).
