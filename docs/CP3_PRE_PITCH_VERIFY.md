# CP3 — Kết quả verify trước pitch

> Chạy lại khi Docker + BE up. Script: `docs/scripts/`.  
> **Windows:** dùng `powershell -ExecutionPolicy Bypass -File ...` (không cần `pwsh`).

---

## Checklist nhanh

| # | Hạng mục | Static review | Runtime (cần Docker+BE) |
|---|----------|---------------|-------------------------|
| 1 | `run-all-seed.ps1` | Script có 6 file SQL | `powershell -ExecutionPolicy Bypass -File docs\scripts\run-all-seed.ps1` |
| 2 | `smoke-golden-path.ps1` | Script OK | Chạy sau seed |
| 3 | `smoke-week2-screens.ps1` | Script OK | Chạy sau seed |
| 4 | Chat 20 câu cite | Prompt OK trong code | `powershell -ExecutionPolicy Bypass -File docs\scripts\chat-cite-verify.ps1` |
| 5 | `locations.sources` Củ Chi | **Nguồn thật trong SQL** | Query DB (mục 1) |
| 6 | Hotspot scene UUID | **Đã sửa SQL week-3** | `powershell -ExecutionPolicy Bypass -File docs\scripts\verify-hotspots-scene.ps1` |
| 7 | 2 nhân vật Củ Chi | **Seed schema = 2** | `GET .../characters/by-location/1111...` |
| 8 | CORS LAN | `.env.example` hướng dẫn | Thêm IP vào `CORS_ALLOWED_ORIGINS` |

---

## 1. AI cite nguồn

### SQL `locations.sources` (Củ Chi) — KHÔNG placeholder

File `week-1/2026-week1_location_sources.sql`:

```
Khu di tích lịch sử Địa đạo Củ Chi; Ban Quản lý Di tích Củ Chi; Tài liệu UNESCO về Di sản Thế giới
```

**Điều kiện:** phải chạy `run-all-seed.ps1` (file 5). Nếu không → fallback prompt: `Khu di tích lịch sử {tên}`.

### Code prompt

`ChatServiceImpl` bắt buộc dòng `Nguồn:` + câu từ chối chuẩn khi ngoài phạm vi.

### Test 20 câu (bắt buộc trước pitch)

```powershell
# Cần GEMINI_API_KEY
powershell -ExecutionPolicy Bypass -File docs\scripts\chat-cite-verify.ps1
# Output: docs/scripts/chat-cite-verify-results.txt
```

**Lưu file kết quả** làm bằng chứng Q&A — không chỉ tin prompt.

---

## 2. Hotspot scene-link

### Vấn đề đã phát hiện & sửa

Seed gốc `TimeLens_DB_Schema.sql` có hotspot `type=scene` nhưng `content_ref='meeting-room'` (string) — **FE Virtual Tour cần UUID**.

`week-3/2026-week3_update_panoramas_cu_chi.sql` đã cập nhật:

- 3 panorama UUID cố định
- Sửa `meeting-room` → UUID phòng họp
- Thêm link cổng vào → bếp → phòng họp (+ link quay lại)

### Verify runtime

```powershell
powershell -ExecutionPolicy Bypass -File docs\scripts\verify-hotspots-scene.ps1
```

---

## 3. Refresh token in-memory

`AuthServiceImpl` dùng `ConcurrentHashMap` — **restart BE = mất session**.

**Vận hành demo:** Không restart giữa buổi. Login `demo@histar.vn` trước pitch.

---

## 4. Nhân vật Củ Chi

Schema seed **đúng 2** cho `11111111-1111-1111-1111-111111111111`:

1. Chị Năm — Nữ du kích Củ Chi  
2. Hướng dẫn viên lịch sử  

**Pitch:** "Demo 2 nhân vật, kiến trúc mở rộng tới 6" — không nói 6 khi demo 2.

---

## 5. MinIO + CORS

| Môi trường | Yêu cầu |
|------------|---------|
| Local LAN | `CORS_ALLOWED_ORIGINS=http://localhost:5173,http://<IP-LAN>:5173` |
| Production | Bucket `public-read` (`minio-init.sh` đã set) + `MINIO_PUBLIC_URL` HTTPS |

---

## 6. Upload panorama API

**Đã code thật:** `POST /api/panoramas`, `PUT /api/panoramas/{id}/image` (JWT).

**Khuyến nghị Tuần 3:** Ưu tiên **SQL + placehold/ảnh thật** cho 3 scene — đơn giản hơn API khi chuẩn bị pitch. API dùng khi cần upload sau này.

---

## Lệnh verify full (copy-paste)

```powershell
docker compose up -d
powershell -ExecutionPolicy Bypass -File docs\scripts\run-all-seed.ps1
.\mvnw spring-boot:run
# terminal khác — một lệnh test A-Z (Online + Offline + smokes):
powershell -ExecutionPolicy Bypass -File docs\scripts\cp3-full-flow-test.ps1
# hoặc từng script:
powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-golden-path.ps1
powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-week2-screens.ps1
powershell -ExecutionPolicy Bypass -File docs\scripts\verify-hotspots-scene.ps1
powershell -ExecutionPolicy Bypass -File docs\scripts\chat-cite-verify.ps1
```

**Hướng dẫn chi tiết 2 mode FE:** [CP3_FULL_FLOW_TEST_GUIDE.md](./CP3_FULL_FLOW_TEST_GUIDE.md)
