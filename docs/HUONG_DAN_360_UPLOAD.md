# Hướng dẫn chụp 360 + upload MinIO + cập nhật DB

> Photo Sphere Viewer cần ảnh **equirectangular** tỉ lệ **2:1**.

---

## 1. Chụp bằng điện thoại

| Nền tảng | App |
|----------|-----|
| iPhone / Android | **Google Street View** → chế độ chụp 360 |

1. Đứng giữa không gian, giữ điện thoại cố định
2. Xoay theo chấm — đủ 360° + trên/dưới
3. Xuất ảnh equirectangular

Điểm gợi ý Củ Chi: cổng vào, cửa hầm, bếp Hoàng Cầm, phòng họp, hành lang/giếng nước.

## 2. Upload — API BE (khuyến nghị)

```http
POST /api/panoramas
Authorization: Bearer <token>
Content-Type: multipart/form-data

locationId=11111111-1111-1111-1111-111111111111
title=Cổng vào
file=<ảnh jpg/png>
```

Cập nhật scene có sẵn: `PUT /api/panoramas/{panoramaId}/image`

## 3. MinIO thủ công

Console: http://localhost:9001 → bucket `timelens-media` → `panoramas/cu-chi/`

Sau đó chạy SQL: [database/2026-week3_update_panoramas_cu_chi.sql](./database/2026-week3_update_panoramas_cu_chi.sql)

## 4. Verify

```powershell
curl "http://localhost:8080/api/panoramas/by-location/11111111-1111-1111-1111-111111111111"
```

Hotspot chuyển scene: `type=scene`, `contentRef` = UUID panorama đích.

## Troubleshooting

| Vấn đề | Xử lý |
|--------|--------|
| Ảnh méo | Chụp lại; tỉ lệ 2:1 |
| 403 load ảnh | Bucket public-read; `MINIO_PUBLIC_URL` đúng |
| Không chuyển scene | Hotspot `type=scene` + `contentRef` UUID hợp lệ |
