# HistAR / TimeLens — Tài liệu backend

| File | Mô tả |
|------|--------|
| [FE_API_HANDOFF.md](./FE_API_HANDOFF.md) | Giao FE: Week 1–3 (endpoint, auth, QR, viral loop) |
| [TimeLens_SpringBoot_Starter.md](./TimeLens_SpringBoot_Starter.md) | Hướng dẫn Spring Boot ngày 1–3 |
| [database/TimeLens_DB_Schema.sql](./database/TimeLens_DB_Schema.sql) | Schema **17 bảng** + seed Củ Chi |

**Tham khảo:** `../TimeLens_Tuan1_1SE_DayDu.docx`, `../TimeLens_Tuan2_Gamification.docx`, `../TimeLens_Tuan3_ViralLoop.docx`

## Chạy local

```bash
cp .env.example .env
docker compose up -d   # Postgres + MinIO (bucket public-read cho ảnh)
./mvnw spring-boot:run
```

- API: http://localhost:8080  
- MinIO console: http://localhost:9001  
- Ảnh upload: `http://localhost:9000/timelens-media/...`

## DB đã tồn tại?

```bash
docker compose down -v && docker compose up -d
```

Cần sau Week 2 (`user_secret_unlocks`) và Week 3 (`user_creations.variant`, `shared_at`).
