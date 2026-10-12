# Gói duyệt nội dung — Pilot Củ Chi: “Nắp hầm và cạm bẫy”

**Trạng thái hệ thống:** `DRAFT`. Tài liệu này chuẩn bị bằng chứng và biểu mẫu; nó **không** là phê duyệt, không thay thế chuyên gia nội dung, và không được dùng để đổi graph sang `APPROVED`.

## Phạm vi cần duyệt

| Hạng mục | Điều cần xác nhận | Người ký |
| --- | --- | --- |
| Fact | Mỗi fact trong node/evidence/outcome khớp nguồn được duyệt, không suy diễn quá nguồn | Chuyên gia lịch sử/di sản |
| Citation | `sourceId`, version và locale tồn tại; khối `VERIFIED_FACT` trỏ đến source tương ứng | Chuyên gia + biên tập |
| Wording | Không lãng mạn hoá bạo lực/cạm bẫy; không hướng dẫn tái tạo hay vận hành cơ chế nguy hiểm | Chuyên gia + safety reviewer |
| Reward | XP/chapter/completion được mô tả rõ, không đổi reward khi retry | Product owner |
| Locale | Pilot hiện chỉ phát hành `vi`. Bất kỳ locale mới nào phải qua một review mới; không dùng bản AI dịch làm citation. | Reviewer tiếng Việt / reviewer locale mới |

## Nguồn nền tảng đã xác minh sơ bộ

1. [Danh mục di tích xếp hạng — Sở Văn hóa và Thể thao TP.HCM](https://svhtt.hochiminhcity.gov.vn/documents/10184/265325/Danh%2Bs%C3%A1ch%2Bdi%2Bt%C3%ADch%2B%C4%91%C3%A3%2BQ%C4%90%2Bx%E1%BA%BFp%2Bh%E1%BA%A1ng%2B%C4%91%E1%BA%BFn%2Bth%C3%A1ng%2B5%2B2017.pdf/948c1ef9-4d46-43b6-af76-c1e616dcf54d): xác nhận Di tích lịch sử Địa đạo Củ Chi và quyết định xếp hạng được liệt kê.
2. [Hồ sơ kiểm kê di tích TP.HCM — Sở Văn hóa và Thể thao TP.HCM](https://svhtt.hochiminhcity.gov.vn/documents/10184/256372/Danh%2Bmuc%2Bkiem%2Bke%2Bdi%2Btich%2Blich%2Bsu%2Bvan%2Bhoa%2Btren%2Bdia%2Bban%2BTPHCM%2Bgiai%2Bdoan%2B2016-2020-ph.pdf/539ee8c6-974e-4c90-a41f-bd2a18682eef): mô tả phát triển từ hầm bí mật tới mạng địa đạo và có thông tin kích thước hầm/nắp trong bối cảnh kiểm kê.

Các nguồn này **không tự động phê duyệt** mọi chi tiết hiện có trong graph. Reviewer phải đối chiếu từng `citationId`/version trước khi ký.

## Đối chiếu nguồn nhà nước (10-10-2026)

Các nguồn sau đủ thẩm quyền để làm **nguồn nền**, không phải là giấy phép tự động chuyển graph sang `APPROVED`:

| Phát biểu có thể dùng, với wording an toàn | Nguồn chính thống | Giới hạn bắt buộc |
| --- | --- | --- |
| Địa đạo Củ Chi là di tích quốc gia đặc biệt, theo Quyết định 2367/QĐ-TTg ngày 23-12-2015. | [Danh mục di tích quốc gia đặc biệt — Cổng Di sản văn hóa](https://dsvh.gov.vn/danh-muc-di-tich-quoc-gia-dac-biet-1752), [danh sách xếp hạng — Sở VH&TT TP.HCM](https://svhtt.hochiminhcity.gov.vn/tin-chi-tiet/-/chi-tiet/danh-sach-cac-cong-trinh-%C4%91ia-%C4%91iem-%C4%91a-%C4%91uoc-quyet-%C4%91inh-xep-hang-di-tich-tren-dia-ban-thanh-pho-ho-chi-minh-%C4%91en-thang-6-2016--19761-1.html) | Dùng cho phần giới thiệu/metadata; không suy ra chi tiết của từng node Tour 360° từ quyết định xếp hạng. |
| Hệ thống bắt đầu hình thành trong kháng chiến chống Pháp, ban đầu có các đoạn ngắn phục vụ cất giấu tài liệu và trú ém; sau đó được gia cố, mở rộng trong kháng chiến chống Mỹ. | [Hồ sơ di tích — Cổng Di sản văn hóa](https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490) | Không tự thêm mốc, kích thước hoặc công năng không có trong source/version đã gắn với node. |
| Khu Bến Đình có hạng mục hầm bí mật (nắp hầm) trong danh mục thành phần di tích. | [Hồ sơ di tích — Cổng Di sản văn hóa](https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490) | Chỉ mô tả quan sát/ý nghĩa lịch sử; cấm mô tả cách chế tạo, vận hành, né tránh hoặc tái tạo cạm bẫy. |

### Nhật ký xác minh nguồn công khai (11-10-2026)

Đợt rà soát này chỉ xác minh **nguồn nền công khai của cơ quan nhà nước**; không phải là phê duyệt nội dung pilot. Reviewer phải lưu URL, ngày truy cập và phiên bản tư liệu vào workflow trước khi ký.

| ID bằng chứng | Nguồn | Có thể hỗ trợ | Không được suy ra |
| --- | --- | --- | --- |
| `DSVH-CUCHI-1490` | [Cổng Di sản Văn hóa: Địa đạo Củ Chi](https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490) | Lịch sử hình thành, phạm vi di tích và sự hiện diện của hầm bí mật/nắp hầm ở khu Bến Đình. | Vật liệu ngụy trang, kích thước, cơ chế hoạt động hoặc xác nhận một khung hình Tour 360° cụ thể nếu nguồn không nói rõ. |
| `HCMC-SVHTT-2016` | [Sở Văn hóa và Thể thao TP.HCM: danh sách di tích](https://svhtt.hochiminhcity.gov.vn/web/guest/tin-chi-tiet/-/chi-tiet/danh-sach-cac-cong-trinh-%C4%91ia-%C4%91iem-%C4%91a-%C4%91uoc-quyet-%C4%91inh-xep-hang-di-tich-tren-%C4%91ia-ban-thanh-pho-ho-chi-minh-%C4%91en-ngay-20-4-2016--20935-1.html) | Trạng thái xếp hạng và Quyết định 2367/QĐ-TTg của di tích. | Chi tiết của node, ending, câu đố, reward hoặc bất kỳ hướng dẫn về cạm bẫy. |

**Danh sách chặn trước duyệt:** bất kỳ copy nào khẳng định nắp hầm được che bằng lá/đất/cành, mô tả cấu tạo/cách vận hành cạm bẫy, hoặc gắn khung Tour 360° với Bến Đình mà không có asset ID và nguồn lưu trữ tương ứng phải bị đổi thành `INTERPRETATION` hoặc bị loại khỏi graph. Pilot này chỉ phát hành tiếng Việt; mọi locale mới đều cần review mới trước khi được phát hành.

### Ma trận duyệt trước pilot

| Hạng mục | Bằng chứng hiện có | Việc reviewer còn phải xác nhận |
| --- | --- | --- |
| `tour-360` / nắp hầm | Source nền xác nhận hạng mục hầm bí mật (nắp hầm). | Ảnh/điểm quan sát Tour 360° có đúng Bến Đình hay không; wording không suy diễn. |
| `ai-interview` | Chỉ được bám vào source/version trong graph. | Prompt, câu trả lời mẫu, citation placement và boundary `VERIFIED_FACT` / `INTERPRETATION`. |
| `source-comparison` | Có nguồn chính thống cho fact nền. | Từng mệnh đề so sánh, source ID, phiên bản và URL còn truy cập được. |
| Evidence/outcome/reward | Không có nguồn nhà nước nào quyết định cơ chế XP. | Product owner duyệt reward, threshold, thông điệp hai ending và xác nhận retry không cấp lặp. |
| `vi` | Source nền tiếng Việt của cơ quan nhà nước. | Biên tập viên/reviewer tiếng Việt ký wording đã duyệt; không dùng AI dịch làm citation. |

## Checklist phát hành

- [ ] Reviewer đã đối chiếu từng node `TOUR_360_OBSERVATION`, `AI_INTERVIEW`, `SOURCE_COMPARISON` và outcome với source/version.
- [ ] Không có câu nào mô tả thao tác làm hoặc né tránh cạm bẫy; copy chỉ phục vụ quan sát và lý giải lịch sử.
- [ ] Mọi `VERIFIED_FACT` tiếng Việt có citation, URL và ngày truy cập đã duyệt; không tự dịch sang locale khác.
- [ ] Ending đầy đủ bằng chứng có threshold rõ và cả hai ending đều đã duyệt copy/reward.
- [ ] Reward ledger được kiểm thử idempotency trước khi mở pilot.
- [ ] Reviewer ký tên, ngày duyệt, source versions và locale coverage trong workflow `POST /api/admin/narrative-investigations/{questId}/reviews`.

## Quyết định

| Reviewer | Vai trò | Ngày | Quyết định | Ghi chú |
| --- | --- | --- | --- | --- |
| _chưa chỉ định_ | Chuyên gia lịch sử/di sản |  | PENDING |  |
| _chưa chỉ định_ | Biên tập/safety |  | PENDING |  |
| _chưa chỉ định_ | Product owner |  | PENDING |  |

Chỉ sau khi mọi dòng bắt buộc được ký qua workflow và validation backend trả hợp lệ mới được đổi `review_status` sang `APPROVED` và bật cờ pilot.
