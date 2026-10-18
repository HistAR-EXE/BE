# Bảng vinh danh toàn hành trình — 17/10/2026

## Thiết kế đã thống nhất

Kết hợp A (đại sảnh vinh danh) và C (dấu ấn cá nhân), nền sáng xanh dương–vàng đồng bộ Khám phá. Tiêu đề: “Những bước chân bền bỉ. Một hành trình đáng tự hào.” Mọi tài khoản đã đăng nhập, Free/Premium, dùng cùng quyền xem và quy tắc xếp hạng. Không có bảng tuần/tháng, đặt lại XP, giải thưởng tiền hoặc XP tặng thêm khi xem.

- Xếp theo tổng XP toàn hệ thống. Cùng XP cùng hạng (1, 1, 3); UUID chỉ ổn định thứ tự hiển thị giữa người đồng hạng.
- Top 3, danh sách tối đa 100 (mặc định cấu hình 50), vị trí cá nhân ngoài top, tối đa 5 vị trí lân cận.
- Chỉ tài khoản có XP dương tham gia thứ hạng. Người chưa có XP vẫn xem được và thấy số dư thật cùng lời động viên.
- Loại ADMIN; Analytics mẫu được nhận diện đồng thời bằng UUID vùng seed và email Analytics. Bootstrap mẫu cũ được nhận diện bằng đúng email mẫu user1–8/demo_user_1–8 và credential mẫu cố định đã công bố trong SQL seed. Không loại tài khoản chỉ vì trùng tên/email tương tự. Không xóa hay sửa tài khoản nào.
- Giữ API phạm vi nhóm/trường/thành phố cho các luồng đang có; nhóm kiểm tra membership, trường lấy org của người xem, thành phố dùng tham số bind. Giao diện chính ưu tiên cộng đồng và nhóm khi truy cập từ Group Hub.
- Cột mốc và huy hiệu cá nhân ghi rõ phạm vi Củ Chi; tổng XP, cấp và tổng huy hiệu trên thẻ cá nhân thuộc toàn hệ thống.

## API và tính toàn vẹn

`GET /api/leaderboard`: yêu cầu đăng nhập, không yêu cầu Premium. Trả `entries`, `viewer`, `nearby`, `totalParticipants`, `updatedAt`; giữ trường cũ `scope`, `city`, `viewerRankLocked=false`. `rank=null` khi người xem không có hạng trong phạm vi. `scope=week` trả 422. Nhóm không có quyền trả 403; chưa đăng nhập trả 401.

`GET /api/me/explore/summary`: endpoint mới chỉ đọc dữ liệu đã xác nhận. Không gọi `lockAccount` hoặc `evaluateAndAward`. Endpoint Khám phá cũ giữ hành vi hiện tại. Cả hai API cá nhân đặt `Cache-Control: private, no-store`.

BE dùng transaction chỉ đọc với repeatable-read; không thêm schema/migration, không sửa XP/ledger hay backfill localStorage. FE tách tải cộng đồng/cá nhân, bỏ phản hồi cũ khi đổi account/phạm vi, kiểm tra cấu trúc dữ liệu, có lỗi/thử lại riêng và modal dùng bàn phím.

## Kiểm chứng

- Backend: bộ đầy đủ 429 tests đạt, bao gồm 14 ExploreRewardIntegrationTest và hồi quy chỉ đọc không trao huy hiệu. Sau khi bổ sung nhận diện bootstrap mẫu cũ, chạy lại các test service/repository có liên quan và đối chiếu PostgreSQL. Test hồi quy đã tái hiện lỗi cũ: badges từ 0 thành 1 khi gọi snapshot ghi nhận, rồi đạt khi dùng readSnapshot.
- PostgreSQL: SQL thực của repository được kiểm tra qua bảng tạm trong transaction rollback — hòa điểm, phạm vi top/lân cận, viewer ngoài top, lọc admin/seed, đếm huy hiệu và giữ nguyên XP đều đạt.
- API thật trên local: 5 nhóm kiểm tra đạt — đăng nhập Free/Premium, hạng ngang nhau, dữ liệu cá nhân/no-store/không trả trường email, 401/422/403, đọc lặp không trao huy hiệu hay đổi ledger/XP, dọn fixture và đối chiếu dữ liệu gốc. Kết quả máy đọc: `target/honors-live-results.json`.
- UI: 11 kiểm thử bảng vinh danh và 12 hồi quy Khám phá đạt, gồm Free, dữ liệu giả trong storage, viewer ngoài top, lỗi/empty, hòa điểm, modal bàn phím, 390px không tràn ngang, phản hồi sai tài khoản/thiếu hợp đồng, API cá nhân chậm và đổi tài khoản khi request cũ đang chờ.
- FE build đạt; lint 0 errors, 13 warnings có sẵn ngoài phần thay đổi.
- Rà soát độc lập đã sửa cả hai vấn đề tích hợp: tải cá nhân chặn cộng đồng, snapshot trao huy hiệu khi xem. Không còn vấn đề quan trọng được phát hiện trong lượt review cuối.

Chạy lại từ BE: `./mvnw.cmd test`, `node scripts/test-honors-postgres.cjs`, `node scripts/test-honors-live.cjs` (hai script cuối chỉ dùng Docker/BE local, không trỏ production). Từ FE: `npm run build`, `npm run lint`, `npx playwright test -c playwright.leaderboard.config.ts`, `npx playwright test -c playwright.explore.config.ts`.

## Tinh chỉnh UI và dữ liệu xem thử trên local

- Hộp nguyên tắc căn giữa bằng native dialog, chia header/body cuộn/footer để nút đóng và tiếp tục luôn tiếp cận được trên mobile. Thẻ cá nhân nằm trong luồng bố cục để không đè lên thẻ huy hiệu khi cuộn.
- Tăng chữ, khoảng cách và độ nổi bật của tiêu đề/thẻ; avatar có bảng màu nhất quán, bục vinh danh có ánh sáng vàng, mascot chuyển động nhẹ, hiệu ứng xuất hiện và hover. Tôn trọng `prefers-reduced-motion`.
- `node scripts/preview-honors-data.cjs create` trong BE tạo 18 hồ sơ chỉ dùng local, có nhãn `Mẫu UI`, XP và huy hiệu khác nhau, tên dài và trường hợp đồng hạng. Các hồ sơ không có mật khẩu/OAuth identity và dùng email `example.invalid`. Không dùng migration để phát tán dữ liệu mẫu lên production.
- Script kiểm tra Docker endpoint local, sao lưu DB đầy đủ vào `.secrets/db-backups`, lưu danh sách UUID mẫu trong `.secrets/honors-ui-preview.json`. Tạo lặp không nhân bản hồ sơ. Chạy `status` để xem số lượng, `remove` để dọn đúng các UUID mẫu đã ghi nhận.
- Đã thực chạy create → remove → create thành công; fingerprint XP/cấp độ và toàn bộ discoveries, badges, ledger, attempts của tài khoản gốc không đổi. Giữ lại 18 hồ sơ mẫu để người dùng trải nghiệm; tài khoản thật 735 XP vẫn giữ nguyên dữ liệu.
- Bổ sung hồi quy căn giữa modal, thẻ không chồng lên huy hiệu, cỡ chữ và chế độ giảm chuyển động, nhiều tên dài trên mobile và footer modal không vượt màn hình.
- Lượt xác minh cuối sau tinh chỉnh: 14/14 kiểm thử UI đạt; `npm run build` đạt; lint 0 lỗi, 13 cảnh báo cũ. Ảnh đối chiếu: `FE/test-results/leaderboard-desktop.png`, `leaderboard-many-mobile.png`, `leaderboard-rules-mobile.png` (ảnh kiểm thử dùng API fixture; bộ 18 mẫu trong DB được kiểm tra thêm trên trình duyệt đăng nhập thật).

## Trạng thái triển khai

Thay đổi ở hai nhánh local `feat/lifetime-honors`. Triển khai BE trước hoặc đồng thời FE vì FE cần các trường phản hồi mới và API `/summary`. Nền DB phải có các migration Explore V74–V75 từ lần tích hợp trước; tính năng này không cần migration mới. Không tự bật Flyway/repair hay chỉnh DB production để bỏ qua lỗi.

Chưa push/merge/deploy thay đổi này. Lỗi production trước đó vẫn cần người có quyền Render xác minh lịch sử migration và log; kiểm thử local không thay thế xác minh production.
