# Hộ chiếu, cộng đồng và Top 10 — 17/10/2026

Đã triển khai trên local, chưa commit/push/deploy trong lượt này.

## Quy tắc đã thống nhất

- Chỉ XP quyết định hạng (1, 1, 3). Khi bằng XP, số huy hiệu khác nhau chỉ sắp thứ tự hiển thị; nếu tiếp tục bằng nhau dùng UUID ổn định. Free/Premium cùng quy tắc.
- Bảng chính tối đa 10 người, kể cả khi đồng XP tại ranh giới. Có giải thích số người đồng XP chưa hiển thị. Vị trí cá nhân và nhóm người đồng hành vẫn trả về độc lập.
- Hộ chiếu là ảnh lưu niệm cá nhân, không phải chứng nhận. Tạo PNG 1200×1500 trên thiết bị, lấy lại thành tích từ server trước khi tạo. Cho ẩn tên/XP/cấp độ/huy hiệu; tối đa ba huy hiệu Củ Chi đã đạt. Đổi lựa chọn sẽ hủy ảnh đã chuẩn bị.
- Chia sẻ cần thao tác người dùng; trình duyệt không hỗ trợ thì tải PNG. Hủy chia sẻ không báo thành công. API không xuất email/avatar hoặc cho đọc hộ chiếu tài khoản khác.
- Cộng đồng đếm nhà thám hiểm có hoạt động, thử thách Củ Chi đã hoàn thành, cặp người/địa điểm và cặp người/huy hiệu. Loại tài khoản admin và dữ liệu mẫu khỏi số liệu chính thức. Bộ thử thách Củ Chi hiện có 18 khóa.
- Giữ 18 hồ sơ mẫu local có nhãn để quan sát giao diện. Không thêm dữ liệu mẫu production.

## Bằng chứng kiểm thử

- BE: 36 bài kiểm thử liên quan, 0 lỗi/thất bại; bốn lớp LeaderboardServiceTest, LeaderboardReadRepositoryTest, HonorsPassportCommunityTest và HonorsStoriesTest.
- PostgreSQL: truy vấn thực tế được chạy trên bảng tạm trong transaction rồi rollback; xác minh Top 10, hạng XP, thứ tự huy hiệu, ranh giới đồng hạng, người ngoài Top 10, huy hiệu chưa biết ngày và phạm vi nhóm.
- API local: tám nhóm kiểm tra bằng tài khoản QA tạm, có kiểm tra Free/Premium, xác thực, no-store, quyền sở hữu, thống kê, đọc lặp. Sau dọn QA, fingerprint profiles/XP/huy hiệu/discovery/ledger/attempts khớp trước chạy.
- FE: toàn bộ 26 bài kiểm thử ban đầu qua; thêm hai bài qua về hộ chiếu mobile/hủy chia sẻ và hủy ảnh khi thay quyền riêng tư. Các chỉnh sửa trình bày sau đó được kiểm tra lại có mục tiêu.
- Lint: 0 lỗi, 13 cảnh báo có sẵn ngoài phần mới. Build production và TypeScript qua; còn cảnh báo kích thước bundle chung.

## Trải nghiệm lại

1. Mở `http://localhost:5173/leaderboard`, tải lại trang khi đã đăng nhập.
2. Quan sát Top 10, vị trí cá nhân và các hàng nội dung hai cột; thu hẹp màn hình để xem bố cục xếp dọc.
3. Bấm **Tạo hộ chiếu của tôi**, chọn thông tin và câu tuyên ngôn, rồi **Tạo ảnh từ thành tích mới nhất**.
4. Xem ảnh, bấm **Tải ảnh PNG** hoặc **Chia sẻ ảnh**; thử ẩn tên và tạo lại.
5. Mở **Cách ghi nhận** trong khối cộng đồng để xem định nghĩa từng chỉ số. Số liệu chính thức không bao gồm các tài khoản có nhãn Mẫu UI.

Ảnh kiểm thử nằm trong `FE/test-results/`: `honors-complete-desktop.png`, `honors-stories-mobile.png`, `honors-passport-mobile.png`, `honors-passport-export.png`. Các ảnh UI dùng dữ liệu kiểm thử, không giả làm số liệu production.

Phạm vi bằng chứng của lượt này là local và các thành phần thay đổi; không phải xác nhận toàn hệ thống production hoặc kiểm thử mọi thiết bị. Không cần migration mới cho hai API này.
