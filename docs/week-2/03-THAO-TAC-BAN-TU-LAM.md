# CP3 Tuần 2 — Thao tác bạn cần tự làm

> Polish UI + Focus Group chủ yếu là **FE + vận hành**. BE cần **ổn định local** trước Ngày 11.

---

## Tổng quan Ngày 8 → 14

| Ngày | Việc | Bạn / team |
|------|------|------------|
| 8–9 | Polish UI 5 màn | FE dev |
| 10 | Loading/error/empty | FE dev |
| 11 | Chuẩn bị Focus Group | **Bạn** recruit + setup máy demo |
| 12–13 | 2 buổi Focus Group | **Bạn** điều phối + ghi chép |
| 14 | Fix bug + BMC/pitch | Team |

**BE Tuần 2 trong repo:** script `smoke-week2-screens.ps1` + docs. **Không** code feature mới.

---

## A. Trước Ngày 8 (kiểm tra BE)

```powershell
cd D:\FPT\SU26\EXE101\HistAR\BE
docker compose up -d
.\mvnw spring-boot:run
```

Terminal khác:

```powershell
powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-golden-path.ps1
powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-week2-screens.ps1
```

Cả hai phải **PASSED**. Nếu fail → xem [week-1/03-THAO-TAC-BAN-TU-LAM.md](../week-1/03-THAO-TAC-BAN-TU-LAM.md).

---

## B. Ngày 11 — Setup máy demo Focus Group

### B.1 Phần cứng

- [ ] Laptop: BE + FE dev server
- [ ] 1–2 điện thoại test (cùng WiFi)
- [ ] Sạc + dự phòng mobile hotspot (wifi phòng yếu)
- [ ] Mic ghi âm (điện thoại/laptop)

### B.2 Phần mềm

| Thành phần | Lệnh / cấu hình |
|------------|-----------------|
| BE | `.\mvnw spring-boot:run` |
| FE | `npm run dev -- --host 0.0.0.0` |
| FE `.env` | `VITE_API_URL=http://<IP-LAN>:8080` |
| BE `.env` | `CORS_ALLOWED_ORIGINS=http://localhost:5173,http://<IP-LAN>:5173` |
| Chat demo | `GEMINI_API_KEY` đã set |
| GPS cứu cánh | `DEMO_ENABLED=true` + `DEMO_SECRET=<chuỗi-bí-mật>` (optional) |

Lấy IP LAN: `ipconfig` → IPv4 WiFi.

### B.3 Tài khoản demo

**Cách 1 — Đăng ký trước buổi:** tạo 1 account facilitator, lưu token.

**Cách 2 — User tự register** trong buổi (mất ~1 phút/user).

**Cách 3 — Demo check-in** khi GPS fail:

```powershell
curl -X POST http://localhost:8080/api/demo/checkin `
  -H "Authorization: Bearer <TOKEN>" `
  -H "X-Demo-Secret: <DEMO_SECRET>" `
  -H "Content-Type: application/json" `
  -d '{"locationId":"11111111-1111-1111-1111-111111111111"}'
```

### B.4 Recruit checklist

- [ ] 10–16 người (2 buổi × 5–8)
- [ ] Khớp personas CP2 (sinh viên, khách du lịch, …)
- [ ] Gửi lịch + địa điểm (phòng FPT)
- [ ] Incentive đã chốt
- [ ] Consent ghi âm / screen record (nói đầu buổi)

---

## C. Ngày 12–13 — Trong buổi Focus Group

- [ ] In hoặc mở [04-FOCUS-GROUP-SCRIPT.md](./04-FOCUS-GROUP-SCRIPT.md)
- [ ] 1 người **observe** (không hướng dẫn quá nhiều)
- [ ] Ghi **task success/fail** từng flow (Online + Offline)
- [ ] Ghi bug vào sheet (màn | bước | mong đợi | thực tế | P0/P1)
- [ ] Cuối buổi: 3 câu hỏi phỏng vấn ngắn (sẽ trả tiền không? recommend?)

**Không cần deploy** — mọi thứ chạy local.

---

## D. Ngày 14 — Triage bug → BE/FE

| Loại bug | Xử lý |
|----------|--------|
| UI spacing, empty state | FE |
| API trả sai shape / 500 | BE |
| GPS message khó hiểu | FE toast (hoặc BE message nếu cần) |
| Leaderboard trống | Kiểm tra seed SQL |
| Chat không cite nguồn | Kiểm tra `week-1` SQL sources + Gemini |

Quy trình:

1. Gom bug P0 (block demo)
2. BE fix trong repo BE, FE fix trong repo FE
3. Chạy lại `smoke-week2-screens.ps1`
4. Retest 1 vòng trên máy demo

---

## E. Checklist “Tuần 2 xong”

### BE

- [x] Docs `docs/week-2/`
- [x] `smoke-week2-screens.ps1`
- [ ] **Bạn:** smoke pass trước Ngày 11
- [ ] **Bạn:** fix bug P0 từ FG (nếu có)

### FE

- [ ] Polish 5 màn (Ngày 8–9)
- [ ] Loading/error/empty (Ngày 10)
- [ ] `npm run build` pass

### Team

- [ ] 10–16 users đã test (2 buổi)
- [ ] Insights + bug list
- [ ] BMC / testing report draft (CEO/CMO)

---

## F. Lỗi thường gặp khi demo Focus Group

| Triệu chứng | Xử lý |
|-------------|--------|
| Điện thoại không gọi API | `VITE_API_URL` = IP LAN, không `localhost` |
| CORS | Thêm IP vào `CORS_ALLOWED_ORIGINS`, restart BE |
| Check-in fail hàng loạt | Bật `DEMO_ENABLED` hoặc dùng QR + tọa độ từ API |
| Chat chậm / lỗi | Kiểm tra `GEMINI_API_KEY`, quota |
| Leaderboard trống | Chạy lại `fe_compat_data_topup.sql` |
| App crash 360 | FE giảm kích thước ảnh / lazy load |

---

*Tuần 2 — ổn định demo local quan trọng hơn deploy. Tuần 3 mới deploy + 360 thật.*
