# CP3 — Audit trạng thái Tuần 1 / 2 / 3

> Đối chiếu [TimeLens_CP3_Plan_Final.docx](../TimeLens_CP3_Plan_Final.docx) với **code + docs** trong repo BE (cập nhật sau review).
>
> **Chú thích:** ✅ xong trong repo | 🔧 bạn/FE chạy tay | ⏳ chưa làm (ngoài BE) | 📄 chỉ docs

---

## Tổng quan 3 tuần

| Tuần | Mục tiêu CP3 | BE code | Docs | Còn thiếu (thực tế) |
|------|--------------|---------|------|---------------------|
| **1** | Online/Offline + AI cite + local golden path | ✅ cite, smoke, seed SQL | ✅ week-1/ | **FE:** AppMode, Virtual Tour |
| **2** | Polish UI + Focus Group | ✅ smoke 5 màn, API sẵn | ✅ week-2/ | **FE:** polish + **Team:** FG 10–16 users |
| **3** | 360 thật + deploy + demo/pitch | ✅ upload panorama API, smoke prod | ✅ week-3/ | **Bạn:** chụp Củ Chi, Railway, survey |

---

## Tuần 1 — Chi tiết

| Ngày | Plan CP3 | BE | FE | Ghi chú |
|------|----------|----|----|---------|
| 1 | 4 file SQL + golden path | ✅ `run-all-seed.ps1` (6 file), `smoke-golden-path.ps1` | 🔧 bỏ mock | Plan gốc 4 SQL → thêm week-1 sources + week-3 panorama seed |
| 2–4 | Mode Online/Offline | ⏳ không cần API | ⏳ Prompt #1 | Thuần FE |
| 3–4 | 2 flow UI + badge | — | ⏳ | |
| 5 | AI cite nguồn | ✅ `ChatServiceImpl` + `locations.sources` | 🔧 hiển thị reply | |
| 6 | Virtual Tour ≥3 scene | ✅ API panoramas/hotspots; SQL scene links (week-3) | ⏳ Prompt #3 | Street View tạm OK |
| 7 | Test mobile LAN | 🔧 CORS env | 🔧 `--host 0.0.0.0` | [week-1/03](./week-1/03-THAO-TAC-BAN-TU-LAM.md) |

### Code BE Tuần 1 (đã có)

- `ChatServiceImpl` — cite + guardrail
- `Location.sources` + migration
- `smoke-golden-path.ps1` — `data.items`
- `Location.rating` / `isArAvailable` map từ DB (bổ sung sau audit)

### Checklist plan §7 liên quan Tuần 1

| Hạng mục | Trạng thái |
|----------|------------|
| AI Chat cite nguồn | ✅ BE |
| Phân Online/Offline | ⏳ FE |
| Virtual Tour | ⏳ FE (+ ảnh thật Tuần 3) |
| Golden path 2 mode | ⏳ FE + test |

---

## Tuần 2 — Chi tiết

| Ngày | Plan CP3 | BE | FE / Team |
|------|----------|----|-----------|
| 8–9 | Polish 5 màn | ✅ API ổn định | ⏳ Prompt #4 |
| 10 | Loading/error/empty | ✅ empty arrays OK (`badges[]`, `entries[]`) | ⏳ UI states |
| 11 | Chuẩn bị FG | ✅ `smoke-week2-screens.ps1` | 🔧 recruit + setup |
| 12–13 | Focus Group | 🔧 BE chạy local | 🔧 2 buổi |
| 14 | Fix bug + BMC | 🔧 theo feedback | Team |

### Code BE Tuần 2

- `docs/scripts/smoke-week2-screens.ps1` — Explore, Quests, Profile, Leaderboard, Scan
- Không thêm feature (đúng plan)

---

## Tuần 3 — Chi tiết

| Ngày | Plan CP3 | BE | Bạn / FE |
|------|----------|----|----------|
| 15 | Chụp 360 Củ Chi | — | 🔧 field trip |
| 16 | Upload + DB panorama | ✅ `POST/PUT /api/panoramas` + SQL hotspots | 🔧 upload ảnh |
| 17 | Deploy production | ✅ Dockerfile, `railway.toml`, `smoke-production.ps1` | 🔧 Railway env |
| 18 | Survey 10+ | — | 🔧 link Vercel prod |
| 19–21 | Video + pitch | 🔧 `demo@histar.vn` trên prod | Team |

### Code BE Tuần 3

- `PanoramaAppServiceImpl` + `PanoramaController` upload/replace
- `SecurityConfig` — GET panorama public, POST/PUT JWT
- `2026-week3_update_panoramas_cu_chi.sql` — multi-scene + hotspot `scene`
- `smoke-production.ps1`
- `railway.toml` — health check ready

---

## Gap còn lại (không block CP3 nếu làm đủ FE + deploy)

| Gap | Ưu tiên | Ghi chú |
|-----|---------|---------|
| Refresh token DB | P1 post-CP3 | Bảng có, code vẫn in-memory |
| `POST /api/hotspots` | P2 | Scene links có thể dùng SQL week-3 |
| Leaderboard pagination | P2 | `entries[]` đủ demo |
| Media URL fallback | P2 | Placeholder seed |
| Online/Offline mode | **P0 FE** | Plan Tuần 1 |
| UI polish + empty states | **P0 FE** | Plan Tuần 2 |
| Deploy Railway + ảnh thật | **P0 bạn** | Plan Tuần 3 |

---

## Script & SQL — thứ tự chuẩn

```powershell
docker compose up -d
powershell -ExecutionPolicy Bypass -File docs\scripts\run-all-seed.ps1
.\mvnw spring-boot:run
powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-golden-path.ps1
powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-week2-screens.ps1
# Sau deploy:
powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-production.ps1 -BaseUrl https://<be-host>
```

| # | File SQL |
|---|----------|
| 1 | `database/TimeLens_DB_Schema.sql` |
| 2 | `database/2026-06-02_fe_compat_migration.sql` |
| 3 | `database/2026-06-02_fe_compat_indexes_seed.sql` |
| 4 | `database/2026-06-02_fe_compat_data_topup.sql` |
| 5 | `week-1/2026-week1_location_sources.sql` |
| 6 | `week-3/2026-week3_update_panoramas_cu_chi.sql` |

---

## Tài liệu theo tuần

| Tuần | BE đã làm | FE cần làm | Thao tác tay |
|------|-----------|------------|--------------|
| 1 | [week-1/01](./week-1/01-BE-DA-LAM.md) | [02](./week-1/02-FE-CAN-LAM.md) | [03](./week-1/03-THAO-TAC-BAN-TU-LAM.md) |
| 2 | [week-2/01](./week-2/01-BE-DA-LAM.md) | [02](./week-2/02-FE-CAN-LAM.md) | [03](./week-2/03-THAO-TAC-BAN-TU-LAM.md) + [FG script](./week-2/04-FOCUS-GROUP-SCRIPT.md) |
| 3 | [week-3/01](./week-3/01-BE-DA-LAM.md) | [02](./week-3/02-FE-CAN-LAM.md) | [03](./week-3/03-THAO-TAC-BAN-TU-LAM.md) |
| 3 deploy | [05-DEPLOY](./week-3/05-DEPLOY-PRODUCTION.md) | — | [DEPLOY.md](./DEPLOY.md) |
| 3 ảnh 360 | [04-HUONG-DAN-360](./week-3/04-HUONG-DAN-360-UPLOAD.md) | — | |

**Master API:** [BE_PROJECT_STATUS_AND_FE_GUIDE.md](./BE_PROJECT_STATUS_AND_FE_GUIDE.md)

---

## Checklist 10 điểm (plan §7) — trạng thái hiện tại

### Prototype

- [ ] Phân Online/Offline — FE
- [ ] Virtual Tour ảnh 360 thật — FE + chuyến Củ Chi
- [x] AI cite nguồn — BE
- [ ] UI polish + loading/error/empty — FE
- [ ] BE deploy production — bạn Ngày 17
- [ ] Golden path 2 mode end-to-end — FE + test
- [ ] Demo video 3 phút — team

### Business (team)

- [ ] BMC + pricing
- [ ] Customer testing ≥ 20
- [ ] Report 5–7 trang

### Pitch

- [ ] Live demo + Q&A bank
- [ ] Backup video + demo check-in

---

**Verify trước pitch:** [CP3_PRE_PITCH_VERIFY.md](./CP3_PRE_PITCH_VERIFY.md)

*File này là nguồn audit chính khi hỏi "3 tuần đã xong chưa".*
