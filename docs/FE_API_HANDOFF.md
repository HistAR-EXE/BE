# HistAR / TimeLens — Frontend API Handoff (Week 1)

Base URL (local): `http://localhost:8080`

CORS: origins from env `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`). Auth uses stateless JWT via `Authorization` header.

---

## Response conventions

### Success (`2xx`)

All successful API responses use **`ApiResponse<T>`**:

```json
{
  "success": true,
  "message": null,
  "data": { }
}
```

`message` is optional and often omitted (`null`).

### Errors (`4xx` / `5xx`)

Errors are **not** wrapped in `ApiResponse`. Shape:

```json
{
  "success": false,
  "code": "NOT_FOUND",
  "message": "Resource not found",
  "timestamp": "2026-06-01T10:00:00Z"
}
```

Validation (`400`):

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fieldErrors": { "email": "must be a well-formed email address" },
  "timestamp": "..."
}
```

| HTTP | `code` | Typical case |
|------|--------|----------------|
| 400 | `VALIDATION_ERROR` | Bean validation |
| 401 | `UNAUTHORIZED` | Missing/invalid JWT |
| 404 | `NOT_FOUND` | Entity not found |
| 409 | `CONFLICT` | Email already registered |
| 422 | `BUSINESS_RULE` | Gemini missing, rate limit, etc. |
| 500 | `INTERNAL_ERROR` | Unexpected error |

---

## Authentication

Register or login → read `data.token` → attach to protected routes:

```
Authorization: Bearer <jwt>
```

JWT contains `sub = email` (subject email). FE must use `data.userId` from login/register response, do not assume token includes a separate `userId` claim.

| Endpoint | Auth |
|----------|------|
| `GET /api/health`, `GET /api/health/ready` | Public |
| `POST /api/auth/register`, `POST /api/auth/login` | Public |
| `GET /api/locations/**`, `GET /api/characters/**`, `GET /api/photo-pairs/**`, `GET /api/panoramas/**`, `GET /api/hotspots/**` | Public |
| `GET /api/profile/me`, `POST /api/chat`, `GET /api/chat/conversations/{id}/messages` | **JWT required** |
| `POST /api/checkins`, `GET /api/me/**`, `POST /api/quests/{id}/start`, `GET /api/quests/{id}/progress`, `GET /api/locations/{id}/secret-story`, `POST /api/demo/checkin` | **JWT required** |
| `POST /api/user-creations`, `POST /api/user-creations/{id}/record-share` | **JWT required** |
| `GET /api/quests`, `GET /api/badges`, `GET /api/photo-frames`, `GET /api/leaderboard`, `GET /api/share/prefill` | Public |

`/api/locations/{id}/secret-story` is a protected exception under `/api/locations/**`.

---

## Seed data (Củ Chi pilot)

Fixed UUIDs in DB seed (safe to hardcode in FE for week 1):

| Resource | UUID |
|----------|------|
| Location — Địa đạo Củ Chi | `11111111-1111-1111-1111-111111111111` |
| Panorama — 360° tour | `22222222-2222-2222-2222-222222222222` |

**Characters** are inserted without fixed IDs. After `docker compose up`, call:

`GET /api/characters/by-location/11111111-1111-1111-1111-111111111111`

Use the returned `id` for chat (`characterId`). Expect 2 characters: *Chị Năm* and *Hướng dẫn viên lịch sử*.

Photo pairs, hotspots: IDs are dynamic; load via list endpoints below.

---

## Endpoints

### Health

`GET /api/health`

```json
{
  "success": true,
  "data": { "status": "UP", "service": "timelens-be" }
}
```

`GET /api/health/ready` returns raw JSON (not `ApiResponse`) and uses HTTP code to indicate readiness:

- `200`: `{ "status": "UP", "database": "UP" }`
- `503`: `{ "status": "DOWN", "database": "DOWN" }`

---

### Auth

#### `POST /api/auth/register`

Body:

```json
{
  "email": "user@example.com",
  "password": "secret123",
  "displayName": "Minh"
}
```

Response `data`:

```json
{
  "token": "<jwt>",
  "userId": "<uuid>",
  "displayName": "Minh"
}
```

#### `POST /api/auth/login`

Body:

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

Same `data` shape as register.

---

### Locations

#### `GET /api/locations`

`data`: array of `LocationResponse`

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "name": "Địa đạo Củ Chi",
  "description": "...",
  "latitude": 11.143,
  "longitude": 106.461,
  "city": "TP.HCM",
  "coverImage": "https://...",
  "createdAt": "..."
}
```

#### `GET /api/locations/{id}`

`data`: single `LocationResponse`

---

### Characters

`personaPrompt` is **never** exposed to the client (server-only for Gemini).

#### `GET /api/characters/by-location/{locationId}`

#### `GET /api/characters/{id}`

`data` item:

```json
{
  "id": "<uuid>",
  "locationId": "11111111-1111-1111-1111-111111111111",
  "name": "Chị Năm — Nữ du kích Củ Chi",
  "era": "1965-1975",
  "portraitUrl": "https://..."
}
```

---

### Photo pairs (before/after slider)

#### `GET /api/photo-pairs/by-location/{locationId}`

Ordered by `sortOrder`. `data` item:

```json
{
  "id": "<uuid>",
  "locationId": "...",
  "historicalImage": "https://...",
  "currentImage": "https://...",
  "year": 1968,
  "caption": "Cửa hầm địa đạo",
  "sortOrder": 1
}
```

---

### Panoramas (360°)

#### `GET /api/panoramas/by-location/{locationId}`

#### `GET /api/panoramas/{id}`

```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "locationId": "11111111-1111-1111-1111-111111111111",
  "imageUrl": "https://...",
  "title": "Địa đạo Củ Chi — góc nhìn 360°"
}
```

#### `POST /api/panoramas` (JWT, multipart) — CP3 Tuần 3

Upload ảnh equirectangular 2:1 lên MinIO + tạo record DB.

```
locationId, title, file (image/jpeg|png|webp, max 8MB)
```

#### `PUT /api/panoramas/{id}/image` (JWT, multipart)

Thay ảnh scene hiện có. Response: cùng shape `PanoramaResponse`.

---

### Hotspots

#### `GET /api/hotspots/by-panorama/{panoramaId}`

Example panorama: `22222222-2222-2222-2222-222222222222`

```json
{
  "id": "<uuid>",
  "panoramaId": "22222222-2222-2222-2222-222222222222",
  "yaw": 0.5,
  "pitch": 0.1,
  "type": "info",
  "contentRef": "tunnel-entrance",
  "label": "Cửa hầm chính"
}
```

`type`: `info` | `scene`. Với `scene`, `contentRef` = **UUID panorama đích** (virtual tour chuyển scene). Với `info`, `contentRef` là key nội dung UI.

---

### Profile (JWT)

#### `GET /api/profile/me`

```json
{
  "id": "<uuid>",
  "email": "user@example.com",
  "displayName": "Minh",
  "avatarUrl": null,
  "level": 1,
  "totalPoints": 0,
  "city": null
}
```

---

### Chat (JWT + Gemini)

Requires `GEMINI_API_KEY` on the server. Daily cap: `GEMINI_DAILY_MESSAGE_LIMIT` (default `50`) per user, counted from persisted user chat messages in DB (UTC day window).

#### `POST /api/chat`

Body:

```json
{
  "characterId": "<uuid from GET characters>",
  "message": "Chị Năm ơi, cuộc sống trong địa đạo thế nào?",
  "conversationId": null
}
```

- Omit `conversationId` or pass `null` to start a new conversation.
- Pass existing `conversationId` to continue a thread.

Response `data`:

```json
{
  "reply": "...(AI text)...",
  "conversationId": "<uuid>"
}
```

#### `GET /api/chat/conversations/{conversationId}/messages`

`data`: array ordered by time:

```json
{
  "id": "<uuid>",
  "role": "user",
  "content": "...",
  "createdAt": "..."
}
```

`role`: `user` | `assistant`

---

## Axios setup (example)

```typescript
import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Usage: const { data } = await api.get('/api/locations');
// FE reads payload from data.data (ApiResponse wrapper)
export function unwrap<T>(res: { data: { success: boolean; data: T } }): T {
  return res.data.data;
}
```

Register + load location:

```typescript
const reg = await api.post('/api/auth/register', {
  email: 'demo@histar.local',
  password: 'Demo1234!',
  displayName: 'Demo',
});
localStorage.setItem('token', reg.data.data.token);

const locations = unwrap(await api.get('/api/locations'));
const cuChiId = '11111111-1111-1111-1111-111111111111';
const pairs = unwrap(await api.get(`/api/photo-pairs/by-location/${cuChiId}`));
const characters = unwrap(
  await api.get(`/api/characters/by-location/${cuChiId}`)
);
const chat = await api.post('/api/chat', {
  characterId: characters[0].id,
  message: 'Xin chào!',
});
```

---

## cURL smoke test

```bash
# 1. Health
curl -s http://localhost:8080/api/health

# 2. Register
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!","displayName":"Test"}'

# Save TOKEN from .data.token in JSON response

# 3. Locations
curl -s http://localhost:8080/api/locations

# 4. Photo pairs
curl -s http://localhost:8080/api/photo-pairs/by-location/11111111-1111-1111-1111-111111111111

# 5. Characters (copy first id)
curl -s http://localhost:8080/api/characters/by-location/11111111-1111-1111-1111-111111111111

# 6. Chat (needs GEMINI_API_KEY in .env)
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"characterId":"CHAR_UUID","message":"Xin chào"}'
```

---

## Local backend setup

```bash
# From BE/
cp .env.example .env
# Set GEMINI_API_KEY=... for chat

docker compose up -d
# Schema: docs/database/TimeLens_DB_Schema.sql (mounted vào Postgres lúc init)
# Nếu DB cũ thiếu panorama seed: docker compose down -v && docker compose up -d

./mvnw spring-boot:run
# Or: mvnw -Dspring-boot.run.profiles=default
```

| Service | URL |
|---------|-----|
| API | http://localhost:8080 |
| Postgres | localhost:5432 (`timelens` / see `.env`) |
| MinIO API | http://localhost:9000 |
| MinIO Console | http://localhost:9001 |

---

## Week 2 — Gamification (BE progress)

```text
[x] T1 APIs  [x] check-in  [x] quest  [x] XP/level  [x] badge  [x] secret unlock
```

**Scope MVP:** Quest hoàn thành khi **check-in** đúng location (1 bước). FE vẫn có thể hiển thị 3 objective UI; BE chỉ validate check-in.

### Seed IDs (Củ Chi)

| Resource | UUID |
|----------|------|
| Location | `11111111-1111-1111-1111-111111111111` |
| Quest — Hành trình dưới lòng đất | `33333333-3333-3333-3333-333333333333` |
| Badge — Người khám phá | `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` |
| Badge — Nhà sử học nhí | `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb` |
| Badge — Lần đầu check-in | `cccccccc-cccc-cccc-cccc-cccccccccccc` |

### QR format

| Loại | Payload mẫu |
|------|-------------|
| Check-in | `timelens:location:11111111-1111-1111-1111-111111111111` hoặc raw UUID |
| Secret unlock | `timelens:secret:11111111-1111-1111-1111-111111111111` (sau khi hoàn thành quest) |

GPS phụ: server từ chối nếu xa hơn **100m** (config `GAMIFICATION_CHECKIN_RADIUS_METERS`).

### Level thresholds

| Level | Tên | Điểm tối thiểu |
|-------|-----|----------------|
| 1 | Explorer | 0 |
| 2 | Time Traveler | 100 |
| 3 | History Hunter | 300 |
| 4 | Legend | 700 |

---

### Check-in (JWT)

`POST /api/checkins`

```json
{
  "locationId": "11111111-1111-1111-1111-111111111111",
  "latitude": 11.143,
  "longitude": 106.461,
  "qrCode": "timelens:location:11111111-1111-1111-1111-111111111111"
}
```

Response `data`:

```json
{
  "success": true,
  "distanceMeters": 12.5,
  "questsCompleted": ["33333333-3333-3333-3333-333333333333"],
  "badgesEarned": [{ "id": "...", "name": "...", "iconUrl": "..." }],
  "secretUnlocked": false
}
```

Secret QR: cùng body nhưng `qrCode` dùng prefix `timelens:secret:` — **không** ghi check-in mới, chỉ unlock nếu quest đã `completed`.

---

### Quest

| Method | Path | Auth |
|--------|------|------|
| GET | `/api/quests?locationId=` | Public |
| GET | `/api/me/quests?locationId=` | JWT |
| POST | `/api/quests/{id}/start` | JWT |
| GET | `/api/quests/{id}/progress` | JWT |

Status: `not_started` → `in_progress` → `completed` (không nhảy cóc).

`QuestProgressResponse`: `questId`, `locationId`, `title`, `description`, `pointsReward`, `status`, `startedAt`, `completedAt`.

**Flow FE:** `start` → user check-in → quest `completed` + points trong response check-in.

---

### Badges

| Method | Path | Auth |
|--------|------|------|
| GET | `/api/badges` | Public (catalog) |
| GET | `/api/me/badges` | JWT (`earned` true/false) |

---

### Secret story (JWT)

`GET /api/locations/{locationId}/secret-story`

```json
{
  "locked": true,
  "title": "Câu chuyện bí mật",
  "story": null
}
```

Khi đã unlock: `locked: false`, `story` = nội dung từ quest seed.

---

### Profile (mở rộng T2)

`GET /api/profile/me` thêm: `levelName`, `pointsToNextLevel`, `levelProgressPercent`.

---

### cURL smoke (week 2)

```bash
TOKEN="..." # từ register/login
LOC=11111111-1111-1111-1111-111111111111
QUEST=33333333-3333-3333-3333-333333333333

curl -s -X POST http://localhost:8080/api/quests/$QUEST/start \
  -H "Authorization: Bearer $TOKEN"

curl -s -X POST http://localhost:8080/api/checkins \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"locationId\":\"$LOC\",\"latitude\":11.143,\"longitude\":106.461,\"qrCode\":\"timelens:location:$LOC\"}"

curl -s http://localhost:8080/api/profile/me -H "Authorization: Bearer $TOKEN"

curl -s -X POST http://localhost:8080/api/checkins \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"locationId\":\"$LOC\",\"latitude\":11.143,\"longitude\":106.461,\"qrCode\":\"timelens:secret:$LOC\"}"

curl -s http://localhost:8080/api/locations/$LOC/secret-story -H "Authorization: Bearer $TOKEN"
```

**DB migration:** Nếu DB cũ thiếu bảng `user_secret_unlocks` / quest UUID cố định → `docker compose down -v && docker compose up -d`.

---

## Week 3 — Viral loop (BE progress)

```text
[x] T1  [x] T2 gamification  [x] photo-frames  [x] upload/creations  [x] share prefill  [x] leaderboard
```

**Scope MVP:** FE composite canvas + watermark; BE lưu ảnh export lên **MinIO**, catalog frame, leaderboard theo điểm T2. Social share = **Web Share API trên FE** (+ API ghi nhận share để cộng điểm).

### Photo frames (public)

`GET /api/photo-frames`

```json
{
  "id": "<uuid>",
  "name": "Khung du kích",
  "imageUrl": "https://...",
  "era": "1968",
  "sortOrder": 1
}
```

Load `imageUrl` với `crossOrigin="anonymous"` để `canvas.toBlob()` không bị taint.

### Upload creation (JWT, multipart)

`POST /api/user-creations` — `Content-Type: multipart/form-data`

| Field | Type | Mô tả |
|-------|------|--------|
| `file` | file | Ảnh đã composite (JPEG/PNG/WebP, max 8MB) |
| `frameId` | UUID | Khung đã chọn |
| `variant` | string | `square` (1080×1080) hoặc `story` (9:16) — metadata |

Response `data`:

```json
{
  "id": "<uuid>",
  "frameId": "...",
  "outputUrl": "http://localhost:9000/timelens-media/creations/{userId}/{file}.jpg",
  "variant": "story",
  "createdAt": "...",
  "shared": false
}
```

`GET /api/me/user-creations` — lịch sử ảnh đã tạo (JWT).

### Share (FE + optional bonus)

`GET /api/share/prefill` (public) — caption/hashtag gợi ý cho `navigator.share()`.

```json
{
  "caption": "Khám phá di sản Việt cùng TimeLens! #TimeLens #DiSanVietNam",
  "hashtags": ["#TimeLens", "#DiSanVietNam"]
}
```

Sau khi user share (Web Share API hoặc fallback download), gọi:

`POST /api/user-creations/{id}/record-share` (JWT) — cộng **15 điểm** một lần/creation (`VIRAL_SHARE_BONUS_POINTS`).

### Leaderboard

`GET /api/leaderboard?scope=all|city|week&city=TP.HCM`

- `scope=all` — toàn bộ user theo `total_points`
- `scope=city` — lọc `profiles.city` (bắt buộc `city`)
- `scope=week` — user có check-in hoặc creation trong **7 ngày** gần nhất

JWT **tùy chọn** — nếu có token, entry của user có `currentUser: true`. Chỉ trả `displayName`, **không** trả email.

```json
{
  "scope": "all",
  "city": null,
  "entries": [
    {
      "rank": 1,
      "userId": "...",
      "displayName": "Minh",
      "avatarUrl": null,
      "totalPoints": 250,
      "currentUser": false
    }
  ]
}
```

Cache server ~60s (`VIRAL_LEADERBOARD_CACHE_SECONDS`).

### Common `422 BUSINESS_RULE` cases FE should handle

| API | Case | Example message |
|-----|------|-----------------|
| `POST /api/chat` | Daily chat limit exceeded | `Đã đạt giới hạn 50 tin nhắn/ngày` |
| `POST /api/checkins` | GPS too far | `Bạn đang cách địa điểm quá xa...` |
| `POST /api/checkins` | QR payload mismatch | `Mã QR không khớp địa điểm` |
| `GET /api/leaderboard` | Invalid `scope` or missing `city` when `scope=city` | `scope phải là all, city hoặc week` |
| `POST /api/user-creations` | Empty file / invalid MIME / invalid variant | `Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP` |
| `POST /api/demo/checkin` | Demo secret missing/invalid | `Invalid demo secret` |

### MinIO URLs

Ảnh upload public-read (dev): `http://localhost:9000/timelens-media/creations/...`

Production: set `MINIO_PUBLIC_URL` trỏ domain CDN/public gateway.

### cURL smoke (week 3)

```bash
curl -s http://localhost:8080/api/photo-frames
curl -s http://localhost:8080/api/share/prefill
curl -s "http://localhost:8080/api/leaderboard?scope=all"

curl -s -X POST http://localhost:8080/api/user-creations \
  -H "Authorization: Bearer $TOKEN" \
  -F "frameId=<FRAME_UUID>" \
  -F "variant=story" \
  -F "file=@export.jpg"

curl -s -X POST http://localhost:8080/api/user-creations/<CREATION_ID>/record-share \
  -H "Authorization: Bearer $TOKEN"
```

**DB migration T3:** Cột `user_creations.variant`, `user_creations.shared_at` — `docker compose down -v` nếu DB cũ.

---

## Backend package conventions (SaaS)

| Package | Vai trò |
|---------|---------|
| `common/` | Chỉ cross-cutting: `exception`, `response`, `security` helper, `audit` — **không** có `@RestController` |
| `config/` | Cấu hình app-level (`DataInitialize`) |
| `security/` | JWT filter, `SecurityConfig` |
| `{module}/` | Mỗi domain: `controller`, `dto`, `service`, `service/impl`, `entity`, `repository` |
| `health/` | Health check (`GET /api/health`) |
| `chat/config/` | WebClient Gemini (config gắn module chat) |

---

## Week 1 scope notes (FE)

- **In scope:** auth, location detail, photo slider, 360 panorama + hotspots, AI chat, profile me.
- **Week 2 (done):** check-in, quest, badges, XP/level on profile, secret story.
- **Week 3 (done):** photo frames catalog, upload creations (MinIO), share prefill + record-share bonus, leaderboard.
- **Deploy:** Railway/hosting is backend team follow-up; point `VITE_API_URL` to deployed URL when ready.

---

## Suggested FE page → API map

| Page / feature | APIs |
|----------------|------|
| Login / Register | `POST /api/auth/login`, `register` |
| Home / map | `GET /api/locations` |
| Location detail | `GET /api/locations/{id}`, `photo-pairs/by-location/{id}`, `characters/by-location/{id}` |
| 360 tour | `panoramas/by-location/{id}`, `hotspots/by-panorama/{panoramaId}` |
| Chat | `POST /api/chat`, `GET .../messages` |
| Profile | `GET /api/profile/me`, `GET /api/me/badges` |
| QR check-in | `POST /api/checkins` |
| Quests | `GET /api/quests`, `/api/me/quests`, `POST .../start` |
| Secret | `GET /api/locations/{id}/secret-story` |
| Photo frame | `GET /api/photo-frames`, `POST /api/user-creations`, `GET /api/me/user-creations` |
| Share | `GET /api/share/prefill`, `POST .../record-share` |
| Leaderboard | `GET /api/leaderboard?scope=` |

---

## Week 4 update (Demo + Production freeze)

Scope week 4: no new product features. Backend focuses on stability, deploy readiness, and demo backup.

### BE progress for FE

| Item | Status |
|------|--------|
| Week 1-3 APIs | Done |
| `GET /api/health/ready` | Done |
| `POST /api/demo/checkin` | Done |
| Dockerfile + `application-prod.yml` + `docs/DEPLOY.md` | Done |
| Smoke script `docs/scripts/smoke-golden-path.ps1` | Done |
| Sentry/full observability | Out of scope |

### New API in week 4

#### Readiness endpoint

`GET /api/health/ready` (public)

- `200`: DB is ready
- `503`: DB is unavailable

Example response:

```json
{ "status": "UP", "database": "UP" }
```

#### Demo backup check-in

`POST /api/demo/checkin`

Headers:

- `Authorization: Bearer <token>`
- `X-Demo-Secret: <DEMO_SECRET>`

Body:

```json
{ "locationId": "11111111-1111-1111-1111-111111111111" }
```

Rules:

- Endpoint is active only when `DEMO_ENABLED=true`
- Keep production default as `DEMO_ENABLED=false`
- Use this only as fallback when QR/GPS fails during demo

### Guest vs Auth reminder

Public APIs remain: locations, characters, photo-pairs, panoramas, hotspots, quests list, badges list, photo-frames, share prefill, leaderboard.

JWT-required APIs: chat, profile, check-in, quest start/progress, secret story, user-creations upload/list/share, demo check-in.

### Stable seed IDs for FE demo

- Location Cu Chi: `11111111-1111-1111-1111-111111111111`
- Panorama: `22222222-2222-2222-2222-222222222222`
- Quest: `33333333-3333-3333-3333-333333333333`

### Smoke command after deploy

```powershell
.\docs\scripts\smoke-golden-path.ps1 -BaseUrl https://<be-host>
```
