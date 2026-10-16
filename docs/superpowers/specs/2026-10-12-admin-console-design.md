# Admin Console và Narrative Pilot Control — Design Spec

## Mục tiêu

Thay các trang quản trị rời rạc bằng một Admin Console độc lập dưới `/admin`. Console quản lý nội dung, narrative pilots, người dùng, tổ chức, billing, analytics, audit và thùng rác. Mọi mutating action phải yêu cầu role `ADMIN`, có audit durable và không tiết lộ raw chat/memory người học.

## Phạm vi

### Điều hướng

`AdminConsoleLayout` là shell dùng chung: sidebar desktop, header hiển thị trạng thái phiên/quyền, vùng nội dung có breadcrumb. Các route là:

```text
/admin
/admin/content
/admin/narrative-pilots
/admin/users
/admin/organizations
/admin/billing
/admin/analytics
/admin/audit
/admin/trash
```

`/admin` hiển thị đúng bốn KPI lấy từ dữ liệu thật: pilot đang `ACTIVE`, draft narrative chờ review, tổng bản ghi trong Thùng rác và báo sai sử liệu được nhận trong 30 ngày. Mỗi ô deep-link tới danh sách với filter tương ứng. Không có KPI/queue “Campaign cần quyết định” hoặc “Quá SLA” trong release này; Console không mở rộng thêm workflow chỉ để phục vụ dashboard. API lỗi phải hiển thị trạng thái lỗi và nút thử lại, không thay bằng số `0`. Các route quản trị cũ được redirect sang Console để bookmark vẫn hoạt động.

Sidebar nhóm mục để tránh chín mục phẳng: **Vận hành** (Nội dung, Narrative pilots, Thùng rác), **Quản lý** (Người dùng, Tổ chức, Billing) và **Giám sát** (Analytics, Audit). Thùng rác có badge tổng bản ghi. Trên mobile, sidebar trở thành drawer đóng sau khi điều hướng.

Header hiển thị role, environment read-only (`DEV`/`STAGING`/`PROD`) và cảnh báo hết hạn phiên. Khi nhận `401` trong lúc nhập form, UI giữ nguyên dữ liệu đã điền và hướng người dùng đăng nhập lại. Khi nhận `403`, UI giữ form và báo rõ tài khoản không đủ quyền; đăng nhập lại không phải là cách xử lý `403`.

Trong release này, các trang **Người dùng**, **Tổ chức**, **Billing** và **Analytics** hiện có chỉ được chuyển vào Console shell và chuẩn hóa điều hướng/guard/trạng thái lỗi; không redesign lại nghiệp vụ hay thay đổi data contract của chúng. Analytics chỉ hiển thị số liệu aggregate; session replay bị loại khỏi Console và endpoint replay cũ không còn trả dữ liệu. Mọi breakdown có mẫu số người dùng phân biệt nhỏ hơn 10 bị ẩn, API trả `suppressed=true` và giá trị nhạy cảm là `null`.

### Nội dung và xóa mềm

Nội dung gồm discovery point, artifact và quest. Các form create/edit đang có được **đưa vào Admin Console shell**, không viết lại contract CRUD. API list mặc định loại bản ghi bị xóa; `includeDeleted=true` chỉ dành cho Console/trash. Xóa là soft delete với `deleted_at`, `deleted_by`, `delete_reason`; restore chỉ xóa trạng thái active trên bản ghi, không xóa lịch sử audit. Nội dung đã publish không có API delete cứng.

Trước soft-delete, API trả impact summary: số đối tượng tham chiếu, quest có pilot active, learner có progress dở dang và hậu quả hiển thị. Xóa bị chặn nếu còn tham chiếu active hoặc pilot active; Admin phải pause/điều chuyển chúng trước. Modal hiển thị impact và reason preset (`Sai nguồn`, `Lỗi kỹ thuật`, `Trùng nội dung`, `Yêu cầu pháp lý`, `Khác`). Preset chuẩn không bắt buộc ghi chú thêm; `Khác` bắt buộc text tự do tối thiểu 10 ký tự. Trash có type, tên, deleted by/reason/date và restore kiểm tra xung đột unique/slug. Nếu bản ghi cha bị xóa mềm, restore trả `409 PARENT_DELETED` và UI yêu cầu khôi phục cha trước. Retention/purge không thuộc release này, nên không có nút purge.

### Narrative Pilot Console

Mỗi pilot gắn quest và location. Danh sách có filter location, `reviewStatus`, `rolloutState`, keyword `q`, sort, page và size; mọi filter/page/sort được đồng bộ URL để bookmark/back hoạt động. Detail chia năm tab: **Readiness**, **Graph**, **Sources & citations**, **Reward & outcomes**, **History**. Không hiển thị raw chat/memory hay breakdown analytics có mẫu số nhỏ.

Review và rollout là hai trạng thái riêng, theo **từng version**:

```text
Review:  DRAFT --approve--> APPROVED
             \--reject---> REJECTED

Rollout: NOT_RELEASED --enable(v1)--> ACTIVE(v1) --pause(reason)--> PAUSED(v1)
                                      |                            |
                                      +--publish(v2)--> ACTIVE(v2) +--resume(v1)
```

Một quest chỉ có một draft mở tại một thời điểm. `POST /drafts` trả `409 DRAFT_ALREADY_EXISTS` kèm draft version hiện tại nếu draft đang tồn tại; UI deep-link đến draft đó. `enable` chỉ thành công khi candidate version có `review_status=APPROVED`, `approved_locales_json` có `vi`, graph hợp lệ, citation source/version/locale/citation đầy đủ và global `narrative_investigation` flag bật. `pause` yêu cầu rollback reason; `resume` chỉ resume đúng published version đang pause khi readiness vẫn pass. Nếu draft mới đã được duyệt trong lúc v1 pause, UI hiển thị hai action rõ ràng: **Resume v1** hoặc **Publish v2**; không tự chọn thay Admin.

`publish-draft` là action duy nhất swap candidate approved sang published graph. Nó hỗ trợ transition `ACTIVE(v1) → ACTIVE(v2)` nguyên tử, để learner đang dùng v1 không bị gián đoạn trong lúc soạn/review v2. Tắt khẩn cấp không làm mất approval. Mọi action ghi durable audit row gồm actor, timestamp, action, from/to state, reason, idempotency key và metadata version.

Không có action nào tự đặt `APPROVED`; review là endpoint hiện có `POST /{questId}/reviews`, yêu cầu `decision`, `approvedLocales` và review note tối thiểu 10 ký tự. Admin Pilot UI gọi endpoint này và hiển thị approved locales/review history.

Graph active không bị thay đổi tại chỗ. `Create draft version` copy graph active thành draft mới; learner tiếp tục dùng published version. Chỉnh sửa chỉ diễn ra trên draft; draft có revision riêng và optimistic lock. Graph tab dùng **structured node editor**: danh sách node, form theo node type, choice transition picker, source/citation table và validation inline. Raw JSON chỉ có read-only export để audit, không có editor JSON trong release này. Publish chỉ swap draft khi version client khớp; nếu admin khác đã thay đổi, UI báo “đã bị thay đổi, tải lại”.

Readiness API nhận `draftVersion` tùy chọn (mặc định candidate version) và trả checklist có cấu trúc cho từng gate, không chỉ `400` text. Detail luôn hiển thị pass/fail, citation/node lỗi neo đúng vị trí, nút Enable/Publish disabled kèm lý do. Global deployment flag tắt hiển thị banner read-only vì browser không được phép đổi flag.

### API

Các endpoint bổ sung, đều `ADMIN`-only:

```text
GET    /api/admin/narrative-investigations?page=&size=&locationId=&reviewStatus=&rolloutState=&q=&sort=
GET    /api/admin/narrative-investigations/{questId}
GET    /api/admin/narrative-investigations/{questId}/readiness?draftVersion=
POST   /api/admin/narrative-investigations/{questId}/reviews
POST   /api/admin/narrative-investigations/{questId}/drafts
PATCH  /api/admin/narrative-investigations/{questId}/drafts/{draftVersion}
POST   /api/admin/narrative-investigations/{questId}/enable
POST   /api/admin/narrative-investigations/{questId}/pause
POST   /api/admin/narrative-investigations/{questId}/resume
POST   /api/admin/narrative-investigations/{questId}/publish-draft
GET    /api/admin/narrative-investigations/{questId}/audit

DELETE /api/admin/discovery-points/{id}
POST   /api/admin/discovery-points/{id}/restore
DELETE /api/admin/artifacts/{id}
POST   /api/admin/artifacts/{id}/restore
DELETE /api/admin/quests/{id}
POST   /api/admin/quests/{id}/restore
GET    /api/admin/trash?page=&size=&type=&q=&sort=
GET    /api/admin/audit?page=&size=&entityType=&actorId=&action=&from=&to=
```

Narrative list accepts `reviewStatus`, `rolloutState`, `q`, `sort`, `page` và `size`, thay vì `status` mơ hồ. Pagination uses the existing page response shape. `DELETE` is semantic soft-delete, never a physical delete. Every mutation includes client `rowVersion`/draft revision to implement optimistic locking and callers send an idempotency key for enable/pause/resume/publish. Pause uses the same reason presets as delete; review requires a note tối thiểu 10 ký tự.

### UI

Console là workspace vận hành **nền sáng hoàn toàn**, tách biệt với giao diện người học: mật độ dữ liệu gọn, card/table responsive, toolbar search/filter và bốn trạng thái loading/data/empty/error rõ ràng. Màu ngữ nghĩa gồm xanh cho active, amber cho review/draft/paused và đỏ cho rejected/destructive; badge luôn có icon và chữ, không truyền đạt trạng thái chỉ bằng màu. Bảng có cột/sort mặc định và sticky header khi cần. Thao tác phá hủy dùng dialog xác nhận và bắt buộc lý do. Focus phải nhìn rõ, dialog/drawer trap focus và hỗ trợ Escape, trả focus về nút mở, toast dùng `aria-live`, hỗ trợ bàn phím và mọi màu phải đạt tương phản cơ bản trên nền sáng. Không thêm UI library hoặc hệ design token mới.

## Dữ liệu

- New `admin_audit_events` table is the sole timeline source for every Admin mutation (content create/update/delete/restore, review, draft, enable/pause/resume/publish), with entity type/id, actor, action, old/new revision/state, reason, metadata JSON and timestamp. Existing `narrative_investigation_reviews` remains the specialist-review detail record and stores `audit_event_id`; timeline reads audit events only, so a review cannot appear twice.
- Add nullable soft-delete columns plus `@Version`/row version to supported content entities and indexed `deleted_at` where required for normal listings.
- Add `narrative_investigation_versions` (one published version plus at most one open draft per investigation), per-version review metadata and row version. Existing graph source metadata remains inside versioned `graph_json`; narrative API validates the candidate version before review/enable/publish.

## Kiểm thử

- Service/unit tests: review/enable/pause/resume/publish gates, readiness check items by requested draft version, one-open-draft invariant, `ACTIVE(v1) → ACTIVE(v2)` swap, optimistic lock conflict, idempotent rollout, soft delete/restore, parent-deleted restore conflict, required rollback reason, audit append and forbidden active-graph mutation.
- Controller integration/security tests: non-admin gets `403`; invalid/unreviewed pilot cannot enable; `PAUSED` pilot resumes only with unchanged approved version; delete impact gate works; all mutations write general audit rows.
- FE tests: AdminRoute guarding, Console navigation, readiness failure explanation, structured graph editor validation, review/enable/pause/publish confirmations, table pagination and URL filters, trash restore/conflict, mobile drawer, `401` versus `403` behavior and API error states.

## Không nằm trong scope

- Không đọc/hiển thị raw chat, memory hoặc dữ liệu nhạy cảm của học sinh.
- Không xóa cứng content đã publish.
- Không tự dịch hoặc tự duyệt narrative content.
- Không thay đổi global feature flags từ browser; flags vẫn thuộc deployment configuration.

## Quyết định

Khung Console áp dụng cho mọi địa điểm. Củ Chi là pilot đầu tiên, không phải nhánh code đặc biệt; mọi action dùng `questId` và `locationId` thay vì hard-code Củ Chi.
