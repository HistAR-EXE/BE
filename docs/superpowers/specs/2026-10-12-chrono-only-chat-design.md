# Chrono-only Chat — Design Spec

## Mục tiêu

Thay các persona lịch sử trong luồng chat mới bằng **Chrono — Trợ lý TimeLens** mà không xóa character hoặc hội thoại cũ.

## Quyết định

- `characters` có thêm cờ `chat_enabled`, mặc định `false` để record cũ không vô tình xuất hiện trong chat mới.
- Migration V62 giữ nguyên mọi record legacy và thêm một record Chrono cho mỗi `locations` hiện có. Đây là một persona logic duy nhất, được tạo một record theo location vì API/chat cần `characterId` gắn với location.
- V62 đánh dấu mọi character legacy `chat_enabled=false`; chỉ Chrono được bật. Không sửa `conversations` hoặc `messages`, nên lịch sử cũ và foreign key vẫn nguyên vẹn.
- `GET /api/characters/by-location/{locationId}` chỉ truy vấn character có `chat_enabled=true`; endpoint `GET /api/characters/{id}` không đổi để record lịch sử vẫn có thể được tra cứu nội bộ.
- FE chỉ dùng persona key `chrono`. Query parameter persona cũ vẫn được chấp nhận nhưng được chuẩn hóa về Chrono để deep-link cũ không hỏng.
- Các nhãn TTS/voice dùng tên Chrono; không còn wording “nhân vật lịch sử”.
- Bootstrap full được tạo lại từ migration directory nên tự gồm V62. Wrapper UTF-8 hiện dùng `docker cp`, không được pipe SQL qua PowerShell.

## Không nằm trong scope

- Không xóa hoặc rewrite conversation/message cũ.
- Không reset bất kỳ database hiện có trong quá trình triển khai code.
- Không đổi fact/RAG policy hay narrative graph.

## Kiểm thử

- BE service test chứng minh danh sách chat chỉ đọc record enabled, còn `findById` vẫn dùng repository bình thường.
- Migration/bootstrap structural test chứng minh generated SQL có V62.
- FE test helper chứng minh route persona legacy/default đều resolve Chrono; build kiểm tra toàn bộ TypeScript.
