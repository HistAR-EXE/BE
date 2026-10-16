# Rà soát Khám phá — 17/10/2026

## Kết quả

Đã rà soát và sửa các lỗi xác nhận được trong luồng Explore hiện tại. Không thay đổi XP, thành tích hay hồ sơ cá nhân để thực hiện kiểm thử; không reset DB và không thêm migration trong đợt này.

| Kiểm tra | Kết quả | Bằng chứng |
| --- | --- | --- |
| Toàn bộ backend | 348 ca, 0 lỗi/thất bại/bỏ qua | `target/explore-review-full.log`, Surefire |
| Explore backend sau điều chỉnh catalog cuối | 28/28 | `target/explore-review-final-focused.log` |
| UI mô phỏng | 12/12, không flaky/bỏ qua | `../FE/explore-review-ui.log`, `../FE/test-results/explore/results.json` |
| UI/API/PostgreSQL thật | 26/26 | `../FE/explore-review-live.log`, `../FE/test-results/explore-live/results.json` |
| Đối chiếu catalog FE/BE | 13/13 khóa, XP, số lựa chọn khớp | `node scripts/verify-explore-catalog.cjs` trong FE |
| Frontend build và lint file thay đổi | Đạt | `../FE/explore-review-build.log`, `../FE/explore-review-lint.log` |
| Kiểm toán số liệu toàn DB, chỉ đọc | 0 lệch số dư, 0 điểm âm, 0 sự kiện thưởng trùng | SQL đối chiếu `profiles.total_points` với tổng `xp_ledger.points` theo user |
| Dọn dữ liệu thử | Đạt, không còn tài khoản QA | `fixturesCleaned: true` và truy vấn xác nhận DB |

Toàn bộ backend được chạy sau bản sửa giao dịch và bổ sung kiểm thử. Sau điều chỉnh cuối về số lựa chọn, 28 ca backend liên quan được chạy lại và đều đạt. Kiểm thử live xác nhận các luồng thành công và bảo toàn dữ liệu trên server local; nhánh catalog bị thiếu được tái hiện trong DB kiểm thử cô lập, không xóa catalog của DB đang sử dụng.

## Các vấn đề đã sửa

1. Hai effect cùng giữ khóa cuộn và bắt Escape: có thể để `body.overflow=hidden` sau khi đóng popup hoặc bỏ qua hai huy hiệu cùng lúc. Chỉ giữ một handler/khóa cuộn; kiểm thử đóng popup, trả cuộn và lần lượt đóng hai huy hiệu.
2. Popup chúc mừng được xếp hàng trong khi đang đọc hồ sơ/giải mật thư: trước đây có thể khóa trang khi popup không nằm trong màn hình đang render, hoặc nhiều dialog dùng chung focus ref. Hoãn popup chúc mừng tới khi người dùng rời nội dung đang mở.
3. Kết quả mật thư/hồ sơ đến chậm có thể cập nhật thông báo thành công của một nội dung khác vừa mở. Gắn phản hồi với phiên mở nội dung; có ca tái hiện mật thư trước khi sửa và ca hồi quy đạt sau sửa.
4. Yêu cầu đang lưu của tài khoản cũ làm tài khoản mới bị khóa thao tác. Reset trạng thái pending khi chuyển tài khoản; phản hồi/finally của phiên cũ không được can thiệp phiên mới.
5. Thiếu catalog DB khi admin làm thử thách: API có thể trả `passed=true` dù discovery không được lưu. Bắt buộc kiểm tra kết quả ghi, nếu thất bại thì báo lỗi và rollback cả attempt/thưởng. Ca kiểm thử xóa catalog chỉ chạy trong transaction DB test và rollback.
6. Thẻ mật thư và hồ sơ chỉ nhận click chuột. Bổ sung tên truy cập, focus và thao tác Enter/Space; kiểm thử cả hai loại thẻ.
7. UI mật thư/hồ sơ có ba lựa chọn nhưng catalog server khai báo bốn. Đồng bộ server thành ba lựa chọn; thêm script đối chiếu tự động 13 thử thách để phát hiện lệch XP, khóa, số lựa chọn và việc vô tình đưa trường đáp án đúng vào dữ liệu UI.

## Phạm vi bảo mật và dữ liệu đã kiểm tra

- API lấy danh tính từ phiên xác thực; endpoint nhận thưởng không lấy user/XP/cấp độ do browser tự khai.
- Chấm đáp án phía server; yêu cầu đủ chứng cứ trước kết luận; chặn gửi khóa thưởng qua discovery endpoint thông thường.
- Lock tài khoản và sổ thưởng idempotent; gửi trùng/đồng thời, lỗi transaction, rollback và giới hạn lần thử.
- Chín huy hiệu dựa trên chứng cứ DB và XP toàn tài khoản. Kiểm thử tổng thưởng Explore là 1.130, mọi lựa chọn của 13 câu hỏi, replay, không đủ chứng cứ và đủ điều kiện cả chín huy hiệu.
- Bằng chứng hội thoại có cờ do server tạo; dữ liệu assistant do client sync không được dùng làm chứng cứ AI.
- Snapshot phân tách tài khoản; phản hồi trễ; localStorage giả; tải lại; xóa bộ nhớ rồi khôi phục phiên hợp lệ; Profile/Explore thống nhất XP.
- Ngắt yêu cầu lưu có chủ đích trong Playwright: UI không tự nhận thưởng; khôi phục mạng lưu được một lần. Các yêu cầu thành công của live suite dùng backend/DB thật.
- Mobile dialog, focus, Tab, Escape, trả cuộn, hàng đợi huy hiệu và truy cập thẻ bằng bàn phím.

## Giới hạn trước phát hành

Đây là bằng chứng cho mã nguồn và môi trường local đã kiểm tra, không phải chứng nhận an toàn tuyệt đối. Chưa kiểm thử end-to-end lần này với cấu hình production thực tế, tải lớn kéo dài, GPS/QR trên thiết bị tại địa điểm, nhà cung cấp AI thực và khôi phục toàn bộ backup. Không triển khai hoặc thay đổi dịch vụ production trong đợt rà soát này.

Production config hiện khai báo `ddl-auto: validate`, test hooks mặc định tắt và Flyway mặc định không chạy lúc boot. Vì vậy khi phát hành phải xác nhận migration đã được áp dụng trước khi chạy bản backend mới. Đợt sửa này không cần migration mới.

Build còn cảnh báo bundle lớn; đây là hạng mục tối ưu hiệu năng chưa được xử lý trong đợt sửa tính đúng đắn này.

## Lặp lại

Trong BE: `.\mvnw.cmd test -q`.

Trong FE: `node scripts/verify-explore-catalog.cjs`, `npx playwright test --config playwright.explore.config.ts`, `npm run build`.

Live local (cần BE:8080, FE:5173, container histar-postgres): `node scripts/test-explore-live.cjs`. Script chỉ tạo tài khoản QA riêng; xác minh UUID/email trước khi dọn chúng trong transaction; không lưu mật khẩu/token vào báo cáo.
