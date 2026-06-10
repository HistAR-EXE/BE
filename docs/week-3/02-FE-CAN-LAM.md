# CP3 Tuần 3 — FE cần làm

> **Đọc trước:** [01-BE-DA-LAM.md](./01-BE-DA-LAM.md) · **Chạy tay:** [03-THAO-TAC-BAN-TU-LAM.md](./03-THAO-TAC-BAN-TU-LAM.md)

---

## 0. Điều kiện

- [ ] Tuần 1–2 xong (mode 2 chiều, polish, Focus Group insights đã fix P0)
- [ ] BE production deploy Ngày 17 xong
- [ ] `VITE_API_URL` Vercel trỏ BE prod

---

## 1. Checklist theo ngày

### Ngày 15 — (Field) Ảnh tư liệu

- [ ] Phối hợp chuyến Củ Chi — chụp theo [04-HUONG-DAN-360-UPLOAD.md](./04-HUONG-DAN-360-UPLOAD.md)
- [ ] (Tùy chọn) Chụp thêm ảnh xưa/nay cho photo slider

### Ngày 16 — Virtual Tour ảnh thật

- [ ] Sau BE upload + SQL panorama: **không cần đổi API client**
- [ ] `GET /api/panoramas/by-location/{cuChiId}` — `imageUrl` mới từ CDN
- [ ] Virtual Tour plugin đọc `imageUrl` trực tiếp — verify 3 scene load mobile
- [ ] Hotspot `type: scene` → `contentRef` = panorama UUID đích (đã có logic Tuần 1)
- [ ] Nén/ lazy load — tránh crash mobile
- [ ] Gỡ fallback Street View hardcode nếu đang dùng tạm

### Ngày 17 — Production

- [ ] Vercel env: `VITE_API_URL=https://<be-production-host>`
- [ ] Redeploy FE sau đổi env
- [ ] Test CORS: login, locations, chat từ domain Vercel
- [ ] Không dùng `localhost` trong build production

### Ngày 18 — Survey

- [ ] Link survey gửi **URL Vercel production** (không local)
- [ ] Form 5–10 câu ngắn (NPS, usability, willingness to pay)
- [ ] CEO/CMO gom 10+ responses

### Ngày 19 — Demo video + pitch assets

- [ ] Quay video **~3 phút**: Online flow + Offline flow + 1 câu chat có **Nguồn**
- [ ] Screenshot 5 màn đã polish cho pitch deck
- [ ] Q&A bank: AR = roadmap, không phải scope CP3

### Ngày 20–21 — Rehearsal

- [ ] Pitch 10 phút, rehearsal 2–3 lần
- [ ] Demo live trên production URL + QR mở app
- [ ] Backup: phát video nếu live fail
- [ ] `DEMO_ENABLED` — chỉ bật khi cần cứu check-in (coord với BE)

---

## 2. API production — không đổi contract

Giữ nguyên Tuần 1–2:

- Locations → `data.items`
- Leaderboard → `data.entries`
- Chat → `data.reply` (có dòng Nguồn)
- Auth → `accessToken` / `token`

```typescript
const API_BASE = import.meta.env.VITE_API_URL; // phải là https:// BE prod trên Vercel
```

---

## 3. Virtual Tour — binding sau ảnh thật

```typescript
const { data: panoramas } = await api.get(`/api/panoramas/by-location/${CU_CHI_ID}`);
// panoramas[].imageUrl — URL HTTPS từ MinIO/S3 sau Ngày 16

const { data: hotspots } = await api.get(`/api/hotspots/by-panorama/${activePanoramaId}`);
// type === 'scene' → navigate to hotspot.contentRef (panorama id)
```

Không thêm endpoint mới.

---

## 4. Checklist demo live (pitch)

- [ ] Login production < 30s
- [ ] Explore load ảnh cover
- [ ] 360 xoay mượt (wifi/hotspot)
- [ ] Chat 1 câu Củ Chi + thấy Nguồn
- [ ] Check-in hoặc demo check-in backup
- [ ] Leaderboard có entries (seed prod)

---

## 5. Không làm Tuần 3

- Rewrite FE architecture
- Thêm tính năng mới ngoài plan
- AR computer vision
- Đổi contract API

---

*FE ship Vercel prod + tour ảnh thật + video backup — BE deploy Ngày 17.*
