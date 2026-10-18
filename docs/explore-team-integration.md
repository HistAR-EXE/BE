# Tích hợp Khám phá với cập nhật nhóm đến V73

## Code và migration

- BE tích hợp `origin/master` tại `673c26b`; FE tại `8e6ed3b`. Giữ nguyên lịch sử và code Admin/Narrative của nhóm.
- Migration Khám phá chưa công bố được chuyển từ V68/V69 sang **V80/V81** vì nhánh nhóm đã dùng V74–V79 cho các migration Admin khác. V68–V79 chuẩn của nhóm không được sửa nội dung/checksum.
- Nội dung hai file SQL Khám phá giữ nguyên để bảo toàn checksum đã áp dụng local; chú thích số phiên bản cũ trong file là lịch sử, tên file mới quyết định version.
- Các báo cáo kiểm thử ngày 16–17/10 trước tích hợp mô tả V68/V69 Khám phá cũ; từ bản tích hợp này dùng V80/V81.
- Đã giữ toàn bộ asset huy hiệu/mặt cắt và thay đổi UI của người dùng, gồm việc bỏ nhánh AR cũ theo lựa chọn trước đó.

## DB đã dùng lịch sử chuẩn của nhóm (đến V73)

Backup trước. Chạy migration bình thường theo quy trình triển khai hiện tại. Với workspace có Docker Compose ở thư mục cha của BE:

```powershell
docker compose run --rm --no-deps postgres-migrate
```

Đường này chỉ thêm V80/V81. **Không dùng script chuyển lịch sử legacy cho DB chuẩn của nhóm.** Không chạy bootstrap lên DB đang có dữ liệu.

## DB local đã áp dụng V68/V69 Khám phá cũ

DB local kiểm tra trong phiên này có baseline V67, `V68__cu_chi_explore_archive_points.sql`, `V69__reward_integrity.sql`. Chúng không phải V68/V69 của nhóm. Không dùng `flyway repair`, không xóa bảng/history và không reset volume để bỏ qua xung đột.

DB `timelens` đang sử dụng **chưa bị thay đổi** trong quá trình tích hợp Git. Đường nâng cấp dưới đây đã được kiểm tra bằng bản sao phục hồi từ backup DB này, không phải chạy trực tiếp lên DB người dùng.

1. Dừng ghi dữ liệu từ BE, tạo full backup và phục hồi một DB clone để thử trước.
2. Trong BE, kiểm tra dry-run; mặc định rollback toàn bộ thay đổi metadata:

```powershell
.\scripts\reconcile-local-explore-history.ps1 -Database TEN_DB_CLONE
```

3. Khi dry-run đúng, áp dụng trên DB clone:

```powershell
.\scripts\reconcile-local-explore-history.ps1 -Database TEN_DB_CLONE -Apply
```

Script chỉ chấp nhận đúng tên file, checksum và trạng thái thành công của hai migration legacy đã biết; từ chối DB khác hoặc V80/V81 đã bị chiếm. Khi `-Apply`, script tạo full dump và chép về `BE/target/db-backups` trước khi sửa hai dòng lịch sử. Sao chép dump ra nơi lưu trữ lâu dài; không commit dump.

4. Chạy Flyway trên **DB clone đã chọn**, bật `outOfOrder=true` một lần để áp dụng V68–V73 của nhóm. Không trỏ nhầm sang DB thật. Sau đó chạy validate/migrate bình thường và đối chiếu XP, ledger, discovery, huy hiệu, attempts trước/sau.
5. Sau khi clone đạt, lặp lại trên DB local đích với backup mới. Với DB đích `timelens` được cấu hình trong Compose, sau script `-Apply` có thể dùng từ workspace root:

```powershell
docker compose run --rm --no-deps -e FLYWAY_OUT_OF_ORDER=true postgres-migrate
docker compose run --rm --no-deps postgres-migrate
```

Chỉ bật out-of-order cho đường nâng cấp legacy này; không bật mặc định cho ứng dụng hay production. Metadata version/script được chuyển sang V80/V81, còn checksum, timestamp, installed rank và dữ liệu ứng dụng được giữ nguyên.

## Kiểm tra bản tích hợp

- Backend đầy đủ sau merge và sửa xóa mềm: 415 ca đạt; bổ sung chạy riêng cả trường hợp nhiệm vụ active/deleted sau đó.
- Frontend build đạt, Admin state 16/16, Explore UI 12/12, đối chiếu catalog 13/13. Lint toàn FE: 0 errors, 13 warnings hiện có.
- Chuyển legacy history rồi áp dụng đủ V68–V73 trên DB clone: đạt; Flyway validate thông thường xác nhận 76 migration hợp lệ.
- So sánh với DB gốc: giữ nguyên 14 hồ sơ (XP/cấp độ), 14 discovery, 5 huy hiệu, 28 ledger entry và 18 attempts.
- Bootstrap mới đến V81 trên DB trống: đạt. Generator giữ transaction cho từng migration để V72 `ON COMMIT DROP` hoạt động giống Flyway.
- Reseed Analytics demo trên schema V75: trước sửa gây 120 số dư lệch ledger; sau sửa về 0. Chỉ tái lập baseline cho tài khoản demo vừa tạo lại, không sửa migration V72 đã công bố.
- Luồng dialogue đã chặn nhiệm vụ xóa mềm; nhiệm vụ còn hoạt động vẫn nhận thưởng bình thường.

Các kiểm thử không thay thế bước xác nhận cấu hình production, backup/restore production, dịch vụ AI và thiết bị GPS thực. Không tự merge PR hoặc triển khai production trong công việc này.
