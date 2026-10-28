# Retired account features — 2026-10-27

Removed the independent Journey Wrapped, Study Groups and My Squad features at the user's request.

Frontend: removed the four pages (including group progress), API clients/types, routes and account-menu entries. Removed links in profile/settings/teacher/onsite/leaderboard/feedback and unsupported marketing/upsell text. Removed the unused multiplayer paywall component. Legacy URLs use the existing not-found screen; no compatibility route keeps the retired features alive.

Backend: removed journey-summary controller operation/service/DTO, independent group controller/operations/DTOs, the squad HTTP/WebSocket module and the dedicated multiplayer entitlement service. Removed the websocket starter and squad public-security exception. Removed standalone group capability from the organization API and obsolete feature tests.

The shared StudyGroup entities/repositories and GroupSummaryResponse remain because teacher field trips, reporting, cooperative ranking and admin trash still use them. Only teacher-managed provisioning/joining logic survives, extracted into fieldtrip/service/TripGroupService. Existing Flyway files and historical database tables/data are preserved; retirement requires no destructive migration.

Validation: targeted backend tests protect teacher trips, leaderboards, organization rights and profile editing; frontend logic tests, TypeScript/lint, production compilation and scripts/retired-features-smoke.mjs check menu omissions, retired-page 404s, removed API 404s and supported account/teacher flows. Local evidence is in the workspace docs/retire-* logs and retired-features-results.json.

Verification completed: 61 backend tests and 13 frontend logic tests passed; modified-source ESLint/TypeScript passed; production frontend build and backend clean package succeeded. Real Edge smoke passed all five checks in retired-features-results.json. Local frontend/backend restarted. Repository publication is recorded separately in Git history.
