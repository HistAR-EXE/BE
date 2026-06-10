# CP3 Tuần 3 — Thao tác bạn cần tự làm

> Ngày 15–21: field trip → ảnh thật → deploy → survey → pitch.  
> Chi tiết kỹ thuật: [04-HUONG-DAN-360-UPLOAD.md](./04-HUONG-DAN-360-UPLOAD.md), [05-DEPLOY-PRODUCTION.md](./05-DEPLOY-PRODUCTION.md).

---

## Tổng quan

| Ngày | Việc chính | File tham chiếu |
|------|------------|-----------------|
| 15 | Chụp 360 Củ Chi | 04 |
| 16 | Upload + SQL panorama | 04 |
| 17 | Deploy BE + verify | 05 |
| 18 | Survey online | Mục C |
| 19 | Video + deck | Mục D |
| 20–21 | Rehearsal + final | Mục E |

---

## A. Trước Ngày 15

- [ ] Tuần 2 Focus Group xong, bug P0 đã fix
- [ ] `smoke-week2-screens.ps1` pass local
- [ ] Chuẩn bị: điện thoại, Google Street View app, pin, xin phép chụp tại di tích

---

## B. Ngày 15 — Chuyến Củ Chi

Checklist field:

- [ ] 5–7 điểm 360: cổng vào, hầm, bếp Hoàng Cầm, phòng họp, …
- [ ] Mỗi điểm 1 sphere equirectangular 2:1
- [ ] Đặt tên file: `cu-chi-cong-vao.jpg`, `cu-chi-bep-hoang-cam.jpg`, …
- [ ] (Tùy chọn) Ảnh xưa/nay cho photo-pairs

Xem hướng dẫn chi tiết: [04-HUONG-DAN-360-UPLOAD.md](./04-HUONG-DAN-360-UPLOAD.md)

**Nếu không đi được:** dùng Street View / placeholder — vẫn đủ demo.

---

## C. Ngày 16 — Upload + DB

1. Nén ảnh \< 3MB/scene (tinypng / squoosh)
2. Upload lên MinIO bucket `timelens-media/panoramas/cu-chi/`
3. Lấy public URL (`MINIO_PUBLIC_URL` + path)
4. Sửa placeholder trong `2026-week3_update_panoramas_cu_chi.sql`
5. Chạy SQL trên DB **local** test trước, sau đó **production** (Ngày 17 sau migrate)

Verify local:

```powershell
curl "http://localhost:8080/api/panoramas/by-location/11111111-1111-1111-1111-111111111111"
# imageUrl phải mở được trên browser
```

---

## D. Ngày 17 — Deploy

Làm theo từng bước: [05-DEPLOY-PRODUCTION.md](./05-DEPLOY-PRODUCTION.md)

Tóm tắt:

1. Tạo Postgres managed + chạy SQL (`run-all-seed.ps1` hoặc 6 file thủ công — [CP3_AUDIT_STATUS.md](../CP3_AUDIT_STATUS.md))
2. Tạo MinIO/S3 bucket public-read
3. Railway/Render: deploy Dockerfile, set env
4. `powershell -ExecutionPolicy Bypass -File docs\scripts\smoke-production.ps1 -BaseUrl https://<be-host>`
5. Vercel: `VITE_API_URL` → redeploy FE
6. Test login + chat trên URL Vercel

**Deadline:** xong **trước** Ngày 20 rehearsal ít nhất 1 ngày.

---

## E. Ngày 18 — Survey online

- [ ] Google Form 5–10 câu (tiếng Việt)
- [ ] Gửi link **https://your-app.vercel.app** (production)
- [ ] Mục tiêu ≥ 10 responses (cộng Focus Group Tuần 2 = ≥ 20)
- [ ] CEO/CMO draft testing report 5–7 trang (FG + survey)

Gợi ý câu hỏi:

1. Bạn hiểu app làm gì sau 1 phút dùng? (1–5)
2. Tính năng hữu ích nhất?
3. Điều khó chịu nhất?
4. Bạn có trả phí không? Mức nào? (VND/tháng)
5. Bạn có giới thiệu bạn bè không? (NPS 0–10)

---

## F. Ngày 19 — Video + pitch

- [ ] Video 3 phút: đăng ký → Online (explore, 360, chat) → Offline (scan, quest) → leaderboard
- [ ] Lưu MP4 backup USB + cloud
- [ ] Pitch deck: screenshot UI polish + BMC slide (CEO/CMO)
- [ ] Q&A: AR, chi phí server, bảo mật dữ liệu

---

## G. Ngày 20–21 — Rehearsal & final

### Setup demo phòng

- [ ] Laptop + HDMI + hotspot dự phòng
- [ ] Production URL bookmark
- [ ] Account `demo@histar.vn` đã login sẵn tab
- [ ] Video backup mở sẵn

### Rehearsal

- [ ] Canh 10 phút (demo 5–7 phút + Q&A)
- [ ] Mở đầu: *"Sản phẩm chạy thật"* + mời quét QR
- [ ] Demo live 2 mode + chat cite nguồn
- [ ] 2–3 lần rehearsal

### Ngày 21 final

- [ ] `smoke-production.ps1` pass sáng demo
- [ ] Chat Gemini quota OK
- [ ] `DEMO_ENABLED=false` (bật chỉ khi cần)
- [ ] Nghỉ ngơi — không merge feature mới

---

## H. Checklist “CP3 xong”

### BE

- [ ] Production deploy + health ready 200
- [ ] SQL prod đầy đủ + panorama URL thật (hoặc fallback OK)
- [ ] `smoke-production.ps1` pass

### FE

- [ ] Vercel prod + CORS OK
- [ ] Virtual tour ảnh load mobile
- [ ] Video backup 3 phút

### Team / CP3 deliverable

- [ ] BMC + pricing (CEO/CMO)
- [ ] Customer testing ≥ 20 users + report 5–7 trang
- [ ] Prototype live + pitch sẵn sàng

---

## I. Rủi ro & cứu cánh

| Rủi ro | Cứu cánh |
|--------|----------|
| Deploy fail Ngày 17 | Buffer đến Ngày 20; dùng video + local backup |
| Ảnh 360 kém | Street View / 1 scene vẫn demo |
| Live demo fail | Video 3 phút |
| GPS/QR fail | `DEMO_ENABLED` + manual check-in |
| WiFi phòng | Hotspot riêng |

---

*Tuần 3 — ship production, không scope creep.*
