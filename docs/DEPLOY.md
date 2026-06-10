# TimeLens BE — Deploy production

Tuần 4: deploy **trước** ngày demo ít nhất 1 ngày. Không thêm feature mới — chỉ chạy stack W1–W3 ổn định.

## 1. Hạ tầng

| Thành phần | Gợi ý |
|------------|--------|
| API | Railway hoặc Render (Dockerfile trong repo) |
| Postgres | Managed DB (Railway Postgres, Neon, Supabase…) |
| Object storage | MinIO cloud hoặc S3-compatible (bucket **public-read** cho ảnh frame) |
| FE | Vercel — `VITE_API_URL=https://<be-host>` |

## 2. Schema DB (một lần)

Chạy toàn bộ [`database/TimeLens_DB_Schema.sql`](./database/TimeLens_DB_Schema.sql) trên Postgres production **trước** khi start app (`ddl-auto: none` trên prod).

## 3. Biến môi trường (production)

Copy từ [`.env.example`](../.env.example). Bắt buộc:

| Biến | Ghi chú |
|------|---------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | JDBC Postgres managed |
| `JWT_SECRET` | ≥ 32 ký tự, không dùng default |
| `GEMINI_API_KEY` | Chat live demo |
| `CORS_ALLOWED_ORIGINS` | URL FE production (vd. `https://timelens.vercel.app`) |
| `MINIO_ENDPOINT`, `MINIO_PUBLIC_URL`, keys, `MINIO_BUCKET_NAME` | `MINIO_PUBLIC_URL` phải là HTTPS browser load được ảnh |
| `DEMO_ENABLED` | `false` trên prod thật; `true` chỉ khi rehearse |
| `DEMO_SECRET` | Chỉ khi `DEMO_ENABLED=true` |

## 4. Build & chạy Docker

```bash
docker build -t timelens-be .
docker run -p 8080:8080 --env-file .env -e SPRING_PROFILES_ACTIVE=prod timelens-be
```

Railway/Render: trỏ **Dockerfile** root, set env trong dashboard, health check path: `/api/health/ready`.

## 5. Health probes

| Path | Mục đích |
|------|----------|
| `GET /api/health` | Liveness — app up |
| `GET /api/health/ready` | Readiness — DB `SELECT 1` OK (503 nếu DB down) |

## 6. Tài khoản demo (presenter)

Không seed user trong SQL. Đăng ký một lần qua API:

- Email: `demo@histar.vn`
- Password: `Demo@2026`

Hoặc `POST /api/auth/register` trước buổi demo, login sẵn trên thiết bị trình bày.

## 7. Manual check-in (backup QR/GPS)

Khi `DEMO_ENABLED=true` và `DEMO_SECRET` đã set:

```http
POST /api/demo/checkin
Authorization: Bearer <token>
X-Demo-Secret: <DEMO_SECRET>
Content-Type: application/json

{ "locationId": "11111111-1111-1111-1111-111111111111" }
```

User phải đã `POST /api/quests/{questId}/start` trước. Chi tiết FE: [`FE_API_HANDOFF.md`](./FE_API_HANDOFF.md) Week 4.

## 8. Smoke test sau deploy

```powershell
.\docs\scripts\smoke-production.ps1 -BaseUrl https://<be-host>
```

(Gồm golden path + week-2 screens.) Hoặc từng script riêng. Chi tiết CP3: [week-3/05-DEPLOY-PRODUCTION.md](./week-3/05-DEPLOY-PRODUCTION.md).

## 9. Checklist trước demo

- [ ] `/api/health` và `/api/health/ready` OK
- [ ] Golden path 9 bước chạy trên production URL
- [ ] Ảnh frame/creation load từ `MINIO_PUBLIC_URL`
- [ ] Chat trả lời (Gemini key hợp lệ)
- [ ] `DEMO_ENABLED=false` trên prod (bật chỉ khi cần nút cứu cánh)
- [ ] Backup video ~3 phút (FE team)
