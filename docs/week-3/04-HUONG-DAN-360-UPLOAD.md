# Hướng dẫn chụp 360 + upload MinIO + cập nhật DB

> CP3 Ngày 15–16. Photo Sphere Viewer cần ảnh **equirectangular** tỉ lệ **2:1**.

---

## 1. Chụp bằng điện thoại (Ngày 15)

### App

| Nền tảng | App |
|----------|-----|
| iPhone / Android | **Google Street View** (miễn phí) → chế độ chụp 360 |

### Cách chụp

1. Đứng giữa không gian, giữ điện thoại cố định
2. Xoay theo chấm trên màn hình — đủ 360° + trên/dưới
3. Xuất ảnh — app ghép **equirectangular** sẵn

### Điểm nên chụp tại Củ Chi (5–7)

1. Cổng vào / khu vực giới thiệu
2. Cửa hầm chính
3. Bếp Hoàng Cầm
4. Phòng họp dưới lòng đất
5. Hành lang / giếng nước (nếu được phép)

### Đặt tên file

```
cu-chi-cong-vao.jpg
cu-chi-bep-hoang-cam.jpg
cu-chi-phong-hop.jpg
```

### Preview trước chuyến đi

- Google Maps → Địa đạo Củ Chi → Street View (tạm cho dev)
- Thay bằng ảnh thật Ngày 16

---

## 2. Xử lý ảnh

| Bước | Công cụ |
|------|---------|
| Kiểm tra tỉ lệ 2:1 | Preview desktop |
| Nén \< 3MB | [tinypng.com](https://tinypng.com), [squoosh.app](https://squoosh.app) |
| Định dạng | JPEG (khuyến nghị) hoặc WebP |

---

## 3. Upload — API (khuyến nghị) hoặc MinIO thủ công

### Cách A — API BE (đã implement)

```http
POST /api/panoramas
Authorization: Bearer <token>
Content-Type: multipart/form-data

locationId=11111111-1111-1111-1111-111111111111
title=Cổng vào
file=<ảnh equirectangular jpg/png>
```

Cập nhật ảnh scene có sẵn:

```http
PUT /api/panoramas/{panoramaId}/image
Authorization: Bearer <token>
file=<ảnh mới>
```

Response `data`: `{ id, locationId, imageUrl, title }` — `imageUrl` dùng trực tiếp cho Virtual Tour.

```powershell
curl -X POST "http://localhost:8080/api/panoramas" `
  -H "Authorization: Bearer $token" `
  -F "locationId=11111111-1111-1111-1111-111111111111" `
  -F "title=Cổng vào" `
  -F "file=@cu-chi-cong-vao.jpg;type=image/jpeg"
```

### Cách B — MinIO console (thủ công)

1. MinIO console: http://localhost:9001
2. Bucket `timelens-media` → folder `panoramas/cu-chi/`
3. Upload file → chạy SQL [2026-week3_update_panoramas_cu_chi.sql](./2026-week3_update_panoramas_cu_chi.sql)

URL mẫu local:

```
http://localhost:9000/timelens-media/panoramas/cu-chi/cu-chi-cong-vao.jpg
```

### Production (S3 / MinIO cloud)

1. Upload cùng path `panoramas/cu-chi/`
2. `MINIO_PUBLIC_URL` = HTTPS base (vd. `https://cdn.example.com` hoặc endpoint public)
3. Test mở URL trên browser điện thoại 4G (không chỉ WiFi nội bộ)

**CLI (tùy chọn):**

```bash
mc alias set prod https://<endpoint> <access-key> <secret-key>
mc cp cu-chi-cong-vao.jpg prod/timelens-media/panoramas/cu-chi/
mc anonymous set download prod/timelens-media
```

---

## 4. Cập nhật database

1. Mở [2026-week3_update_panoramas_cu_chi.sql](./2026-week3_update_panoramas_cu_chi.sql)
2. Thay `https://YOUR-CDN/timelens-media/...` bằng URL thật
3. Chạy trên Postgres:

```powershell
Get-Content docs\week-3\2026-week3_update_panoramas_cu_chi.sql | docker exec -i <pg-container> psql -U timelens -d timelens
```

4. Verify API:

```powershell
curl "http://localhost:8080/api/panoramas/by-location/11111111-1111-1111-1111-111111111111"
curl "http://localhost:8080/api/hotspots/by-panorama/22222222-2222-2222-2222-222222222222"
```

### Hotspot chuyển scene

- `type`: `scene`
- `contentRef`: UUID panorama đích (vd. `22222222-...-222221`)
- FE Virtual Tour plugin đọc và navigate

---

## 5. (Tùy chọn) Cập nhật photo-pairs xưa/nay

Nếu có ảnh thật từ chuyến đi:

```sql
UPDATE photo_pairs
SET
  historical_image = 'https://YOUR-CDN/.../1968-tunnel.jpg',
  current_image = 'https://YOUR-CDN/.../now-tunnel.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111'
  AND caption ILIKE '%hầm%';
```

Không bắt buộc cho CP3 nếu placeholder vẫn ổn.

---

## 6. Troubleshooting

| Vấn đề | Xử lý |
|--------|--------|
| Ảnh méo / không xoay được | Chụp lại; đảm bảo equirectangular 2:1 |
| Mobile load chậm | Nén \< 3MB; lazy load FE |
| 403 khi load ảnh | Bucket public-read; `MINIO_PUBLIC_URL` đúng |
| Tour không chuyển scene | Kiểm tra hotspot `type=scene` + `contentRef` UUID |

---

*Sau bước này FE chỉ cần reload — không đổi API.*
