# HistAR / TimeLens BE — Tổng hợp trạng thái & hướng dẫn FE

> **Mục đích file này:** Gộp `FE_API_HANDOFF.md` (Week 1–4) + `FE_BE_COMPAT_HANDOFF_2026-06-02.md` (sprint tương thích FE) thành **một nguồn sự thật duy nhất** cho team FE và AI (Claude) khi làm việc với BE.
>
> **CP3 3 tuần:** xem audit trạng thái tại [CP3_AUDIT_STATUS.md](./CP3_AUDIT_STATUS.md).
>
> **Base URL local:** `http://localhost:8080`  
> **CORS:** `CORS_ALLOWED_ORIGINS` (mặc định `http://localhost:5173`)  
> **Auth:** JWT stateless qua header `Authorization: Bearer <token>`

---

## 1. Tóm tắt nhanh (đọc trước)

| Hạng mục | Trạng thái |
|----------|------------|
| Week 1 — Content (locations, 360°, chat, auth cơ bản) | **Done** |
| Week 2 — Gamification (check-in, quest, badge, secret, level) | **Done** |
| Week 3 — Viral (photo frame, upload MinIO, share, leaderboard) | **Done** |
| Week 4 — Demo/Prod (health/ready, demo check-in, Dockerfile) | **Done** |
| Sprint FE compat (2026-06-02) — pagination, auth refresh, error 422, DB seed dày | **Done (một phần còn hạn chế, xem mục 6)** |
| FE bỏ mock hoàn toàn, data Stitch đầy đủ | **Cần chạy SQL seed + FE chỉnh contract mới** |

**Kết luận cho FE:** BE đã có đủ API cho golden path demo. Để UI Explore/Quest/Profile/Leaderboard chạy data thật ổn định, FE phải cập nhật theo **breaking changes** ở mục 3 và chạy **bộ SQL seed** ở mục 5.

---

## 2. BE đã làm được gì (theo giai đoạn)

### 2.1 Week 1 — Nội dung & trải nghiệm cốt lõi

- Auth: `POST /api/auth/register`, `POST /api/auth/login`
- Locations, characters, photo-pairs, panoramas, hotspots (đọc public)
- Chat Gemini: `POST /api/chat`, `GET /api/chat/conversations/{id}/messages` (JWT)
- Profile: `GET /api/profile/me` (JWT)
- Health: `GET /api/health`, `GET /api/health/ready`
- Seed pilot Củ Chi với UUID cố định (location, panorama, quest)

### 2.2 Week 2 — Gamification

- Check-in GPS + QR: `POST /api/checkins`
- Quest lifecycle: list / me / start / progress
- Badge catalog + user badges
- Secret story unlock: `GET /api/locations/{id}/secret-story`
- Profile mở rộng: `levelName`, `pointsToNextLevel`, `levelProgressPercent`
- Level thresholds: Explorer (0) → Time Traveler (100) → History Hunter (300) → Legend (700)

### 2.3 Week 3 — Viral loop

- Photo frames catalog: `GET /api/photo-frames`
- Upload creation (multipart → MinIO): `POST /api/user-creations`, `GET /api/me/user-creations`
- Share: `GET /api/share/prefill`, `POST /api/user-creations/{id}/record-share` (+15 điểm)
- Leaderboard: `GET /api/leaderboard?scope=all|city|week`

### 2.4 Week 4 — Ổn định demo / deploy

- `GET /api/health/ready` (200/503 theo DB)
- `POST /api/demo/checkin` (header `X-Demo-Secret`, chỉ khi `DEMO_ENABLED=true`)
- Dockerfile, `application-prod.yml`, script smoke `docs/scripts/smoke-golden-path.ps1`

### 2.5 Sprint FE compat (2026-06-02) — bổ sung cho Stitch UI

| Hạng mục | Chi tiết |
|----------|----------|
| Error contract | `401/403/404/422` thống nhất; `VALIDATION_ERROR` + `fieldErrors` |
| Pagination envelope | `items`, `page`, `size`, `totalItems`, `totalPages` cho **một số** list API |
| Locations | Filter/search/sort/nearby; field `rating`, `distanceKm`, `isArAvailable` |
| Quest progress | `currentStep`, `stepsTotal` trong progress DTO |
| Auth | `accessToken`, `refreshToken`, `expiresIn`; `POST /api/auth/refresh`, `POST /api/auth/logout` |
| Profile | `PATCH /api/profile/me` |
| Observability | `X-Request-Id`, actuator `health,metrics` |
| DB | Migration + index + topup data (>=10 record/domain chính) |
| Bugfix startup | `SecurityExceptionHandlers` không còn phụ thuộc bean `ObjectMapper` |

---

## 3. Breaking changes — FE **bắt buộc** cập nhật

So với `FE_API_HANDOFF.md` gốc, các điểm sau **đã thay đổi trên BE hiện tại**:

### 3.1 List API: không còn trả mảng trực tiếp (một số endpoint)

| Endpoint | Trước (doc cũ) | Hiện tại (BE) |
|----------|----------------|---------------|
| `GET /api/locations` | `data: LocationResponse[]` | `data: { items, page, size, totalItems, totalPages }` |
| `GET /api/quests` | `data: QuestResponse[]` | `data: PageResponse<QuestResponse>` |
| `GET /api/me/quests` | `data: QuestProgressResponse[]` | `data: PageResponse<QuestProgressResponse>` |
| `GET /api/chat/conversations/{id}/messages` | `data: MessageResponse[]` | `data: PageResponse<MessageResponse>` |

**FE action:** Đọc `response.data.data.items`, không dùng `response.data.data` như mảng.

**Query chung:** `page` (default 0), `size` (default 20, max 100), `sort` (vd. `createdAt,desc`).

**Endpoints list vẫn trả mảng trực tiếp** (chưa paginate): `GET /api/characters/...`, `GET /api/photo-pairs/...`, `GET /api/badges`, `GET /api/photo-frames`, `GET /api/leaderboard`, v.v.

### 3.2 Validation error: HTTP 422 (không còn 400)

| HTTP | `code` | Ghi chú |
|------|--------|---------|
| 422 | `VALIDATION_ERROR` | Có `fieldErrors` |
| 422 | `BUSINESS_RULE` | Nghiệp vụ (rate limit, GPS xa, v.v.) |
| 401 | `UNAUTHORIZED` | Thiếu/sai JWT |
| 403 | `FORBIDDEN` | Không đủ quyền |
| 404 | `NOT_FOUND` | Không tìm thấy |
| 409 | `CONFLICT` | Email đã tồn tại |
| 500 | `INTERNAL_ERROR` | Lỗi không mong đợi |

### 3.3 Auth response mở rộng

Login/register `data` hiện có:

```json
{
  "token": "<jwt>",
  "accessToken": "<jwt>",
  "expiresIn": 86400,
  "refreshToken": "<refresh>",
  "refreshExpiresIn": 604800,
  "userId": "<uuid>",
  "displayName": "..."
}
```

- `token` và `accessToken` **cùng giá trị** (tương thích FE cũ).
- FE nên lưu `accessToken` + `refreshToken`, gọi `POST /api/auth/refresh` khi hết hạn.
- `POST /api/auth/logout` body: `{ "refreshToken": "..." }`.

> **Lưu ý BE:** Refresh token hiện lưu **in-memory** (mất khi restart server). Production nên chuyển sang bảng `refresh_tokens` (đã có script migration).

### 3.4 Location item thêm field

```json
{
  "id": "...",
  "name": "...",
  "description": "...",
  "latitude": 0,
  "longitude": 0,
  "city": "...",
  "coverImage": "https://...",
  "rating": 4.8,
  "distanceKm": 1.2,
  "isArAvailable": true,
  "createdAt": "..."
}
```

- `distanceKm` chỉ có ý nghĩa khi gọi kèm `nearLat` + `nearLng`.
- Nếu DB chưa migrate cột mới, BE vẫn trả default (`rating: 0`, `isArAvailable: false`) từ mapper.

### 3.5 Quest progress thêm bước

`QuestProgressResponse` thêm: `currentStep`, `stepsTotal` (dùng cho progress bar UI).

`QuestResponse` thêm: `stepsTotal`, `unlockLevel`, `coverImage`.

### 3.6 Profile cập nhật

- `PATCH /api/profile/me` — body optional: `displayName`, `avatarUrl`, `city`.

---

## 4. Contract API hiện tại (authoritative)

### 4.1 Envelope chung

**Success:**

```json
{ "success": true, "message": null, "data": { } }
```

**Error:**

```json
{
  "success": false,
  "code": "NOT_FOUND",
  "message": "...",
  "timestamp": "2026-06-02T10:00:00Z",
  "fieldErrors": { }
}
```

`fieldErrors` chỉ có khi `code = VALIDATION_ERROR`.

### 4.2 Auth & phân quyền

| Nhóm | Endpoint | Auth |
|------|----------|------|
| Public | `/api/health/**`, `/api/auth/**` | Không |
| Public | `/api/locations/**` (trừ secret-story) | Không |
| Public | characters, photo-pairs, panoramas, hotspots, quests list, badges, photo-frames, leaderboard, share/prefill | Không |
| JWT | profile, chat, checkins, me/*, quest start/progress, secret-story, user-creations, demo/checkin | **Bearer JWT** |

JWT subject = **email**. Dùng `userId` từ response login/register, không parse từ token.

### 4.3 Locations (paginated)

`GET /api/locations?page=0&size=20&sort=createdAt,desc&city=Hue&search=noi&nearLat=16.46&nearLng=107.59&maxDistanceKm=50`

`GET /api/locations/{id}` — single item, không paginate.

### 4.4 Quest

| Method | Path | Auth | Paginated |
|--------|------|------|-----------|
| GET | `/api/quests?locationId=&page=&size=&sort=` | Public | Có |
| GET | `/api/me/quests?locationId=&status=&page=&size=` | JWT | Có |
| POST | `/api/quests/{id}/start` | JWT | — |
| GET | `/api/quests/{id}/progress` | JWT | — |

Status: `not_started` → `in_progress` → `completed`.

### 4.5 Các API Week 1–4 khác

Giữ nguyên như `FE_API_HANDOFF.md` cho: characters, photo-pairs, panoramas, hotspots, checkins, badges, secret-story, photo-frames, user-creations, share, leaderboard, demo check-in.

**Leaderboard** vẫn trả:

```json
{
  "scope": "all",
  "city": null,
  "entries": [ { "rank": 1, "userId": "...", "displayName": "...", "totalPoints": 500, "currentUser": true } ]
}
```

Không dùng `PageResponse` (chưa implement pagination cho leaderboard).

### 4.6 Axios mẫu (cập nhật)

```typescript
const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken') ?? localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export function unwrap<T>(res: { data: { success: boolean; data: T } }): T {
  return res.data.data;
}

export function unwrapPage<T>(res: {
  data: { success: boolean; data: { items: T[]; page: number; size: number; totalItems: number; totalPages: number } };
}) {
  return res.data.data;
}

// Locations
const page = unwrapPage<Location>(await api.get('/api/locations', { params: { page: 0, size: 20 } }));
const locations = page.items;
```

---

## 5. Database & seed — chạy để FE không mock

### 5.1 Thứ tự SQL (PostgreSQL)

1. `docs/database/TimeLens_DB_Schema.sql` — schema + seed cơ bản (Củ Chi + extended)
2. `docs/database/2026-06-02_fe_compat_migration.sql` — cột mới + `refresh_tokens`
3. `docs/database/2026-06-02_fe_compat_indexes_seed.sql` — index + seed quest FE (Hue/Hà Nội)
4. `docs/database/2026-06-02_fe_compat_data_topup.sql` — topup >=10 records/domain

Nếu DB volume cũ thiếu seed/migration: `docker compose down -v && docker compose up -d` rồi chạy lại từ bước 1.

### 5.2 Dữ liệu demo quan trọng cho FE

| Resource | UUID / ghi chú |
|----------|----------------|
| Location Củ Chi | `11111111-1111-1111-1111-111111111111` |
| Panorama 360 | `22222222-2222-2222-2222-222222222222` |
| Quest Hành trình dưới lòng đất | `33333333-3333-3333-3333-333333333333` |
| Locations Stitch | Đại Nội Huế, Chùa Thiên Mụ, Hoàng Thành Thăng Long, … (trong topup) |
| Profiles demo | `user1@histar.vn` … `user12@histar.vn` (password hash trong seed) |
| Leaderboard | Nhiều profile có `totalPoints` để UI không trống |

### 5.3 Local services

| Service | URL |
|---------|-----|
| API | http://localhost:8080 |
| Postgres | localhost:5432 |
| MinIO | http://localhost:9000 |
| Actuator | http://localhost:8080/actuator/health |

Chat cần `GEMINI_API_KEY` trong `.env`.

---

## 6. Hạn chế / chưa hoàn thiện (định hướng BE tiếp)

| Mục | Trạng thái | Gợi ý bước tiếp |
|-----|------------|----------------|
| Pagination toàn bộ list API | Một phần | Thêm `PageResponse` cho leaderboard, badges list, characters, … |
| `tags` filter locations | Param có, logic chưa map domain | Thêm cột/tag table hoặc JSONB tags |
| Media placeholder/fallback tập trung | Chưa có service chung | `MediaUrlResolver` + URL mặc định khi null |
| Refresh token persistent | In-memory | Dùng bảng `refresh_tokens` + revoke thật (post-CP3) |
| `rating` / `is_ar_available` | **Đã map** từ `Location` entity | Cần chạy `fe_compat_migration.sql` |
| Upload panorama 360 | **`POST /api/panoramas`** (JWT) | Xem [week-3/04-HUONG-DAN-360-UPLOAD.md](./week-3/04-HUONG-DAN-360-UPLOAD.md) |
| `FORBIDDEN` code | Đã có trong handler | FE map toast |
| Sentry / full APM | Out of scope Week 4 | Tùy production |
| Leaderboard bug PostgreSQL null weekStart | **Đã fix** (tách `findLeaderboard` / `findLeaderboardSince`) | — |

---

## 7. Định hướng tiếp theo (ưu tiên)

### 7.1 Cho FE (ngay)

1. **Migrate API client** theo mục 3 (pagination + 422 + auth refresh).
2. **Chạy bộ SQL** mục 5 trước khi test UI.
3. **Explore page:** `GET /api/locations` → render `items[]` với `coverImage`, `rating`, `distanceKm`.
4. **Quest page:** dùng `currentStep`/`stepsTotal` từ BE, bỏ logic suy diễn progress ở FE.
5. **Error toast:** map theo `code`, không parse message tự do từng API.
6. **Auth:** implement refresh flow; fallback `token` vẫn OK cho dev nhanh.

### 7.2 Cho BE (sprint kế)

| Ưu tiên | Task |
|---------|------|
| P0 | Persist refresh token (`refresh_tokens` table) |
| P0 | Entity `Location` map `rating`, `isArAvailable` từ DB |
| P1 | Pagination cho `GET /api/leaderboard` |
| P1 | Media URL fallback + health check ảnh |
| P2 | Implement `tags` filter locations |
| P2 | Chuẩn hóa pagination cho toàn bộ list endpoints còn lại |
| P3 | Integration test FE contract (golden path JSON snapshot) |

### 7.3 Checklist smoke end-to-end

```bash
# Health
curl -s http://localhost:8080/api/health
curl -s http://localhost:8080/api/health/ready

# Auth
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"fe.test@histar.vn","password":"Test1234!","displayName":"FE Test"}'

# Locations (paginated)
curl -s "http://localhost:8080/api/locations?page=0&size=10"

# Leaderboard
curl -s "http://localhost:8080/api/leaderboard?scope=all"
```

PowerShell full path: `docs/scripts/smoke-golden-path.ps1`

---

## 8. Map trang FE → API (cập nhật)

| Trang / tính năng | API chính | Lưu ý contract mới |
|-------------------|-----------|-------------------|
| Login / Register | `POST /api/auth/login`, `register`, `refresh`, `logout` | Dùng `accessToken` + `refreshToken` |
| Explore / Home | `GET /api/locations?page&size&...` | Đọc `data.items` |
| Location detail | `GET /api/locations/{id}`, photo-pairs, characters | Array (chưa paginate) |
| 360 tour | panoramas, hotspots | Giữ nguyên |
| Chat | `POST /api/chat`, messages paginated | `data.items` |
| Profile | `GET/PATCH /api/profile/me`, `GET /api/me/badges` | PATCH mới |
| Quest | quests + me/quests paginated, start, progress | `currentStep`, `stepsTotal` |
| Check-in / QR | `POST /api/checkins` | 422 BUSINESS_RULE |
| Leaderboard | `GET /api/leaderboard` | Vẫn `entries[]` (chưa paginate) |
| Photo frame / Share | photo-frames, user-creations, share/prefill | Giữ nguyên Week 3 |

---

## 9. Cấu trúc package BE (tham khảo)

| Package | Vai trò |
|---------|---------|
| `common/` | exception, response (`ApiResponse`, `PageResponse`), `web` (request tracing) |
| `config/` | app config, `DataInitialize` |
| `security/` | JWT, `SecurityConfig`, `SecurityExceptionHandlers` |
| `{module}/` | controller, dto, service, entity, repository |
| `health/` | health endpoints |

---

## 10. Tài liệu gốc & file thay đổi sprint compat

| File | Vai trò |
|------|---------|
| `docs/FE_API_HANDOFF.md` | API reference Week 1–4 (một số phần đã lỗi thời — ưu tiên file này) |
| `docs/FE_BE_COMPAT_HANDOFF_2026-06-02.md` | Tóm tắt sprint compat + 34 files |
| **`docs/BE_PROJECT_STATUS_AND_FE_GUIDE.md`** | **File tổng hợp chính thức (file này)** |
| `docs/DEPLOY.md` | Deploy production |
| `docs/database/*.sql` | Schema + migration + seed |

### 34 files đã sửa trong sprint compat (2026-06-02)

`pom.xml`, `application.yml`, auth (controller/dto/service), chat, common (exception/PageResponse/RequestTracingFilter), location, message repo, profile, quest, security, và 3 file SQL trong `docs/database/`.

Chi tiết từng file: xem `FE_BE_COMPAT_HANDOFF_2026-06-02.md` mục 4.

---

## 11. Ghi chú cho Claude / AI khi đọc codebase

Khi hỗ trợ FE hoặc review BE:

1. **Luôn kiểm tra** endpoint có trả `PageResponse` hay mảng trực tiếp trước khi sửa FE types.
2. **Giả định validation = 422**, không 400.
3. **Leaderboard** đã fix lỗi PostgreSQL `parameter $3` bằng cách tách query week vs all/city — không truyền `Instant null` vào JPQL.
4. **Seed data** là điều kiện tiên quyết để UI không trống; không chỉ dựa vào mock TS.
5. **JWT:** `token` legacy vẫn tồn tại song song `accessToken`.

---

*Cập nhật: 2026-06-02 — gộp từ FE_API_HANDOFF + FE_BE_COMPAT_HANDOFF.*
