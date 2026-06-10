# CP3 Tuần 2 — BE đã làm / hỗ trợ gì

> Tuần 2 tập trung **Polish UI + Focus Group** (local, không deploy). BE **không thêm feature mới** — giữ API ổn định cho 5 màn FE polish.
>
> **Tiền đề:** [Tuần 1](../week-1/01-BE-DA-LAM.md) đã xong (cite nguồn, seed, smoke pagination).  
> **Toàn dự án:** [BE_PROJECT_STATUS_AND_FE_GUIDE.md](../BE_PROJECT_STATUS_AND_FE_GUIDE.md)

---

## 1. Mục tiêu CP3 Tuần 2 (Ngày 8 → 14)

| Ngày | Việc | Owner |
|------|------|-------|
| **8–9** | Polish UI: Explore, Quests, Profile, Leaderboard, Scan | FE |
| **10** | Loading / error / empty states | FE |
| **11** | Chuẩn bị Focus Group (script 90 phút, recruit 10–16 users) | Team |
| **12** | Focus Group buổi 1 (5–8 users) | Team |
| **13** | Focus Group buổi 2 + analyze insights | Team |
| **14** | BMC finalize, pitch deck, **fix bug từ feedback** | Team + BE/FE tùy bug |

**Lưu ý plan:** App chạy **local** trên laptop demo — BE phải `spring-boot:run` + DB seed đầy đủ trước buổi Focus Group.

---

## 2. Vai trò BE Tuần 2

| Việc | Trạng thái |
|------|------------|
| API cho 5 màn polish | **Đã có** từ Week 2–4 + compat 2026-06-02 |
| Feature mới Tuần 2 | **Không** (theo plan CP3) |
| Ổn định demo Focus Group | Script smoke màn hình mới |
| Fix bug từ Focus Group | **Ngày 14** — làm sau khi có feedback |

---

## 3. API theo từng màn (FE polish)

### Explore

| API | Paginated | Ghi chú |
|-----|-----------|---------|
| `GET /api/locations?page&size&sort&city&search` | **Có** (`data.items`) | Empty: `items: []`, `totalItems: 0` |
| `GET /api/locations/{id}` | — | 404 nếu không tồn tại |

### Quests

| API | Paginated | Auth |
|-----|-----------|------|
| `GET /api/quests?locationId=&page=&size=` | **Có** | Public |
| `GET /api/me/quests?status=&page=&size=` | **Có** | JWT |
| `POST /api/quests/{id}/start` | — | JWT |
| `GET /api/quests/{id}/progress` | — | JWT |

Progress DTO: `currentStep`, `stepsTotal`, `status` (`not_started` | `in_progress` | `completed`).

### Profile

| API | Auth | Empty state |
|-----|------|-------------|
| `GET /api/profile/me` | JWT | User mới: `totalPoints: 0`, level Explorer |
| `PATCH /api/profile/me` | JWT | Body optional |
| `GET /api/me/badges` | JWT | User mới: `data: []` (mảng rỗng, không lỗi) |

`ProfileMeResponse` có: `levelName`, `pointsToNextLevel`, `levelProgressPercent`.

### Leaderboard

| API | Paginated | Ghi chú |
|-----|-----------|---------|
| `GET /api/leaderboard?scope=all\|city\|week&city=` | **Không** | `data.entries[]` |

```json
{
  "scope": "all",
  "city": null,
  "entries": [
    { "rank": 1, "userId": "...", "displayName": "...", "totalPoints": 500, "currentUser": false }
  ]
}
```

- `scope=week` dùng query tách (đã fix lỗi PostgreSQL null parameter).
- Empty: `entries: []` — FE hiển thị empty state (Tuần 2 Ngày 10).
- Seed topup có nhiều profile `totalPoints` → thường không trống nếu đã chạy SQL.

### Scan (check-in)

| API | Auth | Lỗi nghiệp vụ |
|-----|------|----------------|
| `POST /api/checkins` | JWT | `422` `BUSINESS_RULE` — GPS xa, QR sai, đã check-in |

Body mẫu:

```json
{
  "locationId": "11111111-1111-1111-1111-111111111111",
  "latitude": 11.143,
  "longitude": 106.461,
  "qrCode": "timelens:location:11111111-1111-1111-1111-111111111111"
}
```

**Demo Focus Group:** Dùng tọa độ từ `GET /api/locations/{id}` hoặc `POST /api/demo/checkin` khi `DEMO_ENABLED=true` (nút cứu cánh GPS).

---

## 4. Error contract (FE Ngày 10)

| HTTP | code | FE xử lý |
|------|------|----------|
| 422 | `VALIDATION_ERROR` | `fieldErrors` + toast |
| 422 | `BUSINESS_RULE` | Toast message (GPS, rate limit chat, …) |
| 401 | `UNAUTHORIZED` | Redirect login |
| 404 | `NOT_FOUND` | Empty / not found UI |
| 500 | `INTERNAL_ERROR` | Toast + retry |

Validation = **422** (không phải 400).

---

## 5. Deliverable BE mới Tuần 2 (trong repo)

| File | Mục đích |
|------|----------|
| [smoke-week2-screens.ps1](../scripts/smoke-week2-screens.ps1) | Smoke 5 màn trước Focus Group |
| [03-THAO-TAC-BAN-TU-LAM.md](./03-THAO-TAC-BAN-TU-LAM.md) | Setup demo, recruit, buổi FG |
| [04-FOCUS-GROUP-SCRIPT.md](./04-FOCUS-GROUP-SCRIPT.md) | Script 90 phút mẫu |

Chạy smoke:

```powershell
powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-week2-screens.ps1
```

Chạy kèm golden path Tuần 1 (sau `run-all-seed.ps1`):

```powershell
powershell -ExecutionPolicy Bypass -File docs\scripts\run-all-seed.ps1
powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-golden-path.ps1
powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-week2-screens.ps1
```

Audit tổng: [CP3_AUDIT_STATUS.md](../CP3_AUDIT_STATUS.md)

---

## 6. SQL / env — không đổi so với Tuần 1

Vẫn 5 file SQL (4 compat + `week-1/2026-week1_location_sources.sql`). Không thêm migration Tuần 2.

| Biến | Focus Group |
|------|-------------|
| `GEMINI_API_KEY` | Chat demo trong buổi FG |
| `CORS_ALLOWED_ORIGINS` | `localhost:5173` + IP LAN nếu user dùng điện thoại |
| `DEMO_ENABLED` + `DEMO_SECRET` | Optional — cứu check-in khi GPS phòng yếu |
| `GAMIFICATION_CHECKIN_RADIUS_METERS` | Mặc định 100m |

---

## 7. Known gaps (chưa làm Tuần 2 — chờ feedback FG)

| Mục | Ghi chú |
|-----|---------|
| Leaderboard pagination | Vẫn `entries[]` — đủ cho polish |
| Refresh token persistent | In-memory |
| Media URL fallback | Placeholder URLs từ seed |
| Bug từ Focus Group | Xử lý Ngày 14 |

---

## 8. File liên quan

| File | Vai trò |
|------|---------|
| [02-FE-CAN-LAM.md](./02-FE-CAN-LAM.md) | Checklist FE Ngày 8–14 |
| [03-THAO-TAC-BAN-TU-LAM.md](./03-THAO-TAC-BAN-TU-LAM.md) | Việc bạn tự làm |
| [04-FOCUS-GROUP-SCRIPT.md](./04-FOCUS-GROUP-SCRIPT.md) | Script buổi test |
| [../week-1/](../week-1/) | Tuần 1 foundation |

---

**Tiếp theo:** [Tuần 3 — Deploy + 360 thật](../week-3/01-BE-DA-LAM.md)

---

*CP3 Tuần 2 — BE giữ ổn định API + smoke demo; polish UI thuộc FE.*
