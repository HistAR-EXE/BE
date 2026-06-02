# HistAR / TimeLens — Tài liệu backend

| File | Mô tả |
|------|--------|
| [FE_API_HANDOFF.md](./FE_API_HANDOFF.md) | Giao FE: Week 1–4 (API contract + tiến độ) |
| [DEPLOY.md](./DEPLOY.md) | Checklist deploy production tuần 4 (Railway/Render, env, health/readiness) |
| [TimeLens_SpringBoot_Starter.md](./TimeLens_SpringBoot_Starter.md) | Hướng dẫn Spring Boot ngày 1–3 |
| [database/TimeLens_DB_Schema.sql](./database/TimeLens_DB_Schema.sql) | Schema **17 bảng** + seed Củ Chi |
| [scripts/smoke-golden-path.ps1](./scripts/smoke-golden-path.ps1) | Smoke script tuần 4 cho golden path |

**Tham khảo:** `../TimeLens_Tuan1_1SE_DayDu.docx`, `../TimeLens_Tuan2_Gamification.docx`, `../TimeLens_Tuan3_ViralLoop.docx`, `../TimeLens_Tuan4_Demo.docx`

## Chạy local

```bash
cp .env.example .env
docker compose up -d   # Postgres + MinIO (bucket public-read cho ảnh)
./mvnw spring-boot:run
```

- API: http://localhost:8080  
- Health: http://localhost:8080/api/health  
- Readiness: http://localhost:8080/api/health/ready  
- MinIO console: http://localhost:9001  
- Ảnh upload: `http://localhost:9000/timelens-media/...`

## DB đã tồn tại?

```bash
docker compose down -v && docker compose up -d
```

Cần sau Week 2 (`user_secret_unlocks`) và Week 3 (`user_creations.variant`, `shared_at`).
