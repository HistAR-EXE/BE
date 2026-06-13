# CP3 — Test full luồng A-Z (Online + Offline)

> Đối chiếu [TimeLens_CP3_Plan_Final.docx](../TimeLens_CP3_Plan_Final.docx) · Chạy tự động: `docs/scripts/cp3-full-flow-test.ps1`

---

## 1. Chạy test tự động (BE)

```powershell
docker compose up -d
powershell -ExecutionPolicy Bypass -File docs\scripts\run-all-seed.ps1
.\mvnw spring-boot:run

# Terminal khác — test full A-Z
powershell -ExecutionPolicy Bypass -File docs\scripts\cp3-full-flow-test.ps1

# Sau khi set GEMINI_API_KEY trong .env + restart BE:
powershell -ExecutionPolicy Bypass -File docs\scripts\cp3-full-flow-test.ps1 -RunChatCiteFull
```

**Output:** `docs/scripts/cp3-full-flow-report.txt`

| Script | Phạm vi |
|--------|---------|
| `cp3-full-flow-test.ps1` | Infra + Online + Offline + Week 2 API + smokes |
| `smoke-golden-path.ps1` | Golden path end-to-end |
| `smoke-week2-screens.ps1` | 5 màn Explore/Quests/Profile/Leaderboard/Scan |
| `verify-hotspots-scene.ps1` | Virtual Tour scene UUID |
| `chat-cite-verify.ps1` | 20 câu AI cite (cần Gemini key) |

---

## 2. Kết quả runtime vừa chạy (local)

| Nhóm | Kết quả | Ghi chú |
|------|---------|---------|
| Infra (Docker, health) | **PASS** | Postgres + MinIO + `/api/health/ready` |
| Data Củ Chi + sources DB | **PASS** | UNESCO/Ban QL trong `locations.sources` |
| **ONLINE** — Explore, slider, 360, hotspots | **PASS** | 5 photo-pairs, 3 panoramas, 2 scene UUID |
| **ONLINE** — 2 nhân vật Củ Chi | **PASS** | Chị Năm + Hướng dẫn viên |
| **ONLINE** — AI Chat | **WARN** | `GEMINI_API_KEY` trống trong `.env` |
| **OFFLINE** — Check-in, Quest, Secret | **PASS** | QR/GPS + quest progress |
| **OFFLINE** — Photo frame + share | **PASS** | Upload + record-share |
| Week 2 — Profile, badges, leaderboard | **PASS** | |
| Week 3 — Panorama URL | **PASS** | Placeholder (chưa ảnh thật Củ Chi) |
| Auth refresh token | **PASS** | In-memory (restart BE = mất session) |
| 3 smoke scripts | **PASS** | |

**BE local: sẵn sàng demo** trừ AI chat (cần key) và các hạng mục FE/team bên dưới.

---

## 3. Map CP3 Plan → Test (tổng thể → chi tiết)

### Tuần 1 — Local golden path

| Ngày | Plan CP3 | Test BE | Test FE (tay) |
|------|----------|---------|---------------|
| 1 | SQL seed + golden path | `run-all-seed.ps1`, `smoke-golden-path.ps1` | Bỏ mock, dùng `data.items` |
| 2 | Mode Online/Offline | — (không cần API mới) | ModeSelectPage + `AppModeContext` |
| 3 | **ONLINE:** Explore → 360 → Chat → Slider | `cp3-full-flow-test` mục Online | Badge "Khám phá từ xa" |
| 4 | **OFFLINE:** Check-in → Quest → Secret → Frame → Share | `cp3-full-flow-test` mục Offline | Badge "Tại di tích" |
| 5 | AI cite 20 câu | `chat-cite-verify.ps1` | Hiển thị dòng `Nguồn:` |
| 6 | Virtual Tour ≥3 scene | `verify-hotspots-scene.ps1` | Photo Sphere + hotspot `scene` |
| 7 | Mobile LAN | CORS trong `.env` | `VITE_API_URL=http://<IP-LAN>:8080` |

#### ONLINE — checklist API (Củ Chi `11111111-1111-1111-1111-111111111111`)

| Bước UI | Method | Endpoint | Đã test |
|---------|--------|----------|---------|
| Explore list | GET | `/api/locations?page=0&size=20` → `data.items` | ✅ |
| Location detail | GET | `/api/locations/{id}` | ✅ |
| Photo slider | GET | `/api/photo-pairs/by-location/{id}` | ✅ 5 pairs (fallback) |
| Photo scenes (3 era) | GET | `/api/photo-scenes/by-location/{id}` | ✅ CP3 upgrade |
| Cổ vật catalog | GET | `/api/artifacts?locationId={id}` | ✅ 17 items |
| Map POI | GET | `/api/discovery-points/by-location/{id}` | ✅ 12 điểm |
| Tiến trình map | GET | `/api/me/discoveries/summary?locationId=` (JWT) | ✅ |
| Unlock discovery | POST | `/api/me/discoveries` body `{ unlockKey }` (JWT) | ✅ |
| Pokédex user | GET | `/api/me/artifacts?locationId=` (JWT) | ✅ |
| Tour 360 list | GET | `/api/panoramas/by-location/{id}` | ✅ 3 scenes |
| Hotspots | GET | `/api/hotspots/by-panorama/{panoramaId}` | ✅ scene UUID |
| Chọn nhân vật | GET | `/api/characters/by-location/{id}` | ✅ 2 chars |
| Chat | POST | `/api/chat` (JWT) | ⚠️ cần `GEMINI_API_KEY` |

#### OFFLINE — checklist API

| Bước UI | Method | Endpoint | Đã test |
|---------|--------|----------|---------|
| Check-in QR/GPS | POST | `/api/checkins` | ✅ |
| Quest list | GET | `/api/quests?locationId=` | ✅ |
| My quests | GET | `/api/me/quests` | ✅ |
| Start quest | POST | `/api/quests/{id}/start` | ✅ |
| Secret story | GET | `/api/locations/{id}/secret-story` (JWT) | ✅ |
| Photo frames | GET | `/api/photo-frames` | ✅ |
| Upload creation | POST | `/api/user-creations` multipart | ✅ |
| Share | POST | `/api/user-creations/{id}/record-share` | ✅ |
| Share caption | GET | `/api/share/prefill` | ✅ |

### Tuần 2 — Polish + Focus Group

| Hạng mục | BE test | FE/team |
|----------|---------|---------|
| 5 màn API | `smoke-week2-screens.ps1` ✅ | Polish UI Stitch |
| Loading/error/empty | API trả `[]` OK | Skeleton + toast |
| Focus Group | BE local ổn | 10–16 users, checklist trong guide này |

### Tuần 3 — 360 thật + deploy

| Hạng mục | BE test | Bạn/FE |
|----------|---------|--------|
| 3 panorama placeholder | ✅ SQL week-3 | Thay ảnh equirectangular thật |
| Upload API | Code có, chưa smoke upload | `POST /api/panoramas` khi cần |
| Deploy Railway | `smoke-production.ps1` | Set env + CORS prod |
| Survey 10+ | — | Link Vercel prod |

---

## 4. Test FE thủ công — 2 mode (bắt buộc trước pitch)

Plan CP3 yêu cầu **golden path 2 mode**. BE không có flag Online/Offline — FE lưu `localStorage` và điều hướng.

### Mode ONLINE (ở nhà)

1. Login → chọn **Khám phá từ xa**
2. Explore → mở **Địa đạo Củ Chi**
3. **Time Portal** — slider ảnh xưa/nay (5 pairs)
4. **Tour 360** — 3 scene, click hotspot chuyển scene (UUID)
5. **Chat** — hỏi "Bếp Hoàng Cầm là gì?" → cuối câu có `Nguồn:`
6. Hỏi ngoài phạm vi ("vé máy bay") → từ chối chuẩn

### Mode OFFLINE (tại di tích)

1. Đổi mode → **Đang tại di tích**
2. **Scan** — QR `timelens:location:11111111-...` hoặc GPS gần tọa độ Củ Chi
3. **Quests** — start quest Củ Chi → xem progress
4. **Secret story** — mở sau check-in (JWT)
5. **Photo frame** — chụp/chọn ảnh → upload → share
6. **Leaderboard** — điểm tăng sau check-in/share

### Mobile LAN (Ngày 7)

```env
# BE .env
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://192.168.x.x:5173

# FE .env
VITE_API_URL=http://192.168.x.x:8080
```

Lặp lại cả 2 mode trên điện thoại cùng WiFi.

---

## 5. Việc còn làm trước pitch

| Ưu tiên | Việc | Ai |
|---------|------|-----|
| **P0** | Set `GEMINI_API_KEY` + chạy `chat-cite-verify.ps1` | BE |
| **P0** | FE Mode Online/Offline + test 2 mode mobile | FE |
| **P0** | UI polish 5 màn | FE |
| **P1** | Chuyến Củ Chi → ảnh 360 thật → update SQL/MinIO | Bạn |
| **P1** | Deploy Railway + `smoke-production.ps1` | Bạn |
| **P1** | Focus Group + Survey 20 users | Team |
| **P2** | Refresh token persist DB | BE post-CP3 |

---

## 6. UUID demo (copy nhanh)

```
Location Củ Chi:  11111111-1111-1111-1111-111111111111
Panorama chính:   22222222-2222-2222-2222-222222222222
Panorama bếp:     22222222-2222-2222-2222-222222222221
Panorama họp:     22222222-2222-2222-2222-222222222223
Quest Củ Chi:     33333333-3333-3333-3333-333333333333
QR check-in:      timelens:location:11111111-1111-1111-1111-111111111111
```

---

*Cập nhật sau lần chạy `cp3-full-flow-test.ps1` — xem report TXT để biết PASS/FAIL mới nhất.*
