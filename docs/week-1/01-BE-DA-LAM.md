# CP3 Tuần 1 — BE đã làm gì

> Tài liệu tổng hợp cho team FE và AI (Claude). Bám [TimeLens_CP3_Plan_Final.docx](../../TimeLens_CP3_Plan_Final.docx) + nền tảng BE Week 1–4 + sprint compat 2026-06-02.
>
> **Base URL local:** `http://localhost:8080`  
> **Chi tiết toàn dự án:** [BE_PROJECT_STATUS_AND_FE_GUIDE.md](../BE_PROJECT_STATUS_AND_FE_GUIDE.md)

---

## 1. Mục tiêu CP3 Tuần 1 (7 ngày)

Làm trên **local**, ảnh 360 tạm Google Street View. Deploy production và ảnh 360 thật dời sang **Tuần 3**.

| Ngày | Việc | Owner |
|------|------|-------|
| 1 | Chạy SQL seed, test golden path end-to-end | BE + FE |
| 2 | Màn chọn mode sau login: Khám phá từ xa / Tại di tích | FE |
| 3 | ONLINE flow: Explore → Tour360 → Chat → Photo slider | FE |
| 4 | OFFLINE flow: Check-in → Quest → Secret → Frame → Share | FE |
| 5 | AI Chat cite nguồn + guardrail chống bịa | **BE** |
| 6 | Virtual Tour đa scene (Photo Sphere Viewer plugin) | FE |
| 7 | Buffer, test mobile qua LAN, internal review | FE + team |

---

## 2. Nền tảng BE đã có (trước CP3 Tuần 1)

### 2.1 Week 1 cốt lõi (content)

| API | Mô tả |
|-----|--------|
| `POST /api/auth/register`, `login` | JWT + userId |
| `POST /api/auth/refresh`, `logout` | Refresh token (in-memory) |
| `GET /api/locations` | Danh sách địa điểm (**paginated**) |
| `GET /api/locations/{id}` | Chi tiết địa điểm |
| `GET /api/characters/by-location/{id}`, `/{id}` | Nhân vật AI (không trả `personaPrompt`) |
| `GET /api/photo-pairs/by-location/{id}` | Slider xưa/nay |
| `GET /api/panoramas/by-location/{id}`, `/{id}` | 360° scenes |
| `GET /api/hotspots/by-panorama/{id}` | Hotspot trong panorama |
| `POST /api/chat` | Chat Gemini (JWT) |
| `GET /api/chat/conversations/{id}/messages` | Lịch sử chat (**paginated**) |
| `GET /api/profile/me` | Profile (JWT) |
| `PATCH /api/profile/me` | Cập nhật displayName, avatarUrl, city |
| `GET /api/health`, `GET /api/health/ready` | Health / readiness |

### 2.2 Week 2–4 (đã có, dùng trong OFFLINE flow)

Check-in, quest, badge, secret story, photo frame, upload MinIO, share, leaderboard, demo check-in — xem [FE_API_HANDOFF.md](../FE_API_HANDOFF.md).

### 2.3 Sprint compat 2026-06-02

- Error contract: `422` cho `VALIDATION_ERROR`, `401/403/404` thống nhất
- Pagination: `items`, `page`, `size`, `totalItems`, `totalPages`
- Location DTO: `rating`, `distanceKm`, `isArAvailable`
- Observability: `X-Request-Id`, actuator health/metrics
- DB seed dày (≥10 record/domain)

---

## 3. Việc BE mới làm trong CP3 Tuần 1

### 3.1 AI cite nguồn (Ngày 5 — Prompt #2)

**File:** `src/main/java/com/histar/be/chat/service/impl/ChatServiceImpl.java`

- Load `Location` qua `character.locationId` khi build prompt
- System prompt yêu cầu:
  - Cuối mỗi câu trả lời: dòng `Nguồn: ...` từ danh sách nguồn hợp lệ
  - Câu hỏi ngoài phạm vi: trả câu từ chối chuẩn, không bịa số liệu
  - Giữ giọng nhân vật từ `personaPrompt`
- **Không đổi contract** `POST /api/chat` — `reply` vẫn là string, FE hiển thị nguyên văn (có dòng Nguồn cuối)

**Cột DB mới (nội bộ, không expose API):**

- `locations.sources` (TEXT) — dùng cho prompt
- Migration: [2026-week1_location_sources.sql](./2026-week1_location_sources.sql)

**Ví dụ reply mong đợi:**

```
Chào em! Trong địa đạo, chị và các đồng đội thường sinh hoạt rất kín đáo, tránh tiếng động lớn...

Nguồn: Khu di tích lịch sử Địa đạo Củ Chi; Ban Quản lý Di tích Củ Chi
```

**Câu ngoài phạm vi:**

```
Mình chưa có thông tin chính xác về điều này. Bạn nên tham khảo tài liệu chính thức từ Ban quản lý di tích.

Nguồn: Khu di tích lịch sử Địa đạo Củ Chi
```

### 3.2 Smoke script cập nhật pagination (Ngày 1)

**File:** [docs/scripts/smoke-golden-path.ps1](../scripts/smoke-golden-path.ps1)

- `GET /api/locations` → `data.items[0].id`
- `GET /api/quests` → `data.items[0].id`

---

## 4. Contract API — phần FE Tuần 1 cần biết

### 4.1 Envelope

**Success:** `{ "success": true, "message": null, "data": { ... } }`

**Error:** `{ "success": false, "code": "...", "message": "...", "timestamp": "...", "fieldErrors": {} }`

| HTTP | code | Ghi chú |
|------|------|---------|
| 422 | `VALIDATION_ERROR` | Có `fieldErrors` |
| 422 | `BUSINESS_RULE` | Rate limit chat, GPS xa, v.v. |
| 401 | `UNAUTHORIZED` | Thiếu/sai JWT |
| 403 | `FORBIDDEN` | Không đủ quyền |
| 404 | `NOT_FOUND` | Không tìm thấy |
| 409 | `CONFLICT` | Email đã tồn tại |

### 4.2 Auth

Login/register `data`:

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

Header: `Authorization: Bearer <accessToken>` (hoặc `token` — cùng giá trị).

### 4.3 Locations (paginated)

`GET /api/locations?page=0&size=20&sort=createdAt,desc&city=...&search=...&nearLat=...&nearLng=...`

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "11111111-1111-1111-1111-111111111111",
        "name": "Địa đạo Củ Chi",
        "description": "...",
        "latitude": 11.143,
        "longitude": 106.461,
        "city": "TP.HCM",
        "coverImage": "https://...",
        "rating": 0,
        "distanceKm": null,
        "isArAvailable": false,
        "createdAt": "..."
      }
    ],
    "page": 0,
    "size": 20,
    "totalItems": 12,
    "totalPages": 1
  }
}
```

### 4.4 Chat (JWT)

`POST /api/chat` body:

```json
{
  "characterId": "<uuid>",
  "message": "...",
  "conversationId": null
}
```

Response:

```json
{
  "success": true,
  "data": {
    "reply": "...(có dòng Nguồn: cuối)...",
    "conversationId": "<uuid>"
  }
}
```

`GET /api/chat/conversations/{id}/messages?page=0&size=20` → `data.items[]`

### 4.5 Endpoints list vẫn trả mảng trực tiếp

`characters`, `photo-pairs`, `panoramas`, `hotspots` — `data` là array, không paginate.

---

## 5. Thứ tự chạy SQL (local)

```powershell
docker compose down -v && docker compose up -d
powershell -ExecutionPolicy Bypass -File docs\scripts\run-all-seed.ps1
```

Script chạy **6 file** (plan gốc 4 + sources Tuần 1 + panorama/hotspot Tuần 3). Chi tiết: [CP3_AUDIT_STATUS.md](../CP3_AUDIT_STATUS.md).

Sau đó:

```bash
./mvnw spring-boot:run
powershell -ExecutionPolicy Bypass -File docs/scripts/smoke-golden-path.ps1
powershell -ExecutionPolicy Bypass -File docs/scripts/smoke-week2-screens.ps1
```

---

## 6. UUID demo Củ Chi (hardcode FE được)

| Resource | UUID |
|----------|------|
| Location — Địa đạo Củ Chi | `11111111-1111-1111-1111-111111111111` |
| Panorama — 360° | `22222222-2222-2222-2222-222222222222` |
| Quest (nếu cần) | `33333333-3333-3333-3333-333333333333` |

Characters: gọi `GET /api/characters/by-location/11111111-1111-1111-1111-111111111111` — expect 2 nhân vật (Chị Năm, Hướng dẫn viên).

---

## 7. Curl smoke nhanh

```bash
# Health
curl -s http://localhost:8080/api/health

# Locations (paginated)
curl -s "http://localhost:8080/api/locations?page=0&size=5"

# Register + chat (cần GEMINI_API_KEY)
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@histar.vn","password":"Test1234!","displayName":"Test"}'
```

---

## 8. Known gaps (không block Tuần 1)

| Mục | Ghi chú |
|-----|---------|
| Refresh token | In-memory, mất khi restart BE |
| `locations.sources` | Không trả ra API, chỉ dùng prompt |
| `rating` / `isArAvailable` | Map từ DB sau `fe_compat_migration.sql` |
| Virtual Tour đa scene | FE có thể dùng Street View tạm; BE API panoramas/hotspots đủ |
| Online/Offline mode | Thuần FE — không cần API BE |

---

## 9. File liên quan

| File | Vai trò |
|------|---------|
| [02-FE-CAN-LAM.md](./02-FE-CAN-LAM.md) | Checklist FE Tuần 1 |
| [03-THAO-TAC-BAN-TU-LAM.md](./03-THAO-TAC-BAN-TU-LAM.md) | Thao tác bạn cần tự chạy (SQL, smoke, chat, LAN) |
| [BE_PROJECT_STATUS_AND_FE_GUIDE.md](../BE_PROJECT_STATUS_AND_FE_GUIDE.md) | Master handoff toàn dự án |
| [FE_API_HANDOFF.md](../FE_API_HANDOFF.md) | API reference Week 1–4 |
| [2026-week1_location_sources.sql](./2026-week1_location_sources.sql) | Migration sources Tuần 1 |

---

**Tiếp theo:** [Tuần 2 — Polish + Focus Group](../week-2/01-BE-DA-LAM.md)

---

*Cập nhật: CP3 Tuần 1 — BE cite nguồn + smoke pagination + docs.*
