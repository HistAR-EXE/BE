# Hộ chiếu di sản qua email — 18/10/2026

> Cập nhật tích hợp 19/10: migration hộ chiếu hiện là **V82**, sau các migration mới của nhóm. Các số V76 bên dưới ghi lại lượt kiểm chứng trước tích hợp. Khi triển khai production, áp dụng chuỗi migration chuẩn đến V82 trước khi khởi động bản BE mới; xem `honors-migration-integration-2026-10-19.md`.

## Phần đã triển khai

Hộp thoại giữ thiết kế hộ chiếu hiện tại, xuất ảnh đầy đủ 1200×1500 và có đúng hai thao tác đầu ra: **Nhận qua email** và **Tải ảnh về máy**. Hai thao tác dùng cùng blob đã xem trước; đổi lựa chọn riêng tư hủy ảnh cũ. Phần gửi có trạng thái chờ, tiếp nhận, lỗi, cooldown và số lượt còn lại. Tải xuống vẫn dùng được khi email đang gửi hoặc không khả dụng. Bỏ địa chỉ email tự suy diễn từ UUID, ẩn cả avatar khi ẩn danh.

API chỉ gửi đến email đã xác minh trong DB của tài khoản đăng nhập. Không nhận địa chỉ nhận/người gửi/tiêu đề/nội dung thư tùy ý. Ảnh PNG tối đa2MiB, đúng1200×1500, được kiểm tra kích thước trước giải mã và tái mã hóa trong bộ nhớ để loại metadata. Không lưu ảnh hoặc tạo URL công khai; ảnh lưu niệm không được dùng để cộng XP hay xác nhận thành tích.

Giới hạn bền vững: một yêu cầu mỗi60giây, tối đa3lần trong24giờ trượt. Reservation được commit trước gọi mạng; không giữ khóa hồ sơ trong lúc gửi. RequestID chống gửi trùng và gắn SHA256 ảnh; kết quả không rõ được giữ UNKNOWN, không tự gửi lại. Xác minh email được đọc mới dưới khóa trước khi chốt người nhận. Free/Premium cùng quy tắc.

SMTP/Brevo/Resend hỗ trợ PNG attachment. Sender riêng mặc định `TimeLens <histar.timelens@gmail.com>`, không đổi sender OTP. Mail tắt/testhooks bật không trả thành công giả. `ACCEPTED` nghĩa là dịch vụ tiếp nhận, không khẳng định thư đã vào Inbox.

## DB và khởi tạo

- V76 bổ sung bảng `passport_mail_attempts` và ràng buộc FK/cascade, unique(user,request), hash/state checks, index. Script nhận diện cả bảng đã được Hibernate tạo trước đó, không xóa dữ liệu không tương thích.
- Thêm `spring-boot-starter-flyway`: Spring Boot4 tách auto-configuration Flyway thành module riêng; chỉ có flyway-core không đủ để thuộc tính bật migration có tác dụng. [Hướng dẫn chính thức](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide).
- H2 test profile tắt Flyway vì các bài đó tự tạo schema. PostgreSQL kiểm tra migration riêng.
- Local đã sao lưu trước thay đổi, xác minh lênV76 và đủ các ràng buộc. V74/V75 đã tồn tại nhưng có installed_rank cũ hơn các migration admin; không nhầm thứ tự cài đặt với version hiện tại.
- Lượt API đầu phát hiện3bản ghi QA còn sót do bảng Hibernate thiếu FK. Đã dọn đúng ba dòng của UUID QA do lượt này tạo, bổ sung ràng buộc qua migration rồi chạy lại: cleanup/cascade và fingerprint toàn bộ điểm/cấp độ/huy hiệu/discovery/ledger/attempts khớp trước chạy.

## Kiểm chứng

- Bộ hồi quy BE đầy đủ sau sửa bootstrap: **477 bài qua, 0 thất bại, 0 lỗi, 0 bỏ qua**; gồm PostgreSQL thật trên schema tạm, V76 nhận bảng Hibernate/fresh schema và SMTP capture loopback. Log `target/passport-full-suite.log`. Bộ86bài liên quan trước đó cũng đã qua.
-34kịch bản UI Vinh danh đã được chạy:33qua lượt toàn bộ, ca empty-state gặp timeout khi máy bận và đã chạy lại thành công. Bao gồm Top10/đồng hạng, vị trí cá nhân, dữ liệu server, lỗi/retry, tài khoản khác, timeline/spotlights, mobile, PNG, email/cooldown/lỗi/quota/pending và ẩn danh.
- `npm run ci` qua: TypeScript/build thành công, lint0lỗi;13cảnh báo có sẵn ngoài phần mới, cảnh báo bundle chung vẫn còn.
- API thật + PostgreSQL + SMTP capture `127.0.0.1:2526` qua5nhóm kiểm tra: quyền truy cập/no-store, đúng người nhận/sender, replay không gửi trùng, giới hạn60giây/24giờ, ảnh sai/người nhận tự truyền/email chưa xác minh bị từ chối, dọn QA và bảo toàn tiến độ.
- Ảnh đính kèm sau chuẩn hóa khớp toàn bộ1.800.000pixel với PNG tải xuống (`scripts/ComparePassportPixels.java`); khác biệt metadata/nén không ảnh hưởng nội dung.
- Backend local8080 đã bật bằng runtime mới, Flyway xác nhậnV76; chín nhóm hồi quy API Vinh danh/Explore/email-status qua, dọn QA và fingerprint dữ liệu gốc khớp. Runtime capture8081 đã dừng.
- Không có thư thật nào được gửi ra Internet trong kiểm thử. Capture chỉ gửi đến QA@example.invalid qua127.0.0.1. Backend capture8081 đã được dừng sau test.

## Khi triển khai thật

Xem `docs/mail-passport-config.md`. Quản trị dịch vụ email cần xác thực đúng sender `histar.timelens@gmail.com` và cung cấp cấu hình provider hợp lệ qua secrets. Không đưa mật khẩu/API key vào chat hay Git. Production hiện chủ động tắt Flyway lúc boot theo quy trình nhóm: phải áp dụng V76 bằng quy trình migration trước khi phát hành code. Không tự đổi sang Gmail SMTP hay bật migration production khi chưa đối chiếu DB đích.

Phần này chưa commit/push/deploy trong lượt làm việc. Chưa xác minh thư thật đến hộp thư production hoặc sender đã được provider chấp thuận; không thể suy ra các điều đó từ kiểm thử local.
