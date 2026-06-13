# Tài liệu Backend

| File | Mô tả |
|------|--------|
| [BE_PROJECT_STATUS_AND_FE_GUIDE.md](./BE_PROJECT_STATUS_AND_FE_GUIDE.md) | Trạng thái BE + hướng dẫn tích hợp FE |
| [FE_API_HANDOFF.md](./FE_API_HANDOFF.md) | Contract API chi tiết |
| [DEPLOY.md](./DEPLOY.md) | Deploy production (Railway/Render) |
| [CP3_FULL_FLOW_TEST_GUIDE.md](./CP3_FULL_FLOW_TEST_GUIDE.md) | Test flow Online + Offline |
| [HUONG_DAN_360_UPLOAD.md](./HUONG_DAN_360_UPLOAD.md) | Chụp & upload panorama 360° |
| [database/](./database/) | Schema + migration SQL |

## Chạy local

**Khuyến nghị:** dùng [`docker-compose.yml`](../../docker-compose.yml) ở thư mục gốc HistAR (Postgres + MinIO + BE + AI + FE).

Chỉ BE:

```powershell
cp .env.example .env
cd ..
docker compose up -d postgres minio minio-init
cd BE
./mvnw spring-boot:run
```

- API: http://localhost:8080/api/health
- MinIO: http://localhost:9001

Seed DB: `.\docs\scripts\run-all-seed.ps1`
