# CP3 Tuần 3 — BE đã làm / hỗ trợ gì

> Tuần cuối CP3: **ảnh 360 thật + deploy production + demo/pitch**.  
> **FREEZE feature** — không thêm feature ngoài plan; có API upload panorama 360 (Tuần 3 Ngày 16).
>
> **Tiền đề:** [Tuần 1](../week-1/01-BE-DA-LAM.md), [Tuần 2](../week-2/01-BE-DA-LAM.md)  
> **Deploy chi tiết:** [05-DEPLOY-PRODUCTION.md](./05-DEPLOY-PRODUCTION.md) · [DEPLOY.md](../DEPLOY.md)

---

## 1. Mục tiêu CP3 Tuần 3 (Ngày 15 → 21)

| Ngày | Việc | Owner |
|------|------|-------|
| **15** | Chuyến Củ Chi — chụp 360 (5–7 điểm) + ảnh xưa/nay | **Bạn** (field) |
| **16** | Xử lý ảnh, upload MinIO, **UPDATE DB panoramas** | **Bạn** + BE SQL |
| **17** | **Deploy BE production** (Railway/Render) | **Bạn** |
| **18** | Survey online 10+ users (link prod) | CEO/CMO + **Bạn** |
| **19** | Video demo 3 phút + pitch deck | Team |
| **20** | Rehearsal pitch, test setup demo | Team |
| **21** | Final check production + buffer | Team |

**Customer testing 20+:** Focus Group Tuần 2 (10–16) + Survey Tuần 3 (10+) = đủ yêu cầu CP3.

---

## 2. Vai trò BE Tuần 3

| Việc | Trạng thái trong repo |
|------|------------------------|
| API mới | **Không** (freeze) |
| Dockerfile + `application-prod.yml` | **Đã có** từ Week 4 |
| SQL cập nhật panorama Củ Chi | **Mới** — [2026-week3_update_panoramas_cu_chi.sql](./2026-week3_update_panoramas_cu_chi.sql) |
| Smoke production | **Mới** — [smoke-production.ps1](../scripts/smoke-production.ps1) |
| Hướng dẫn chụp/upload 360 | [04-HUONG-DAN-360-UPLOAD.md](./04-HUONG-DAN-360-UPLOAD.md) |
| Deploy Railway step-by-step | [05-DEPLOY-PRODUCTION.md](./05-DEPLOY-PRODUCTION.md) |

---

## 3. Panorama / Virtual Tour (Ngày 16)

### API hiện có (không đổi)

| API | Ghi chú |
|-----|---------|
| `GET /api/panoramas/by-location/{locationId}` | Danh sách scene |
| `GET /api/panoramas/{id}` | `imageUrl` phải là HTTPS public |
| `GET /api/hotspots/by-panorama/{id}` | `type: scene` + `contentRef` = UUID panorama đích |

### UUID Củ Chi

| Scene | UUID |
|-------|------|
| Cổng vào (seed gốc) | `22222222-2222-2222-2222-222222222222` |
| Bếp Hoàng Cầm (thêm) | `22222222-2222-2222-2222-222222222221` |
| Phòng họp (thêm) | `22222222-2222-2222-2222-222222222223` |

Sau upload MinIO: chạy SQL template (thay `YOUR-CDN`).

**Yêu cầu ảnh:** equirectangular 2:1, nén \< 3MB/scene (mobile).

### Fallback

Nếu chuyến đi không đạt → giữ Street View / placeholder, vẫn demo được (plan CP3).

---

## 4. Deploy production (Ngày 17)

### Stack

| Thành phần | Gợi ý |
|------------|--------|
| BE | Railway / Render — Dockerfile root |
| DB | Managed Postgres |
| Storage | MinIO cloud / S3-compatible, bucket **public-read** |
| FE | Vercel — `VITE_API_URL` → BE prod |

### Env bắt buộc

`SPRING_PROFILES_ACTIVE=prod`, `DB_*`, `JWT_SECRET`, `GEMINI_API_KEY`, `CORS_ALLOWED_ORIGINS` (domain Vercel), `MINIO_*`, `MINIO_PUBLIC_URL` (HTTPS).

### DB production — thứ tự SQL

1. `TimeLens_DB_Schema.sql`
2. `2026-06-02_fe_compat_migration.sql`
3. `2026-06-02_fe_compat_indexes_seed.sql`
4. `2026-06-02_fe_compat_data_topup.sql`
5. `week-1/2026-week1_location_sources.sql`
6. `week-3/2026-week3_update_panoramas_cu_chi.sql` (sau khi có URL ảnh thật)

`ddl-auto: none` trên prod — schema phải khớp trước khi start app.

### Verify sau deploy

```powershell
powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-production.ps1 -BaseUrl https://<be-host>
```

Health: `GET /api/health/ready` → 200, `"database": "UP"`.

---

## 5. Demo / pitch (Ngày 19–21)

| Backup | Cách |
|--------|------|
| Video 3 phút | FE quay golden path 2 mode |
| QR/GPS fail live | `DEMO_ENABLED=true` + `POST /api/demo/checkin` |
| WiFi yếu | Mobile hotspot riêng |
| AR question | Trả lời: web MVP, AR roadmap EXE201+ |

Tài khoản presenter: đăng ký `demo@histar.vn` / `Demo@2026` trên **production** trước buổi.

---

## 6. Known limits (không fix Tuần 3)

| Mục | Ghi chú |
|-----|---------|
| Refresh token | In-memory — user đăng nhập lại nếu BE restart |
| Upload panorama | `POST /api/panoramas`, `PUT /api/panoramas/{id}/image` (JWT) |
| Leaderboard pagination | Vẫn `entries[]` |

---

## 7. File Tuần 3

| File | Vai trò |
|------|---------|
| [02-FE-CAN-LAM.md](./02-FE-CAN-LAM.md) | FE: Vercel URL, thay ảnh tour, video |
| [03-THAO-TAC-BAN-TU-LAM.md](./03-THAO-TAC-BAN-TU-LAM.md) | Checklist tay Ngày 15–21 |
| [04-HUONG-DAN-360-UPLOAD.md](./04-HUONG-DAN-360-UPLOAD.md) | Chụp + upload + SQL |
| [05-DEPLOY-PRODUCTION.md](./05-DEPLOY-PRODUCTION.md) | Railway/Render từng bước |

---

*CP3 Tuần 3 — freeze code, ship production + ảnh thật.*
