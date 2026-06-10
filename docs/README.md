# HistAR / TimeLens — Tài liệu backend

| File | Mô tả |
|------|--------|
| [CP3_AUDIT_STATUS.md](./CP3_AUDIT_STATUS.md) | **Audit CP3** — trạng thái Tuần 1/2/3 vs plan |
| [CP3_FULL_FLOW_TEST_GUIDE.md](./CP3_FULL_FLOW_TEST_GUIDE.md) | **Test A-Z** Online + Offline theo plan CP3 |
| [CP3_PRE_PITCH_VERIFY.md](./CP3_PRE_PITCH_VERIFY.md) | Checklist verify trước pitch |
| [week-1/01-BE-DA-LAM.md](./week-1/01-BE-DA-LAM.md) | **CP3 Tuần 1** — BE đã làm + contract |
| [week-1/02-FE-CAN-LAM.md](./week-1/02-FE-CAN-LAM.md) | **CP3 Tuần 1** — checklist FE tích hợp |
| [week-1/03-THAO-TAC-BAN-TU-LAM.md](./week-1/03-THAO-TAC-BAN-TU-LAM.md) | **CP3 Tuần 1** — thao tác chạy tay (SQL, smoke, chat, LAN) |
| [week-2/01-BE-DA-LAM.md](./week-2/01-BE-DA-LAM.md) | **CP3 Tuần 2** — API 5 màn + smoke Focus Group |
| [week-2/02-FE-CAN-LAM.md](./week-2/02-FE-CAN-LAM.md) | **CP3 Tuần 2** — polish UI + Focus Group (FE) |
| [week-2/03-THAO-TAC-BAN-TU-LAM.md](./week-2/03-THAO-TAC-BAN-TU-LAM.md) | **CP3 Tuần 2** — setup demo, recruit, triage bug |
| [week-2/04-FOCUS-GROUP-SCRIPT.md](./week-2/04-FOCUS-GROUP-SCRIPT.md) | Script Focus Group 90 phút |
| [week-3/01-BE-DA-LAM.md](./week-3/01-BE-DA-LAM.md) | **CP3 Tuần 3** — 360 thật + deploy prod |
| [week-3/03-THAO-TAC-BAN-TU-LAM.md](./week-3/03-THAO-TAC-BAN-TU-LAM.md) | **CP3 Tuần 3** — chuyến Củ Chi, survey, pitch |
| [week-3/05-DEPLOY-PRODUCTION.md](./week-3/05-DEPLOY-PRODUCTION.md) | Deploy Railway/Render từng bước |
| [BE_PROJECT_STATUS_AND_FE_GUIDE.md](./BE_PROJECT_STATUS_AND_FE_GUIDE.md) | Master handoff BE ↔ FE (toàn dự án) |
| [FE_API_HANDOFF.md](./FE_API_HANDOFF.md) | Giao FE: Week 1–4 (API contract + tiến độ) |
| [DEPLOY.md](./DEPLOY.md) | Checklist deploy production tuần 4 (Railway/Render, env, health/readiness) |
| [TimeLens_SpringBoot_Starter.md](./TimeLens_SpringBoot_Starter.md) | Hướng dẫn Spring Boot ngày 1–3 |
| [database/TimeLens_DB_Schema.sql](./database/TimeLens_DB_Schema.sql) | Schema **17 bảng** + seed Củ Chi |
| [scripts/cp3-full-flow-test.ps1](./scripts/cp3-full-flow-test.ps1) | Test full CP3 (Online + Offline + smokes) |
| [scripts/smoke-golden-path.ps1](./scripts/smoke-golden-path.ps1) | Smoke golden path |
| [scripts/run-all-seed.ps1](./scripts/run-all-seed.ps1) | Chạy tất cả SQL seed vào Postgres Docker |
| [scripts/smoke-production.ps1](./scripts/smoke-production.ps1) | Smoke full trên URL production |

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
