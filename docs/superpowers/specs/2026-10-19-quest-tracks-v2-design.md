# Nhiệm vụ v2 — tách Online / Tại di tích — Design Spec

Ngày: 2026-10-19 · Trạng thái: **Giai đoạn 1 và 2 đã làm** (BE + dữ liệu + ghi nhận bước + giao diện mới). Giai đoạn 3 (trình sửa bước cho admin) chưa làm.

## Giai đoạn 3 — ảnh, Hành trình 6 trạm, mini game (đã làm)

Phản hồi kiểm thử: lỗi media trạm, ảnh bước trùng/placeholder, giao diện tối lạc tông, "Hành trình 6 trạm" không chạy, mini game sơ sài.

- **Ảnh thật** (`FE/public/images/cu-chi`, `characters`, `minigames`, nén từ thư mục `Ảnh/`): V85 đặt ảnh bìa + ảnh bước Củ Chi, mỗi ảnh khác nhau trong một nhiệm vụ và khác ảnh bìa. FE `QuestStepList` thêm lưới an toàn: ảnh lặp/SVG thì hiện ô biểu tượng theo loại bước.
- **Hành trình 6 trạm**: chỉ thuộc nhóm `ONSITE`, giao diện sáng, ảnh từng trạm; chương khoá vẫn có "Xem trước trạm". Bước trạm trong 340 có thêm nút "Xem trước trạm".
  Lối vào (sửa sau phản hồi "kiếm không thấy"): panel nằm ngay trên trang Nhiệm vụ tab **Tại di tích** (dưới danh sách), tab **Online** có dải nhắc bấm sang; vẫn còn ở trang chi tiết 340 và tab Trải nghiệm của trang di tích. Link "Đọc chương truyện" trong `StationNextSteps` trước trỏ `/home#story-journey-*` (trang chủ không có panel) → nay trỏ `/quests?...&track=onsite`.
- **Trang trạm** (`/sites/:site/stations/:code/media`): ảnh lớn + bộ ảnh có chú thích, lời giới thiệu (`GET /api/sites/{site}/stations/{code}`), chương truyện, nghe thuyết minh (Web Speech vi-VN), mini-game trạm. Video/soundscape R2 chỉ hiện khi `HEAD` trả tệp thật; chưa có thì ẩn, không báo lỗi. V85 bỏ ghi chú nội bộ ("BQL xác thực…", "nếu trên tuyến") khỏi lời giới thiệu trạm và chương 2.
- **Mini game nhiệm vụ** (`MinigamePage` + `features/minigame/engines/*`): khung chung giới thiệu → chơi → kết quả (sao, XP, dữ kiện có nguồn), nhân vật Anh Ba / Chị Năm, âm thanh WebAudio có nút tắt.
  - Soi nắp: ảnh chụp thật nắp hầm dưới lá (3 góc/mức khó), gợi ý nóng/lạnh, lộ ảnh nắp mở khi tìm thấy.
  - Đường khói: lưới rãnh mặt cắt (bếp → hầm khói → miệng thoát), khói chạy theo đường nối, đánh dấu chỗ rò; sao theo thời gian (`parMs`).
  - Xếp tầng: mặt cắt 3 tầng có độ sâu (~3 m / ~5 m / ~8–10 m, baolaocai.vn); thẻ sai quay về khay.
  - Đào gặp nhau: bản đồ lòng đất, chọn hướng, tiếng cuốc báo trái/phải, gần/xa.
  - Im lặng: lính tuần tra đi trên nắp hầm; micro (3 sao) hoặc giữ nút (2 sao).
  - Đếm lỗ thông hơi (san_co_vat chế độ đếm): cắm dấu trên ảnh rồi chốt số.
- **BE grader** (`QuestMinigameGrader`): lượt chưa xong trả `pending` + `hint` không lộ đáp án — soi_nap `warmth`, dao_gap `direction`/`near`, xep_tang `wrong`/`mistakes` (tối đa 8 lần sai), duong_khoi `wrongCells` (ô thẳng xoay 0≡2, 1≡3; ô không có `rotation` không chấm), san_co_vat điểm nóng `found`/`lastHit`. `MinigameAttemptResponse.hint` mới.
- **Mini-game trạm** (`StationGamePage`): sửa lỗi có sẵn `MinigameResponse.config` là `JsonNode` Jackson 2 nên API trả thuộc tính getter thay cho nội dung → mọi game trạm trống. Nay là `Map`. Giao diện sáng, phản hồi từng câu, nối thuật ngữ bằng chạm, thứ tự bị xáo (trước đây đã đúng sẵn), tự lưu kỷ lục.
- **Kiểm thử**: BE 511 test (510 đạt, 1 bỏ qua có sẵn); V85 chạy trên DB bootstrap/fresh/chain; FE `tsc`, `eslint`, `vite build` sạch; Playwright chơi hết 7 engine + 6 game trạm ở 1280 và 390 px.
- **Gộp "Theo dấu địa đạo" và "Hành trình 6 trạm"** (phản hồi: hai khối nói cùng một việc): nhiệm vụ ONSITE mà mọi bước đều là `station:ST0x`
  không hiện danh sách "Các bước" nữa; `StoryJourneyPanel` nhận `questSteps` + `pointsReward` và trở thành giao diện duy nhất
  (đếm theo *trạm đã check-in* chứ không theo *chương đã mở*, hiện XP, mỗi dòng có nút "Quét QR tại trạm" khi chưa check-in và
  "Mini-game trạm" khi đã check-in). Hết lệch 0/6 bước vs 1/6 chương. Vì bước tính theo thứ tự bất kỳ còn chương mở tuần tự,
  dòng đã check-in nhưng chương còn khoá ghi rõ "Đã check-in trạm này. Nội dung chương mở sau khi bạn check-in trạm ST0x <tên>".
  Nhiệm vụ ONSITE khác (nếu có bước không phải trạm) vẫn giữ danh sách bước + panel như cũ.
- **Khoá mini-game trạm theo QR** (quyết định 19/10): 6 game trạm chỉ mở sau khi người chơi check-in tại chính trạm đó.
  `MinigameService.checkedIn(userId, siteCode, stationCode)` dựa trên `checkins` (không phân biệt hoa thường);
  `MinigameResponse` thêm `unlocked` + `lockReason="CHECKIN"` và **trả `config` rỗng khi còn khoá** để không lộ câu hỏi/đáp án;
  `submit` chặn luôn ở BE (`BusinessRuleException`, 422) nên khoá không chỉ là lớp giao diện.
  FE: trang game hiện thẻ "Chưa mở khoá" (ảnh trạm làm mờ, loại game, điểm thưởng, nút Quét QR / Xem trước trạm);
  thẻ mini-game ở trang trạm đổi sang trạng thái khoá; sau khi quét QR, `StationNextSteps` đưa "Đã mở khoá: mini-game trạm ST0x" lên làm nút chính.
  7 mini-game của nhiệm vụ Online (soi nắp, đường khói, xếp tầng…) **không** bị khoá — đó là nội dung chơi từ xa.
  Test: `StationMinigameLockTest` (3 ca: khách/chưa quét → khoá và config rỗng; quét ST01 chỉ mở ST01; submit bị chặn tới khi quét).
- **V86**: ảnh bìa nhiệm vụ 333 là tệp Củ Chi duy nhất còn trên R2 → đổi sang `/images/cu-chi/loi-xuong-ham.jpg`; nay 8/8 nhiệm vụ Củ Chi dùng ảnh trong `FE/public`.
- **Còn mở**: ảnh hiện vật của 9 di tích khác vẫn là ảnh bìa di tích (FE hiện ô biểu tượng thay vì lặp ảnh); tệp video/soundscape trạm chưa có trên R2.

## Giai đoạn 2 — giao diện (đã làm)

- **Trang Nhiệm vụ** (`pages/QuestsPage.tsx`) dùng `GET /api/v2/quests`:
  - 2 tab **Online** / **Tại di tích**, nhớ trên URL `?track=online|onsite`, mỗi tab đếm số đã xong.
  - Thẻ nhiệm vụ hiện trạng thái do BE tính (Khóa · Chưa nhận · Đang làm · Hoàn thành), lý do khóa, tiến độ "x/y bước bắt buộc", độ khó và số phút.
  - Nút "Nhận nhiệm vụ" ngay trên thẻ.
  - Khi mở trang thì gọi `reconcile` một lần.
- **Trang chi tiết** (`pages/QuestDetailPage.tsx`) dùng `GET /api/v2/quests/{id}`:
  - Khung trạng thái:
    - Khóa: lý do, nút tới nhiệm vụ trước hoặc danh sách điểm cần khám phá.
    - Chưa nhận: báo trước số bước đã có sẵn.
    - Đang làm: thanh tiến độ và nút "Bước tiếp theo".
    - Hoàn thành: ngày hoàn thành.
  - Danh sách bước (`QuestStepList`) theo trạng thái thật của từng bước:
    - nhãn Bắt buộc / Tùy chọn / Premium;
    - "Đã khám phá trước đó";
    - nút mở đúng nơi (`questStepAction`).
  - Hồ sơ điều tra hiện **song song** bên dưới (trước đây thay thế danh sách bước).
  - Giao diện sáng thương hiệu, giống trang danh sách.
- **Xem hiện vật trong trang** (`ArtifactViewerModal`):
  - Bước `artifact` mở khung xem ảnh và câu chuyện hiện vật.
  - Xem ≥ 3 giây thì ghi nhận; chế độ Offline không ghi.
  - BE: `DirectDiscoveryPolicy` chỉ cho ghi thẳng khóa `artifact:` khi nó là bước `artifact` của một nhiệm vụ `ONLINE` tại đúng di tích. Hiện vật sưu tầm Củ Chi vẫn chỉ mở bằng check-in / minigame. Khóa `xuanay:` cũng được ghi thẳng như `scene:`/`era:`.
  - Trước đó 9 nhiệm vụ ở 9 di tích khác vẫn không hoàn thành được vì không có đường nào ghi khóa hiện vật.
- **Sửa lỗi có sẵn**: `UserProgressProvider` nằm ngoài Router, nên màn "Hoàn thành nhiệm vụ" (dùng `useNavigate`) làm sập cả trang mỗi khi một nhiệm vụ hoàn thành. Đã chuyển provider vào trong `BrowserRouter`. Màn chúc mừng thêm nút "Ở lại trang này".
- Xoá `QuestJourneyPanel.tsx`, `heritageQuestSteps.ts` (`HERITAGE_QUEST_META`), `questGate.ts` (tick bước theo vị trí, khóa tính ở FE).
- Kiểm thử Giai đoạn 2: chạy BE thật + FE thật trên Postgres đã migrate. Playwright đi hết luồng:
  - nhận 333, tick theo khóa, bước Premium mở màn nâng cấp;
  - 334 khóa sau 333; 340 hiện tên trạm;
  - 401 xem 2 hiện vật → hoàn thành, không sập trang.
  - `tsc`, `eslint`, `vite build` đều sạch. Toàn bộ test BE: 506, 0 lỗi.

## Mục tiêu

Mỗi nhiệm vụ có **một nguồn định nghĩa duy nhất**, **một bộ luật duy nhất do BE tính**, và FE chỉ hiển thị. Hai nhóm nhiệm vụ **Online** và **Tại di tích** tách hẳn, không chặn nhau.

## Quyết định đã chốt

| # | Câu hỏi | Quyết định |
|---|---|---|
| 1 | Check-in tại di tích | **Tách thành 2 nhóm nhiệm vụ**: `ONLINE` (làm từ xa) và `ONSITE` (phải có mặt). Không nhiệm vụ online nào chứa bước check-in/trạm; hai nhóm không khóa lẫn nhau. |
| 2 | Bước Premium (cổng 1948/1968) | Là **bước tùy chọn**. Người dùng free vẫn hoàn thành nhiệm vụ khi xong các bước bắt buộc. |
| 3 | Khóa đã có trước khi nhận nhiệm vụ | **Có tính**, và hiển thị nhãn "Đã khám phá trước đó". |
| 4 | Hồ sơ điều tra (narrative) | **Chạy song song**: danh sách bước luôn quyết định việc hoàn thành; hồ sơ điều tra là phần riêng, có tiến độ và XP riêng. |
| 5 | Bước Xưa & Nay trong 333 | **Thêm**, bắt buộc: `xuanay:bep-hoang-cam` (bước 3). |
| 6 | Nhiệm vụ Tại di tích | Dùng bảng `stations` ST01–ST06 của `cu-chi`, cả 6 bắt buộc. Tên bước lấy từ `stations.name` lúc chạy, để đội nội dung đổi tên trạm mà không phải sửa nhiệm vụ. |
| 7 | 9 di tích khác | **Xóa bước `era:2026`**. Cổng thời gian của các di tích này hiện màn "Đang được cập nhật". |

## Hiện trạng cần thay (tóm tắt audit 2026-10-18)

- Một nhiệm vụ được định nghĩa ở 4 nơi: `quest_steps` (UI), `quests.step_discovery_keys` (tiến độ/hoàn thành), `quests.steps_total` (mẫu số), `HERITAGE_QUEST_META` ở FE.
- `currentStep` là **số khóa đã có**, FE lại dùng như **vị trí bước** → tick sai bước.
- Khóa mở `required_order` chỉ FE kiểm tra; `unlock_after_quest_id` không ai dùng; BE `start` chỉ kiểm tra `unlock_discovery_keys`.
- Chuỗi chính Củ Chi bị chặn: nhiệm vụ 1 cần Premium 1948 **và** check-in tại chỗ, lại là điều kiện mở nhiệm vụ 2→3.
- `era:XXXX` được ghi ngay khi mở URL có `?questRecord`; `era:2026` **không bao giờ được ghi** → 9 nhiệm vụ ở 9 di tích khác không thể hoàn thành.
- Nhiệm vụ 334/335 chỉ có trong seed tay; seed tay `DELETE FROM quests` (kéo theo xoá tiến độ người dùng) rồi chèn lại.

## Mô hình dữ liệu (V83)

### `quests` — thêm cột

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `track` | `VARCHAR(16) NOT NULL DEFAULT 'ONLINE'` | `CHECK (track IN ('ONLINE','ONSITE'))` |
| `sort_order` | `INT NOT NULL DEFAULT 100` | Thứ tự hiển thị trong cùng `location + track`. Lấy từ `required_order × 10`. |
| `difficulty` | `VARCHAR(16)` | `EASY` / `MEDIUM` / `HARD` (`CHECK`) |
| `estimated_minutes` | `INT` | |

BE **thực thi** `unlock_after_quest_id` và `unlock_discovery_keys` (cả khi xem lẫn khi nhận).

### `quest_steps` — thêm cột

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `is_required` | `BOOLEAN NOT NULL DEFAULT TRUE` | Bước bắt buộc để hoàn thành |
| `is_premium` | `BOOLEAN NOT NULL DEFAULT FALSE` | Ràng buộc `NOT (is_premium AND is_required)` |

- `UNIQUE (quest_id, step_order) DEFERRABLE INITIALLY DEFERRED` (bản ghi trùng thứ tự được đánh số lại trước).
- `action_type` thêm `station` (QR tại một trạm, khóa `station:<CODE>`) và `xuanay` (khóa `xuanay:<điểm>`).
- `xp_partial` để `NULL` ở các bước v2 (là số trang trí, không được cộng).

### Một nguồn duy nhất, cột cũ là cột dẫn xuất

- **Danh sách bước = `quest_steps`.** Luật tính từ đây (`QuestStepDefs`). Nhiệm vụ chưa có dòng nào (ví dụ admin vừa tạo) thì dựng bước từ `step_discovery_keys` + một bước check-in nếu `completion_trigger ≠ discovery` — giữ nguyên hành vi cũ.
- Các cột cũ được giữ và **V84 đồng bộ từ `quest_steps`** để API cũ, nhóm học, phân tích… vẫn đọc đúng:
  - `step_discovery_keys` = khóa khám phá của bước bắt buộc (không gồm `station:`/`checkin`)
  - `steps_total` = số dòng `quest_steps`
  - `completion_trigger` = `checkin` cho `ONSITE`, `discovery` cho `ONLINE`
  - `require_onsite_checkin` = `track = 'ONSITE'`
  - `required_order` = `NULL`
- Xóa các cột cũ ở một migration sau, khi Giai đoạn 2 xong và admin sửa được bước.

## Luật nghiệp vụ (`quest/rules/QuestRules`, thuần Java)

### Bước "xong" khi nào

| `action_type` | Xong khi |
|---|---|
| `tour`, `portal`, `artifact`, `dialogue`, `minigame`, `xuanay`, `reveal`, `briefing` | Có `user_discoveries(user, location, unlock_key)` |
| `station` (khóa `station:<CODE>`) | Có `checkins(user, location, station_code = CODE, presence_method = 'QR')` |
| `checkin` | Có bất kỳ `checkins(user, location)` — chỉ dùng trong nhóm `ONSITE` |

`doneAt` = thời điểm sớm nhất của bản ghi đó. `preDiscovered = doneAt < startedAt` (hoặc nhiệm vụ chưa nhận).

### Trạng thái bước và nhiệm vụ

- Bước: `DONE` · `TODO` · `PREMIUM_LOCKED` (bước `premium`, tài khoản không có Premium).
- Thứ tự bước là **gợi ý**. `nextStepId` = bước bắt buộc đầu tiên chưa xong; hết thì bước tùy chọn đầu tiên làm được.
- Nhiệm vụ: `LOCKED` (chưa thỏa điều kiện mở) · `AVAILABLE` · `IN_PROGRESS` · `COMPLETED` (lưu trong `user_quest_progress`).
- Nhiệm vụ đủ điều kiện hoàn thành khi **có ít nhất một bước bắt buộc và tất cả đã xong**.
- `lock.reason`: `AFTER_QUEST` (kèm `afterQuest {id, title, status}`) · `DISCOVERY_KEYS` (kèm `keys [{key, label, done}]`, nhãn từ `discovery_points.name`).
  `LOCATION_LOCKED` chưa làm — khóa di tích vẫn ở FE trong Giai đoạn 1.

### Nhận và hoàn thành

- `start` kiểm điều kiện mở (`QuestUnlockGate`, lỗi `BusinessRuleException` → 422). Đủ bước bắt buộc từ trước thì hoàn thành ngay trong cùng request.
- Hoàn thành được kiểm lại sau: ghi khám phá, check-in, minigame, start, reconcile. Bỏ logic "cần check-in trước" trong `QuestCompletionServiceImpl`.
- Thưởng không đổi: `+points_reward` một lần, XP khám phá theo khóa như hiện tại.
- Người đang `IN_PROGRESS` mà luật mới làm đủ điều kiện: hoàn thành qua `POST /api/v2/me/quests/reconcile`. Migration **không** tự cộng điểm.

### Hồ sơ điều tra (song song)

Không thêm cờ `hasInvestigation` vào API v2: FE vẫn gọi endpoint điều tra như hiện tại. Tiến độ/XP điều tra giữ nguyên `NarrativeInvestigationService`, **không** ảnh hưởng trạng thái nhiệm vụ.

## API v2 (đã làm, FE chưa dùng)

API cũ giữ nguyên hình dạng (chỉ **thêm** trường). API mới nằm dưới `/api/v2` để FE chuyển dần ở Giai đoạn 2.

| Method | Path | Ghi chú |
|---|---|---|
| `GET` | `/api/v2/quests?locationId=&track=ONLINE\|ONSITE` | Không đăng nhập vẫn xem được (không có tiến độ). Sắp xếp `sort_order, title`. `track` sai → 422. |
| `GET` | `/api/v2/quests/{id}` | `{ quest, steps, rewardSummary }` |
| `POST` | `/api/v2/quests/{id}/start` | `{ detail, completed[] }`; đang khóa → 422 `BUSINESS_RULE` kèm thông báo (cùng mã lỗi API cũ dùng) |
| `POST` | `/api/v2/me/quests/reconcile?locationId=` | `QuestCompletedDto[]`; gọi nhiều lần vẫn an toàn |

`quest` (thẻ): `id, locationId, track, title, description, story, coverImage, sortOrder, difficulty, estimatedMinutes, pointsReward, status, lock, progress {requiredDone, requiredTotal, optionalDone, optionalTotal}, nextStepId, startedAt, completedAt`.

`steps[]`: `id, order, key, actionType, title, objective, description, hint, actionLabel, previewImage, portalEra, minigameId, chatPrompt, required, premium, state, doneAt, preDiscovered`. Bước `station:*` có tiêu đề "Trạm CODE · tên trạm" lấy từ `stations`.

Mỗi lần gọi nạp một lần mỗi loại dữ liệu (nhiệm vụ, bước, khám phá, check-in, tiến độ) — không N+1.

### Tương thích (API cũ)

- `QuestResponse` thêm `track, sortOrder, difficulty, estimatedMinutes`; `QuestStepResponse` thêm `required, premium`.
- `GET /api/me/quests`, `/api/quests/{id}/progress`: giữ cách tính cũ trên cột dẫn xuất (`currentStep` = số khóa bắt buộc đã có, `stepsTotal` = số bước). Nhiệm vụ trạm QR hiện tiến độ 0/6 tới khi xong — chấp nhận tới Giai đoạn 2.

## Ghi nhận bước phía FE (đã làm)

| Khóa | Ghi khi |
|---|---|
| `era:1948`, `era:1968`, `era:2026` | Người dùng **xem hết chương** (tới slide câu đố) trong Dòng thời gian. Bỏ ghi khi chỉ mở URL `?questRecord=era:…`. BE vẫn chặn 1948/1968 khi không Premium. |
| `xuanay:<điểm>` | Trong Xưa & Nay, người dùng **tự kéo thanh trượt** và ở lại điểm đó ≥ 4 giây. |
| `station:<CODE>` | Check-in QR tại trạm (đã có). |

FE Giai đoạn 1 cũng: mở bước `xuanay` vào Xưa & Nay đúng điểm, bước `station` vào màn quét; gắn nhãn "Tùy chọn"; khóa nhiệm vụ theo `unlockAfterQuestId` như BE; bỏ bước Time Portal của 9 di tích khỏi `HERITAGE_QUEST_META`; đường dẫn Cổng thời gian dùng slug (`/time-portal/dia-dao-cu-chi`), di tích chưa có nội dung hiện "Đang được cập nhật".

## Nội dung sau V84

**Củ Chi — ONLINE**

| sort | Nhiệm vụ | Bước bắt buộc | Bước tùy chọn (Premium) | Mở khi |
|---|---|---|---|---|
| 10 | 333 Hành trình dưới lòng đất (HARD, 25′) | Tour · Bếp Hoàng Cầm; Tour · Phòng họp; Xưa & Nay · Bếp Hoàng Cầm | Cổng thời gian · 1948 | Luôn mở |
| 20 | 337 Bếp không khói (EASY, 8′) | Hỏi Chrono; Đường khói | — | Có 2 cảnh tour của 333 |
| 30 | 338 Nắp hầm và cạm bẫy (EASY, 10′) | Hỏi Chrono; Soi nắp; Im lặng | — | Có 2 cảnh tour của 333 |
| 40 | 339 Ba tầng lòng đất (EASY, 8′) | Hỏi Chrono; Xếp tầng | — | Có 2 cảnh tour của 333 |
| 50 | 334 Mật mã Sa bàn Tác chiến (HARD, 20′) | Hỏi Chrono về các tầng; Mảnh sa bàn | Cổng thời gian · 1968 | Hoàn thành 333 |
| 60 | 335 Ánh sáng Đèn Mù U (MEDIUM, 15′) | Hỏi Chrono về bếp; Khu trạm xá trong Nhà sa bàn | Cổng thời gian · 1968 | Hoàn thành 334 |
| 900 | 336 Mật lệnh thử game (chỉ web dev) | 4 minigame | — | — |

- Bỏ bước check-in `checkin:hanh-trinh-duoi-long-dat` khỏi 333.
- Sửa chữ: bỏ "phục kích xe tăng M41" (334), "lớp học dưới đèn mù u" (335); bước trạm xá ghi rõ nằm trong cảnh Nhà sa bàn.
- Bước 334/335 chuyển sang id cố định (`…341–343`, `…351–353`).

**Củ Chi — ONSITE (mới)**

| sort | Nhiệm vụ | Bước bắt buộc |
|---|---|---|
| 10 | 340 Theo dấu địa đạo (MEDIUM, 45′, 150 điểm) | Quét QR tại ST01 … ST06 |

**9 di tích khác (`4444…401`–`409`)**: `ONLINE`, 2 bước hiện vật (trước đây bước chỉ có ở FE), bỏ `era:2026`, mô tả bỏ chữ "Time Portal", độ khó lấy từ `HERITAGE_QUEST_META`, 12 phút.

**Điểm khám phá mới tại Củ Chi**: `xuanay:mieng-ham`, `xuanay:bep-hoang-cam`, `xuanay:phong-hop`, `xuanay:thong-hoi`, `xuanay:den-ham`; bảo đảm có `era:1948/1968/2026`.

## Migration

- `V83__quest_tracks_v2_schema.sql`: cột + ràng buộc ở trên; bước cổng 1948/1968 hiện có → tùy chọn Premium.
- `V84__quest_tracks_v2_content.sql`: nội dung ở trên, upsert theo `(quest_id, unlock_key)`, rồi đồng bộ cột dẫn xuất.
- **Không** xoá nhiệm vụ; **không** sửa `user_quest_progress`, `user_discoveries`, `checkins`.
- Cả hai chạy lại an toàn.
- Seed tay `2026-07-08_quest_steps_init.sql` **vẫn ở trong `seed-manifest.txt`** (V72 cần 334/335 tồn tại khi dựng bootstrap) nhưng đã bỏ `DELETE` và chỉ chèn phần còn thiếu (`ON CONFLICT DO NOTHING`); V84 là nội dung chuẩn.
- Ứng dụng không tự chạy Flyway khi khởi động (Spring Boot 4 không có module `spring-boot-flyway` trong dự án): chạy `mvnw flyway:migrate` hoặc dựng lại `TimeLens_full_bootstrap.sql`.

## Kiểm thử (đã chạy 2026-10-19)

- **Unit** `QuestRulesTest` (8): tick theo khóa, free hoàn thành không cần Premium, bước tùy chọn cho Premium, `preDiscovered`, trạm QR không phân biệt hoa thường, nhiệm vụ cũ giữ bước check-in, nhiệm vụ không có bước bắt buộc không tự hoàn thành.
- **Tích hợp (H2)** `QuestStateServiceTest` (5): sắp xếp và lý do khóa cho khách; khóa có trước được tính, nhận là hoàn thành ngay; BE chặn `AFTER_QUEST` và `DISCOVERY_KEYS`; reconcile idempotent; `track` sai bị từ chối.
- **Dữ liệu** `SeedConsistencyValidator` + `SeedConsistencyTest`: bước Premium phải tùy chọn; nhiệm vụ `ONLINE` không có bước `station`/`checkin`; mỗi nhiệm vụ có ≥ 1 bước bắt buộc; `unlock_after_quest_id` cùng `track`.
- `CuChiTestFixture` (luồng "khám phá rồi check-in") chuyển thành nhiệm vụ `ONSITE` có bước check-in trong `quest_steps`.
- **Toàn bộ test BE**: 502 test, 501 đạt, 1 bỏ qua (có sẵn), 0 lỗi.
- **Migration trên PostgreSQL 16** (bản bootstrap thật + V83/V84):
  - chạy 2 lần không lỗi;
  - `user_quest_progress`, `user_discoveries`, `checkins`, `quest_reward_ledger` không đổi một byte;
  - bootstrap dựng mới với seed đã sửa cho ra đúng cùng nội dung nhiệm vụ;
  - Hibernate `ddl-auto=validate` khớp schema;
  - API v2 trên dữ liệu thật: người dùng kẹt ở 401 và 334 được reconcile hoàn thành.
- **FE**: `tsc` 0 lỗi, `eslint` sạch; ghi `era:*`/`xuanay:*` kiểm bằng Playwright.

## Ngoài phạm vi Giai đoạn 1

- Giao diện mới: trang danh sách hai tab Online / Tại di tích, trang chi tiết dùng API v2, thuật ngữ, theme. Khi đó xoá `HERITAGE_QUEST_META` và cách tick bước theo vị trí.
- Trình sửa bước cho admin (Giai đoạn 3). Admin hiện vẫn sửa các cột cũ; không đổi trong Giai đoạn 1.
- `HeritageOnsiteBonusService` ("Thưởng onsite · Đã đến nơi") giữ nguyên.
- Quest campaigns / LMS / group quests: không đổi (vẫn dựa trên trạng thái `completed`).

## Việc có sẵn phát hiện khi kiểm thử (không thuộc nhiệm vụ v2)

- **Đã sửa:** `V82__seed_demo_segment_accounts.sql` chèn `invite_code` dài 13 ký tự vào cột `VARCHAR(8)`. Nay dùng mã `DEMOSTD1` / `DEMOMIC1`; chuỗi V82 → V83 → V84 chạy được trên bootstrap mới, chạy lại V82 vẫn an toàn.
- **Còn mở:** `SeedConsistencyValidator` báo `discovery_artifact_links` trỏ tới khóa `hotspot:kitchen` không có trong `discovery_points`.
