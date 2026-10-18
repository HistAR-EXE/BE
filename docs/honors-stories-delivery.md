# Câu chuyện hành trình và vinh danh đa dạng

Triển khai bộ 1, 2, 3 đã được người dùng chọn, giữ thiết kế xanh–vàng hiện tại và toàn bộ thay đổi UI đang có trong workspace.

## Nội dung

1. **Hành trình làm nên bạn:** cột mốc huy hiệu toàn hệ thống, thử thách Củ Chi và tổng XP. Ngày hiển thị lấy từ DB. Cột mốc hoàn thành trọn bộ lấy ngày muộn nhất của các bằng chứng cần thiết; nếu bất kỳ bằng chứng nào thiếu ngày thì không tự suy diễn. Mốc XP hiện tại không được gán ngày của baseline/import. Danh sách tối đa 60 cột mốc, hiển thị ban đầu 3 và cho mở rộng bằng bàn phím.
2. **Muôn cách để tỏa sáng:** bốn tiêu chí độc lập: mật thư Củ Chi đã giải đúng, địa điểm có khám phá được ghi nhận, bộ thử thách Củ Chi đã hoàn thành đầy đủ, huy hiệu khác nhau đã đạt. Tiêu chí và phạm vi được giải thích ngay trên thẻ. Mỗi thẻ có tối đa 3 người tiêu biểu và thành tích/vị trí của người xem; cùng kết quả cùng hạng (1, 1, 3), thứ tự UUID chỉ để ổn định hiển thị. Không cộng thêm XP cho danh hiệu hiển thị.
3. **Dấu ấn tiếp theo:** chọn bộ chưa hoàn thành có tỷ lệ hoàn thành cao nhất; đồng mức ưu tiên thứ tự hành trình. Chỉ dùng danh mục key đã định nghĩa phía BE. Liên kết mở đúng tầng, mật thư, hồ sơ hoặc chứng cứ còn thiếu. Mở liên kết không gửi đáp án hay tự ghi nhận chứng cứ. Khi hoàn thành hết, chuyển sang lời ghi nhận và xem lại.

## API và dữ liệu

`GET /api/leaderboard/stories?scope=all&groupId=...` trả `userId`, `timeline`, `paths`, `spotlights`, `updatedAt`. Free và Premium cùng quy tắc. Xác thực, kiểm tra thành viên nhóm và phạm vi trường/thành phố dùng cùng quy tắc với bảng chính. Không nhận userId tùy ý từ client. Phản hồi `private, no-store`.

Repository chỉ đọc, transaction `REPEATABLE_READ`; không sửa XP/ledger, không trao huy hiệu, không cần migration mới. Tổng hợp cộng đồng dùng một truy vấn có tham số, giới hạn kết quả. Giữ nguyên cách loại ADMIN và seed Analytics/bootstrap của bảng xếp hạng. Timeline thuộc riêng người đang đăng nhập; dữ liệu cộng đồng không trả email hay thông tin đăng nhập.

FE tải stories độc lập với bảng chính và Explore summary. Phản hồi không đúng chủ tài khoản hoặc hợp đồng dữ liệu bị từ chối; URL chỉ được điều hướng vào Explore/Profile. Owner + scope + revision chặn phản hồi cũ sau đổi tài khoản/phạm vi. Lỗi stories có thông báo/thử lại, không biến thành lịch sử rỗng và không che bảng chính. Tôn trọng chế độ giảm chuyển động.

## Kiểm chứng

- BE toàn bộ: **440 tests, 0 failures/errors/skips** (`mvnw.cmd test`), gồm 11 tests mới về stories.
- PostgreSQL: `node scripts/test-stories-postgres.cjs` kiểm tra SQL thực trên bảng tạm rollback: ties 1/1/3, top3, vị trí cá nhân, lọc tài khoản mẫu/admin, phạm vi nhóm, XP không đổi.
- API local: `node scripts/test-honors-live.cjs` đạt 7 nhóm kiểm tra; Free/Premium, quyền truy cập, owner/no-store, 4 paths/4 spotlights, mốc XP không có ngày giả, đọc lặp không trao huy hiệu/đổi XP. Kiểm tra dương trên QA còn xác nhận ngày hoàn thành trọn bộ lấy đúng bằng chứng PostgreSQL và huy hiệu đã đạt hiển thị đúng. QA tự dọn và fingerprint dữ liệu gốc giữ nguyên. Payload stories thực được lưu trong target để kiểm tra FE parser; không lưu credentials.
- Explore: **15/15 UI tests đạt**, gồm mở đúng tầng/mật thư, đóng mật thư không tự mở lại, hồ sơ quay về đúng khu lưu trữ, và các hồi quy bảo toàn dữ liệu/chống phản hồi cũ.
- UI vinh danh **21/21 đạt**, bao gồm payload thật từ PostgreSQL/API đi qua bộ kiểm tra dữ liệu FE, các ngày thật/thiếu ngày, mục tiêu gần hoàn thành/đã hoàn thành hết, lỗi và retry, owner khác, URL không an toàn, mobile nhiều tên dài, bàn phím và giảm chuyển động.
- FE build đạt; lint **0 lỗi, 13 cảnh báo có sẵn** ngoài các phần mới.

Chạy từ FE: `npx playwright test -c playwright.leaderboard.config.ts`; test hợp đồng PostgreSQL chỉ chạy khi file target từ lượt API local ở trên có sẵn. Các ảnh kiểm tra nằm trong `FE/test-results/`.

## Trải nghiệm và triển khai

Mở `http://localhost:5173/leaderboard`, tải lại để nhận UI mới. BE local 8080 đã được khởi động với schema validate và Flyway hiện có. Giữ 18 hồ sơ có nhãn “Mẫu UI” trong DB local từ lượt trước; không thêm hồ sơ mẫu lên production và không thay đổi dữ liệu thật khi kiểm thử.

Chưa commit/push/merge/deploy bộ thay đổi này. Khi triển khai phải cập nhật BE có endpoint stories trước hoặc cùng FE. Kiểm thử local không thay thế kiểm chứng môi trường production.
