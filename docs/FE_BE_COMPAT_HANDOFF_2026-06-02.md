# FE-BE Compatibility Handoff (2026-06-02)

Tai lieu nay tong hop **34 files** BE da cap nhat de phu hop voi `backend-work-items-for-fe-compat.md`, giup FE bo mock va chuyen sang data that.

## 1) Da hoan thanh gi theo contract FE

- Chuan hoa error envelope cho `401/403/404/422` (`code`, `message`, `fieldErrors`, `timestamp`).
- Chuan hoa list envelope cho cac list API:
  - `items`, `page`, `size`, `totalItems`, `totalPages`.
- Nang cap `GET /api/locations`:
  - ho tro `page/size/sort/city/search/nearLat/nearLng/maxDistanceKm/tags`.
  - bo sung field FE can: `rating`, `distanceKm`, `isArAvailable`.
- Chuan hoa quest/progress APIs:
  - `GET /api/quests`
  - `GET /api/me/quests`
  - `POST /api/quests/{id}/start`
  - `GET /api/quests/{id}/progress`
  - bo sung progress fields: `currentStep`, `stepsTotal`.
- Nang cap auth contract:
  - login/register tra them `accessToken/expiresIn/refreshToken/refreshExpiresIn`.
  - van giu `token` de backward-compatible FE cu.
  - them `POST /api/auth/refresh`, `POST /api/auth/logout`.
- Them `PATCH /api/profile/me` de update `displayName`, `avatarUrl`, `city`.
- Chat messages pagination:
  - `GET /api/chat/conversations/{id}/messages?page&size&sort`.
- Observability:
  - `X-Request-Id` middleware + structured request log.
  - expose actuator `health,metrics`.
- DB scripts:
  - migration fields moi cho locations/quests/user_quest_progress/refresh_tokens.
  - index pack.
  - topup demo data cho FE (>=10 record moi domain chinh).

## 2) FE can dieu chinh gi

- **List APIs**: doc du lieu tu `data.items` (khong doc truc tiep mang root).
- **Pagination state**: dung them `page/size/totalItems/totalPages` de render pager.
- **Auth response**:
  - uu tien luu `accessToken`.
  - dung `refreshToken` khi refresh session.
  - neu FE code cu dang dung `token` thi van chay duoc.
- **Error handling**:
  - map toast theo `code` (`UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `VALIDATION_ERROR`, `BUSINESS_RULE`).
  - voi `VALIDATION_ERROR`, hien field-level message tu `fieldErrors`.
- **Locations UI**:
  - co the render truc tiep `rating`, `distanceKm`, `isArAvailable`.
- **Quest UI**:
  - dung `currentStep/stepsTotal` de ve progress bar thay vi suy dien o FE.

## 3) Thu tu chay SQL de co du lieu that (khong mock)

Chay theo thu tu sau tren PostgreSQL:

1. `docs/database/TimeLens_DB_Schema.sql`
2. `docs/database/2026-06-02_fe_compat_migration.sql`
3. `docs/database/2026-06-02_fe_compat_indexes_seed.sql`
4. `docs/database/2026-06-02_fe_compat_data_topup.sql`

Sau khi chay xong, cac domain chinh cho FE da du day (muc tieu >=10 records): locations, quests, user_quest_progress, profiles, badges, user_badges, conversations/messages, checkins, user_creations, unlocks.

## 4) Danh sach 34 files da thay doi

1. `pom.xml`
2. `src/main/resources/application.yml`
3. `src/main/java/com/histar/be/auth/controller/AuthController.java`
4. `src/main/java/com/histar/be/auth/dto/AuthResponse.java`
5. `src/main/java/com/histar/be/auth/dto/LogoutRequest.java`
6. `src/main/java/com/histar/be/auth/dto/RefreshTokenRequest.java`
7. `src/main/java/com/histar/be/auth/service/AuthService.java`
8. `src/main/java/com/histar/be/auth/service/impl/AuthServiceImpl.java`
9. `src/main/java/com/histar/be/chat/controller/ChatController.java`
10. `src/main/java/com/histar/be/chat/service/ChatService.java`
11. `src/main/java/com/histar/be/chat/service/impl/ChatServiceImpl.java`
12. `src/main/java/com/histar/be/common/exception/ErrorCode.java`
13. `src/main/java/com/histar/be/common/exception/GlobalExceptionHandler.java`
14. `src/main/java/com/histar/be/common/response/PageResponse.java`
15. `src/main/java/com/histar/be/common/web/RequestTracingFilter.java`
16. `src/main/java/com/histar/be/location/controller/LocationController.java`
17. `src/main/java/com/histar/be/location/dto/LocationResponse.java`
18. `src/main/java/com/histar/be/location/repository/LocationRepository.java`
19. `src/main/java/com/histar/be/location/service/LocationService.java`
20. `src/main/java/com/histar/be/location/service/impl/LocationServiceImpl.java`
21. `src/main/java/com/histar/be/message/repository/MessageRepository.java`
22. `src/main/java/com/histar/be/profile/controller/ProfileController.java`
23. `src/main/java/com/histar/be/profile/dto/UpdateProfileRequest.java`
24. `src/main/java/com/histar/be/quest/controller/QuestController.java`
25. `src/main/java/com/histar/be/quest/dto/QuestProgressResponse.java`
26. `src/main/java/com/histar/be/quest/dto/QuestResponse.java`
27. `src/main/java/com/histar/be/quest/service/QuestProgressService.java`
28. `src/main/java/com/histar/be/quest/service/impl/QuestProgressServiceImpl.java`
29. `src/main/java/com/histar/be/security/JwtService.java`
30. `src/main/java/com/histar/be/security/SecurityConfig.java`
31. `src/main/java/com/histar/be/security/SecurityExceptionHandlers.java`
32. `docs/database/2026-06-02_fe_compat_migration.sql`
33. `docs/database/2026-06-02_fe_compat_indexes_seed.sql`
34. `docs/database/2026-06-02_fe_compat_data_topup.sql`

## 5) Quick FE smoke checklist

- Login/register thanh cong, co access+refresh token.
- `/api/locations` co du card (khong trang/trang xo).
- `/api/quests` va `/api/me/quests` co progress that.
- Profile me + patch me chay dung.
- Leaderboard/chat/list APIs doc dung `data.items`.
- Error toast map theo `code` thong nhat.
