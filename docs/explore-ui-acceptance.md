# Kiểm thử nghiệm thu Khám phá

## Phạm vi và nguồn dữ liệu

- XP chính và cấp độ là của toàn tài khoản, lấy từ backend. XP Củ Chi là phần thống kê riêng, không cộng lần nữa vào tổng tài khoản.
- Thành tích chỉ khôi phục từ DB. Không nhập dữ liệu cũ chỉ tồn tại trong localStorage.
- Xóa toàn bộ localStorage sẽ xóa cả phiên đăng nhập: cần đăng nhập lại cùng tài khoản trước khi kiểm tra dữ liệu.
- Trải nghiệm sa bàn có biên nhận riêng, không thưởng XP. Mật thư, hồ sơ và thử thách tầng được chấm ở server.
- Hội thoại tự đồng bộ từ client được lưu nhưng không được coi là bằng chứng server đã trả lời. Để ghi nhận bước nhiệm vụ hội thoại, gửi đầy đủ câu hỏi gợi ý và chờ phản hồi server; các câu hỏi khác vẫn có thể dùng để trao đổi.

## Trước khi test trên dữ liệu ứng dụng

1. Dùng môi trường local/staging và hai tài khoản thử nghiệm A/B.
2. Sao lưu DB bằng quy trình hiện có. Không xóa bảng, không chạy lại seed để đặt lại dữ liệu.
3. Áp dụng Flyway V68 và V69, xác nhận cả hai `success=true`. DB local lúc kiểm tra vẫn ở V67; các migration mới chưa được áp dụng vào dữ liệu ứng dụng.
4. Với DB đã tồn tại và PostgreSQL đang chạy, tác vụ migration là `docker compose run --rm --no-deps postgres-migrate`; không chạy lại seed chỉ để nâng cấp schema. Kiểm tra cấu hình môi trường trước khi chạy để tránh trỏ nhầm DB. Dừng backend trước khi migrate vì cấu hình Hibernate `ddl-auto: update` có thể tự tạo bảng trước Flyway.
5. Khởi động backend với code mới và frontend với code mới. Trong Network, `GET /api/me/explore` phải trả 200 với `profile`, `discovery.keys`, `archiveXp`, `badges`.

## Các ca nghiệm thu UI

| Ca | Thao tác | Kết quả cần đạt |
|---|---|---|
| Tổng XP/cấp độ | Đăng nhập A, đối chiếu Khám phá và Hồ sơ. Hoàn thành một hoạt động ở tính năng khác rồi trở lại Khám phá. | Tổng XP/cấp độ thống nhất theo server; thống kê Củ Chi không được cộng chồng lên tổng. |
| Xóa localStorage | Ghi lại XP, huy hiệu, các bài đã hoàn thành. DevTools → Application → Local Storage → xóa dữ liệu của ứng dụng. Tải lại rồi đăng nhập A. | Dữ liệu đã có trong DB xuất hiện lại. Không có request nhập bù key từ localStorage. |
| Thiết bị khác | Đăng nhập A trong cửa sổ riêng tư hoặc trình duyệt khác. | XP, cấp độ, thành tích đã lưu giống nhau sau khi tải dữ liệu. |
| Đổi tài khoản | A đã có thành tích → đăng xuất → đăng nhập B trên cùng trình duyệt → quay lại A. | B chỉ thấy dữ liệu B; A vẫn giữ dữ liệu A. |
| Đáp án sai/đúng | Chọn sai một đáp án tầng, sau đó chọn đúng. | Sai không có XP, vẫn được thử lại. Đúng chỉ hiển thị thưởng sau khi API xác nhận. |
| Gửi lặp | Tải lại bài đã hoàn thành, mở hai tab cùng tài khoản và thử hoàn thành cùng một bài. | Chỉ một lần thưởng; tải lại/tái gửi không tăng thêm XP. |
| Hai hoạt động đồng thời | Hai tab hoàn thành hai hoạt động khác nhau gần cùng lúc. | Tổng điểm giữ đủ hai khoản thưởng và các khoản thưởng quest hợp lệ đi kèm. |
| Mật thư/hồ sơ | Trả lời sai rồi đúng, đóng/mở lại hồ sơ, tải lại trang. | Chỉ kết quả server xác nhận mới mở khóa; trạng thái đã lưu khôi phục được. |
| Điều tra | Thử kết luận trước khi có đủ chứng cứ, sau đó ghi nhận đủ bốn chứng cứ và kết luận. | Chưa đủ không được kết luận. Đủ điều kiện nhận thưởng một lần. |
| Mất mạng | DevTools → Network → Offline trước khi nộp bài. Kết nối lại và thử lại/tải lại. | Không báo thưởng thành công giả. Nếu request trước đã được server lưu nhưng mất phản hồi, tải lại sẽ phục hồi và không thưởng trùng. |
| Lỗi tải | Chặn `/api/me/explore` rồi tải trang; bỏ chặn và bấm Thử lại. | Có thông báo lỗi và nút thử lại; không trình bày dữ liệu chưa tải thành số 0 chính thức. |
| Giới hạn gửi | Gửi nhiều đáp án sai liên tiếp trên tài khoản test. | Backend giới hạn lượt gửi trong một phút và UI hiển thị hướng dẫn chờ. |
| Sa bàn | Bắt đầu trải nghiệm Cedar Falls, quay lại Khám phá; xóa storage rồi đăng nhập lại. | Biên nhận trải nghiệm còn trong DB; không phát sinh XP chỉ vì mở sa bàn. |
| Hội thoại nhiệm vụ | Vào Chat từ nhiệm vụ, dùng câu gợi ý, đợi server trả lời, quay lại nhiệm vụ. | Bước hội thoại được lưu. Không tự đánh dấu chỉ vì mở trang Chat. |
| Huy hiệu | Mở huy hiệu đã có và chưa có, kiểm tra điều kiện và tiến độ. | Huy hiệu lấy từ DB, đã nhận hiển thị 100%; điều kiện XP dùng XP toàn tài khoản. |
| Mobile/bàn phím | Màn hình 390px; dùng Tab/Shift+Tab trong modal, Escape để đóng. | Nội dung dùng được, focus ở trong modal và trở lại nút mở sau khi đóng. |

## Kiểm tra API/DB bổ sung

- Gửi key `depth_quiz:*`, `cipher:*`, `dossier:*`, `minigame:*`, `feature:*` trực tiếp vào `POST /api/me/discoveries`: phải bị từ chối.
- API không nhận `userId` hay số XP do client chọn để cấp thưởng. Tài khoản lấy từ phiên xác thực.
- Sau một hoạt động, đối chiếu `profiles.total_points`, `user_discoveries`, `xp_ledger` và `user_badges`.
- `baseline:v1` giữ số dư lịch sử; các sự kiện mới có khóa chống thưởng trùng. Số dư lịch sử không phải bằng chứng mọi điểm cũ đều hợp lệ.
- Không thử các ca gửi lặp/gian lận trên tài khoản thật hoặc dữ liệu production.

## Lệnh kiểm tra tự động

Backend: chạy `mvnw.cmd test` trong thư mục BE. Bộ kiểm thử mới bao gồm chấm đáp án, rollback, gửi đồng thời, chống trùng, tách tài khoản, điểm toàn hệ thống và nguồn gốc phản hồi AI.

UI: chạy `npx playwright test --config playwright.explore.config.ts` trong FE. Bộ này dùng Edge và API giả lập để kiểm tra hợp đồng UI; không thay thế nghiệm thu toàn luồng với backend và PostgreSQL đang chạy.

Migration: chạy `BE/scripts/verify-explore-migrations.ps1`. Script kiểm tra trong schema tạm trên PostgreSQL local rồi rollback, không triển khai migration vào bảng ứng dụng.
