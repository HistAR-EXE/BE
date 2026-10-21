# Khôi phục đăng nhập local — 2026-10-21

## Nguyên nhân và bằng chứng
- localhost:5173 vẫn chạy, PostgreSQL healthy, cổng8080 không có listener. /api/profile/me qua Vite trả502.
- Khởi động BE phát hiện Flyway checksum mismatch82: lịch sử có V82__passport_mail_attempts.sql checksum1978920354, nhưng source đã đổi thành V87__passport_mail_attempts.sql. V82 hiện tại là seed_demo_segment_accounts.
- Tính checksum Flyway theo từng dòng UTF8 của V87 cho đúng1978920354, xác nhận đây là đổi số migration, không phải sửa nội dung.

## Khôi phục
- Sao lưu pg_dump custom toàn database local timelens vào .secrets/db-backups/auth-before-v82-reconcile-20261021.dump trước mọi thay đổi.
- Trong transaction, chỉ đổi version82→87 và script V82__passport_mail_attempts.sql→V87__passport_mail_attempts.sql nếu đúng script, checksum, success và chưa có87. Yêu cầu đúng một dòng, nếu không abort.
- Khởi động Maven Spring Boot với SPRING_FLYWAY_OUT_OF_ORDER=true một lần. Các V82–V86, V88–V90 được Flyway chạy thành công. Không reset database, không disable validation, không chạy repair rộng.
- Dùng BE/.env hiện có: DB local timelens; Firebase enabled với service account đã cấu hình. Không sửa hay ghi thông tin bí mật vào báo cáo.
- Backend chạy nềnẩn trên8080, stdout/stderr auth-recovery-migrated.*.log. Log này được gitignore.

## Xác minh
- Firebase Admin SDK initialized for Google login; Tomcat started8080.
- POST /api/auth/login qua localhost:5173 thành công, /api/profile/me trả premium@histar.vn tierPREMIUM.
- Trình duyệt Edge thật, không mock API: đăng nhập demo qua form, mở trang Tái hiện lịch sử được bảo vệ và phát phim1920x1080 thành công.
- Token Google cố ý sai được API trả422 BUSINESS_RULE: Token Google không hợp lệ hoặc đã hết hạn. Không còn502. Chưa thực hiện chọn tài khoản Google thật thay người dùng.

## Khởi động lại lần sau
PostgreSQL phải đang chạy. Trong thư mục BE chạy .\mvnw.cmd -Dmaven.test.skip=true spring-boot:run; không cần OUT_OF_ORDER sau khi đã hoàn tất các migration. Không dùng launcher AR cách ly nếu cần Google vì các launcher đó đặt FIREBASE_ENABLED=false.
Tài khoản demo seed: premium@histar.vn / Demo@2026.
