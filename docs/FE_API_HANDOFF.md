# HistAR / TimeLens — Frontend API Handoff (Week 1)

Base URL (local): `http://localhost:8080`

CORS: origins from env `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`). Send credentials if using cookies; JWT is sent via header (see below).

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

JWT payload includes user id (used server-side for profile/chat).

| Endpoint | Auth |
|----------|------|
| `GET /api/health` | Public |
| `POST /api/auth/register`, `POST /api/auth/login` | Public |
| `GET /api/locations/**`, `characters/**`, `photo-pairs/**`, `panoramas/**`, `hotspots/**` | Public |
| `GET /api/profile/me`, `POST /api/chat`, `GET /api/chat/conversations/{id}/messages` | **JWT required** |

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

`type`: `info` | `scene` (string from DB). `contentRef` is an opaque key for FE routing.

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

Requires `GEMINI_API_KEY` on the server. Daily cap: `GEMINI_DAILY_MESSAGE_LIMIT` (default `50`) per user.

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

## Backend package conventions (SaaS)

| Package | Vai trò |
|---------|---------|
| `common/` | Chỉ cross-cutting: `exception`, `response`, `security` helper, `audit` — **không** có `@RestController` |
| `config/` | Cấu hình app-level (`DataInitialize`) |
| `security/` | JWT filter, `SecurityConfig` |
| `{module}/` | Mỗi domain: `controller`, `dto`, `service`, `service/impl`, `entity`, `repository` |
| `health/` | Health check (`GET /api/health`) |
| `chat/config/` | WebClient Gemini (config gắn module chat) |

Tuần 2+ mới thêm API: quest, badge, check-in, photo-frame upload, leaderboard.

---

## Week 1 scope notes (FE)

- **In scope:** auth, location detail, photo slider, 360 panorama + hotspots, AI chat, profile me.
- **Not in week-1 APIs yet:** quests, badges, check-ins, leaderboard, MinIO upload, photo frames.
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
| Profile | `GET /api/profile/me` |
