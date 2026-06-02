# HistAR / TimeLens — Tài liệu backend

| File | Mô tả |
|------|--------|
| [FE_API_HANDOFF.md](./FE_API_HANDOFF.md) | Giao FE: endpoint, auth, seed ID, ví dụ axios/curl |
| [TimeLens_SpringBoot_Starter.md](./TimeLens_SpringBoot_Starter.md) | Hướng dẫn Spring Boot ngày 1–3 (entities, JWT, curl) |
| [database/TimeLens_DB_Schema.sql](./database/TimeLens_DB_Schema.sql) | Schema 16 bảng + seed Củ Chi (Docker Postgres mount file này) |

**Tham khảo thêm (repo gốc):** `../TimeLens_Tuan1_1SE_DayDu.docx` — kế hoạch fullstack tuần 1.

## Chạy local nhanh

```bash
cp .env.example .env
docker compose up -d
./mvnw spring-boot:run
```

API: http://localhost:8080 — xem chi tiết trong [FE_API_HANDOFF.md](./FE_API_HANDOFF.md).
