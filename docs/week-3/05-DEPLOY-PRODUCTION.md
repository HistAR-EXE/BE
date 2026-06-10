# Deploy BE production — Railway / Render (Ngày 17)

> Mở rộng từ [DEPLOY.md](../DEPLOY.md) + Prompt #5 CP3.  
> **Không hardcode secret** — chỉ env vars trên hosting dashboard.

---

## 1. Kiểm tra artifact sẵn có

| File | OK |
|------|-----|
| [Dockerfile](../../Dockerfile) | Multi-stage build JAR |
| [railway.toml](../../railway.toml) | Health check `/api/health/ready` |
| [application-prod.yml](../../src/main/resources/application-prod.yml) | `ddl-auto: none` |
| Smoke | `docs/scripts/smoke-production.ps1` |
| Seed prod | `docs/scripts/run-all-seed.ps1` (trên DB managed) |

Build local test:

```powershell
docker build -t timelens-be .
docker run -p 8080:8080 --env-file .env -e SPRING_PROFILES_ACTIVE=prod timelens-be
```

---

## 2. Postgres production

### Tạo database

- Railway: Add PostgreSQL plugin → copy `DATABASE_URL` hoặc tách host/user/pass
- Render / Neon / Supabase: tương tự

### Chạy SQL (một lần, theo thứ tự)

```text
1. docs/database/TimeLens_DB_Schema.sql
2. docs/database/2026-06-02_fe_compat_migration.sql
3. docs/database/2026-06-02_fe_compat_indexes_seed.sql
4. docs/database/2026-06-02_fe_compat_data_topup.sql
5. docs/week-1/2026-week1_location_sources.sql
6. docs/week-3/2026-week3_update_panoramas_cu_chi.sql  (sau khi có URL ảnh)
```

Dùng client (DBeaver, `psql`, Railway Query) — **trước** khi start app lần đầu.

---

## 3. Object storage (MinIO / S3)

- Bucket: `timelens-media`
- Policy: **public-read** cho path ảnh frame + panorama
- Upload ảnh 360 theo [04-HUONG-DAN-360-UPLOAD.md](./04-HUONG-DAN-360-UPLOAD.md)

---

## 4. Railway deploy (gợi ý)

1. New Project → Deploy from GitHub repo `HistAR/BE`
2. Service type: **Dockerfile**
3. Add env variables:

| Biến | Ví dụ / ghi chú |
|------|-----------------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `PORT` | `8080` (Railway inject có thể khác — app đọc `${PORT}`) |
| `DB_URL` | `jdbc:postgresql://host:5432/railway` |
| `DB_USER` | từ provider |
| `DB_PASSWORD` | từ provider |
| `JWT_SECRET` | chuỗi random ≥ 32 ký tự |
| `GEMINI_API_KEY` | key production |
| `CORS_ALLOWED_ORIGINS` | `https://timelens.vercel.app` (domain FE thật) |
| `MINIO_ENDPOINT` | `https://s3...` |
| `MINIO_ACCESS_KEY` | |
| `MINIO_SECRET_KEY` | |
| `MINIO_BUCKET_NAME` | `timelens-media` |
| `MINIO_PUBLIC_URL` | HTTPS URL browser load ảnh |
| `DEMO_ENABLED` | `false` |
| `DEMO_SECRET` | (để trống trên prod thật) |

4. Health check path: `/api/health/ready`
5. Deploy → lấy public URL `https://xxx.up.railway.app`

### Render

Tương tự: Web Service + Docker, env giống bảng trên.

---

## 5. Verify BE

```powershell
curl https://<be-host>/api/health/ready
# {"status":"UP","database":"UP"}

powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-production.ps1 -BaseUrl https://<be-host>
```

---

## 6. FE Vercel

1. Project Settings → Environment Variables
2. `VITE_API_URL` = `https://<be-host>` (không slash cuối)
3. Redeploy production branch
4. Mở app Vercel → F12 Network → login không lỗi CORS

### Test CORS thủ công

```powershell
curl -I -X OPTIONS "https://<be-host>/api/auth/login" `
  -H "Origin: https://<vercel-app>" `
  -H "Access-Control-Request-Method: POST"
```

Phải có `Access-Control-Allow-Origin` khớp origin FE.

---

## 7. Tài khoản demo production

```http
POST https://<be-host>/api/auth/register
Content-Type: application/json

{
  "email": "demo@histar.vn",
  "password": "Demo@2026",
  "displayName": "Demo Presenter"
}
```

Login sẵn trên máy pitch.

---

## 8. Checklist trước pitch (Ngày 20–21)

- [ ] `/api/health` + `/api/health/ready` → 200
- [ ] `smoke-production.ps1` pass
- [ ] FE Vercel login + locations + 360 load
- [ ] Chat 1 câu (Gemini key OK)
- [ ] Ảnh frame/upload từ `MINIO_PUBLIC_URL`
- [ ] `DEMO_ENABLED=false` (chỉ bật rehearse)
- [ ] Video backup 3 phút sẵn sàng

---

## 9. Rollback

- Railway: redeploy commit trước
- DB: không `down -v` trên prod — backup trước khi chạy SQL mới
- FE: revert `VITE_API_URL` về bản cũ trên Vercel

---

*Deploy xong trước rehearsal ít nhất 1 ngày — buffer Ngày 18–20.*
