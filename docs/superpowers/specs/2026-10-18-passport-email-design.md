# Gửi hộ chiếu di sản qua email

Trạng thái: người dùng đã chấp thuận và bổ sung yêu cầu đúng hai nút đầu ra email/tải ảnh. Giữ nguyên các thay đổi BE/FE chưa commit hiện có.

## Mục tiêu và lựa chọn

Hộp thoại hộ chiếu có đúng hai nút đầu ra: **Nhận qua email** và **Tải ảnh về máy**, thay cho chia sẻ trực tiếp trước đây. Gửi đúng ảnh đã chuẩn bị đến email đã xác minh của tài khoản đang đăng nhập, từ **TimeLens <histar.timelens@gmail.com>**. Áp dụng như nhau cho Free/Premium.

Phương án khuyến nghị: dùng PNG đã xem trước, kiểm tra và chuẩn hóa lại ảnh ở BE, đính kèm bằng hạ tầng email hiện có. Ưu điểm: giữ đúng thiết kế hộ chiếu mới và lựa chọn riêng tư. Đây là ảnh lưu niệm do người dùng tạo, không phải bằng chứng được ký số; không dùng ảnh upload để xác nhận hay cộng thành tích.

Phương án thay thế: BE tự vẽ lại ảnh từ dữ liệu DB và lựa chọn hiển thị. Dữ liệu trong ảnh được kiểm soát chặt hơn nhưng cần duy trì bộ dựng ảnh thứ hai và có thể khác bản xem trước hiện tại. Không thêm ô gửi đến email tùy ý trong phạm vi này.

## Luồng người dùng

1. Người dùng tạo ảnh như hiện tại. Nút email chỉ bật khi ảnh đã sẵn sàng.
2. Hiển thị rõ gửi về email tài khoản đã xác minh, không suy diễn địa chỉ từ UUID. Không nhận người nhận do client tự truyền.
3. Người dùng bấm gửi; khóa thao tác lặp trong lúc xử lý. Giữ nguyên lựa chọn ẩn tên/XP/cấp độ/huy hiệu.
4. Chỉ báo **Đã được dịch vụ email tiếp nhận** sau khi nhà cung cấp chấp nhận; không khẳng định thư đã vào Inbox. Hiển thị hướng dẫn kiểm tra Spam.
5. Cấu hình tắt, lỗi mạng, email chưa xác minh, hết hạn phiên và giới hạn gửi có thông báo riêng. Tải PNG vẫn dùng được.

## Backend và bảo mật

- Endpoint xác thực, chỉ gửi về email đã xác minh lấy từ DB của người gọi. Không có trường from/to/subject/html tùy ý.
- From được cấu hình riêng cho tính năng hộ chiếu, không tự thay đổi người gửi của email OTP/khôi phục tài khoản.
- Chỉ nhận PNG dung lượng tối đa 2 MiB, kích thước đúng bộ dựng ảnh hiện tại; kiểm tra header/kích thước trước giải mã, rồi tái mã hóa để bỏ metadata và dữ liệu phụ. Không tải URL ảnh do client cung cấp ở BE.
- Giới hạn theo tài khoản: ít nhất 60 giây giữa các yêu cầu và tối đa 3 lần/24 giờ. Lưu kiểm soát trong DB để không mất khi restart hoặc chạy nhiều instance. Chống gửi trùng theo request key; không tự retry khi kết quả gửi không rõ ràng.
- Không giữ ảnh lâu dài trong hồ sơ/DB; không công khai URL ảnh. Lịch sử tối thiểu chỉ lưu người yêu cầu, thời gian, mã yêu cầu và trạng thái cần cho giới hạn gửi. Không ghi ảnh/token/nội dung nhạy cảm vào log.
- Không thay đổi XP, huy hiệu, discovery, ledger hoặc cấp độ. Các API đọc xếp hạng giữ no-store và quy tắc Top 10 đã chốt.
- Mở rộng hạ tầng mail cho attachment SMTP/Brevo/Resend, giữ tương thích email cũ. Mail bị tắt hoặc test hooks không được trả thông báo giả rằng thư thật đã gửi.

## Cấu hình và kiểm thử

Địa chỉ From chưa được xác minh bằng quyền truy cập provider. Người quản lý triển khai cấu hình biến môi trường và xác thực sender; không gửi mật khẩu Gmail/API key vào chat hoặc commit. Không tự chuyển production sang SMTP Gmail vì hiện production dùng Brevo.

Kiểm thử unit/integration: attachment thực, xác thực, quyền sở hữu, verified email, ảnh lỗi/quá lớn/sai kích thước, đồng thời, giới hạn gửi, request trùng, cấu hình tắt, provider từ chối/timeout, không làm đổi dữ liệu thành tích. Chạy hồi quy các lớp leaderboard/mail và truy vấn PostgreSQL.

Kiểm thử UI toàn bộ bộ leaderboard hiện có và các ca mới: tạo ảnh → gửi, trạng thái loading/success/error/cooldown, đổi tùy chọn làm ảnh cũ mất hiệu lực, chuyển tài khoản, responsive và tải/chia sẻ cũ. Kiểm thử thư bằng SMTP capture local hoặc provider giả lập; gửi thư thật chỉ khi cấu hình hợp lệ và người dùng bấm gửi.

Không claim production hoàn tất trước khi cấu hình sender được xác nhận và thư thật được kiểm tra ở hộp thư đích.
