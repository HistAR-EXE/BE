# CP3 Tuần 1 — Thao tác bạn cần tự làm

> File này liệt kê việc **không thể hoàn thành chỉ bằng code trong repo BE** — bạn (và team FE) cần chạy tay trên máy local.
>
> **Đối chiếu:** [01-BE-DA-LAM.md](./01-BE-DA-LAM.md) (BE đã code) · [02-FE-CAN-LAM.md](./02-FE-CAN-LAM.md) (FE cần code)

---

## Tổng quan trạng thái Ngày 1 → 7

| Ngày | Việc theo plan CP3 | Trong repo BE | Bạn cần làm |
|------|-------------------|---------------|-------------|
| **1** | SQL seed + golden path | Smoke script đã sửa `data.items` | Chạy Docker, 5 file SQL, start BE, chạy smoke |
| **2** | Màn chọn Online/Offline | Không có (thuần FE) | Làm trên repo **FE** theo [02-FE-CAN-LAM.md](./02-FE-CAN-LAM.md) |
| **3** | ONLINE flow UI | API đã sẵn | FE: Explore → 360 → Chat → Photo slider |
| **4** | OFFLINE flow UI | API đã sẵn | FE: Scan → Quest → Secret → Frame → Share |
| **5** | AI cite nguồn | **Đã code** `ChatServiceImpl` + SQL `sources` | Chạy SQL file 5 + test chat tay với `GEMINI_API_KEY` |
| **6** | Virtual Tour đa scene | API panoramas/hotspots đủ | FE: cài plugin, ≥3 scene, Street View tạm |
| **7** | Test mobile LAN + review | CORS cấu hình qua env | Đổi IP LAN, test điện thoại, họp review nhóm |

**Kết luận:** Phần **BE Tuần 1 trong repo này đã xong** (Ngày 1 smoke script + Ngày 5 cite nguồn). Tuần 1 **chưa hoàn thành end-to-end** cho đến khi bạn chạy SQL, smoke pass, FE làm Ngày 2–7, và test chat thật.

---

## A. Setup môi trường (làm trước Ngày 1)

### A.1 File `.env` BE

```powershell
cd D:\FPT\SU26\EXE101\HistAR\BE
copy .env.example .env
```

Chỉnh tối thiểu trong `.env`:

| Biến | Ghi chú |
|------|---------|
| `GEMINI_API_KEY` | **Bắt buộc** để test chat Ngày 5 |
| `JWT_SECRET` | ≥ 32 ký tự |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` (thêm IP LAN ở Ngày 7) |

### A.2 Docker (Postgres + MinIO)

```powershell
docker compose up -d
docker compose ps
```

Đảm bảo Postgres `5432` và MinIO `9000` đang chạy.

---

## B. Chạy SQL seed (Ngày 1 + Ngày 5)

```powershell
docker compose up -d
powershell -ExecutionPolicy Bypass -File docs\scripts\run-all-seed.ps1
```

Chạy **6 file** SQL (plan gốc 4 + sources + panorama hotspots). Bảng chi tiết: [CP3_AUDIT_STATUS.md](../CP3_AUDIT_STATUS.md).

**Nếu DB cũ lỗi / thiếu seed:**

```powershell
docker compose down -v
docker compose up -d
powershell -ExecutionPolicy Bypass -File docs\scripts\run-all-seed.ps1
```

**Kiểm tra nhanh sau SQL:**

```powershell
docker exec -i <container> psql -U timelens -d timelens -c "SELECT id, name, left(sources, 40) FROM locations WHERE id = '11111111-1111-1111-1111-111111111111';"
```

Phải thấy cột `sources` có nội dung Củ Chi.

---

## C. Chạy BE + smoke golden path (Ngày 1)

### C.1 Start backend

```powershell
cd D:\FPT\SU26\EXE101\HistAR\BE
.\mvnw spring-boot:run
```

Đợi log không còn lỗi `ddl-auto: validate` (schema khớp DB).

### C.2 Health check

```powershell
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health/ready
```

`ready` phải `200` và `"database": "UP"`.

### C.3 Smoke script

```powershell
powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-golden-path.ps1
```

Kỳ vọng dòng cuối: `Smoke golden path OK. userId=...`

Nếu fail:
- `No location found` → chưa chạy đủ SQL hoặc BE chưa start
- `VALIDATION` / schema → chạy file migration hoặc `docker compose down -v`

---

## D. Test chat cite nguồn tay (Ngày 5)

Cần `GEMINI_API_KEY` hợp lệ trong `.env` và **restart BE** sau khi sửa env.

```powershell
# 1. Register
$body = '{"email":"chat.test@histar.vn","password":"Test1234!","displayName":"Chat Test"}'
$reg = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/register" -ContentType "application/json" -Body $body
$token = $reg.data.accessToken

# 2. Lấy characterId Củ Chi
$locId = "11111111-1111-1111-1111-111111111111"
$chars = Invoke-RestMethod -Uri "http://localhost:8080/api/characters/by-location/$locId"
$charId = $chars.data[0].id

# 3. Hỏi chat
$chatBody = @{ characterId = $charId; message = "Chị Năm ơi, cuộc sống trong địa đạo thế nào?"; conversationId = $null } | ConvertTo-Json
$headers = @{ Authorization = "Bearer $token" }
$chat = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/chat" -Headers $headers -ContentType "application/json" -Body $chatBody
$chat.data.reply
```

**Pass khi:**
- Reply có dòng bắt đầu `Nguồn:` ở cuối
- Câu hỏi ngoài phạm vi (vd: "Giá vé máy bay đi Củ Chi?") → từ chối, không bịa số

Ghi lại 2–3 câu trả lời mẫu nếu cần cho demo/pitch.

---

## E. Repo FE — Ngày 2 → 6 (bạn / team FE)

Làm trên repo **HistAR/FE** (không nằm trong repo BE này):

| Ngày | Checklist ngắn |
|------|----------------|
| 2 | `ModeSelectPage`, `AppModeContext`, badge TopNav |
| 3 | ONLINE: locations `data.items`, tour, chat JWT |
| 4 | OFFLINE: check-in, quest paginated, secret, share |
| 5 | Hiển thị `reply` có dòng Nguồn |
| 6 | `@photo-sphere-viewer/virtual-tour-plugin`, ≥3 scene |

Chi tiết: [02-FE-CAN-LAM.md](./02-FE-CAN-LAM.md)

**FE `.env`:**

```
VITE_API_URL=http://localhost:8080
```

```powershell
npm run dev
```

---

## F. Test mobile qua LAN (Ngày 7)

### F.1 Lấy IP máy chạy BE

```powershell
ipconfig
# Ví dụ: 192.168.1.42
```

### F.2 CORS BE

Trong `.env` BE:

```
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://192.168.1.42:5173
```

Restart `./mvnw spring-boot:run`.

### F.3 FE trên điện thoại

- Máy tính và điện thoại **cùng WiFi**
- FE chạy bind LAN: `npm run dev -- --host 0.0.0.0`
- Trên điện thoại mở `http://192.168.1.42:5173`
- Nếu FE gọi API trực tiếp từ browser phone: `VITE_API_URL=http://192.168.1.42:8080`

### F.4 Internal review

- [ ] Test cả mode ONLINE và OFFLINE
- [ ] Ghi bug vào issue/sheet nhóm
- [ ] Xác nhận golden path không mock

---

## G. Checklist “Tuần 1 xong” (tick khi pass)

### BE (repo này)

- [x] Code cite nguồn (`ChatServiceImpl`)
- [x] Entity + SQL `locations.sources`
- [x] Smoke script `data.items`
- [ ] **Bạn:** 5 file SQL đã chạy trên DB local
- [ ] **Bạn:** `spring-boot:run` + smoke pass
- [ ] **Bạn:** Chat test có `Nguồn:` (cần Gemini key)

### FE (repo khác)

- [ ] Bỏ mock, dùng `data.items`
- [ ] Mode Online/Offline
- [ ] Virtual Tour ≥3 scene
- [ ] Test mobile LAN

### Team

- [ ] Internal review Ngày 7
- [ ] Bug list từ review

---

## H. Lỗi thường gặp

| Triệu chứng | Cách xử lý |
|-------------|------------|
| BE không start: `validate` schema | Chạy đủ migration SQL; hoặc reset volume Docker |
| `column sources does not exist` | Chạy `2026-week1_location_sources.sql` |
| Chat 422 `BUSINESS_RULE` | Thiếu/sai `GEMINI_API_KEY` |
| Chat rate limit | Đợi UTC ngày mới hoặc tăng `GEMINI_DAILY_MESSAGE_LIMIT` |
| FE CORS | Thêm origin FE vào `CORS_ALLOWED_ORIGINS`, restart BE |
| Smoke `items` null | Locations trống → chạy lại seed SQL |
| Refresh token mất sau restart BE | Bình thường (in-memory); đăng nhập lại |

---

## I. Thứ tự làm gợi ý (1 buổi)

1. `docker compose up -d`
2. Chạy 5 file SQL
3. `.\mvnw spring-boot:run`
4. `powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-golden-path.ps1`
5. Test chat (mục D)
6. Chuyển sang FE làm [02-FE-CAN-LAM.md](./02-FE-CAN-LAM.md)
7. Ngày 7: LAN + review

---

*Cập nhật sau audit CP3 Tuần 1 — phần code BE trong repo đã hoàn tất; các mục tick trống ở mục G cần bạn chạy tay.*
