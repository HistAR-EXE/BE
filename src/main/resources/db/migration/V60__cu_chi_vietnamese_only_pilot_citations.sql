-- The Củ Chi pilot is Vietnamese-only.  This migration deliberately keeps the
-- pilot disabled and in DRAFT until an authorised reviewer records approval.
UPDATE narrative_investigations
SET version = 3,
    enabled = FALSE,
    review_status = 'DRAFT',
    approved_locales_json = '[]',
    reviewed_by = NULL,
    reviewed_at = NULL,
    graph_json = $graph$
{
  "startNodeCode": "tour-360",
  "sources": [
    {
      "sourceId": "dsvh-cu-chi-history-2026-10-12",
      "version": "accessed-2026-10-12",
      "locale": "vi",
      "citation": "Cục Di sản văn hóa, ‘Di tích lịch sử Địa đạo Củ Chi’, đoạn về hình thành khoảng năm 1948 và việc gia cố, mở rộng trong kháng chiến chống Mỹ.",
      "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"
    },
    {
      "sourceId": "dsvh-cu-chi-ben-dinh-2026-10-12",
      "version": "accessed-2026-10-12",
      "locale": "vi",
      "citation": "Cục Di sản văn hóa, ‘Di tích lịch sử Địa đạo Củ Chi’, danh mục thành phần Địa đạo Bến Đình có hầm bí mật (nắp hầm).",
      "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"
    },
    {
      "sourceId": "dsvh-cu-chi-status-2026-10-12",
      "version": "accessed-2026-10-12",
      "locale": "vi",
      "citation": "Cục Di sản văn hóa, ‘Di tích lịch sử Địa đạo Củ Chi’, thông tin xếp hạng di tích quốc gia đặc biệt theo Quyết định 2367/QĐ-TTg ngày 23-12-2015.",
      "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"
    }
  ],
  "nodes": [
    {
      "code": "tour-360",
      "type": "TOUR_360_OBSERVATION",
      "title": "Dấu hiệu trên mặt đất",
      "prompt": "Trong Tour 360°, quan sát nắp hầm và chỉ ghi nhận chi tiết bạn thực sự nhìn thấy. Không kết luận về vật liệu, cách ngụy trang hoặc cơ chế hoạt động nếu tư liệu không xác nhận.",
      "actionHref": "/tour-360",
      "choices": [{"code": "đặt câu hỏi kiểm chứng", "targetNodeCode": "ai-interview"}]
    },
    {
      "code": "ai-interview",
      "type": "AI_INTERVIEW",
      "title": "Hỏi nhân vật AI",
      "prompt": "Hỏi về bối cảnh lịch sử của nắp hầm. AI chỉ được sử dụng các nguồn đã liệt kê, phân biệt rõ fact đã xác minh với diễn giải.",
      "choices": [{"code": "đối chiếu tư liệu", "targetNodeCode": "source-comparison"}]
    },
    {
      "code": "source-comparison",
      "type": "SOURCE_COMPARISON",
      "title": "Đối chiếu tư liệu",
      "prompt": "So sánh quan sát Tour 360° với tư liệu Cục Di sản văn hóa; đánh dấu nội dung nào là quan sát và nội dung nào là fact có citation.",
      "choices": [{"code": "ghi chứng cứ", "targetNodeCode": "evidence-lid"}]
    },
    {
      "code": "evidence-lid",
      "type": "EVIDENCE",
      "title": "Mảnh chứng cứ: nắp hầm",
      "prompt": "Ghi nhận rằng tư liệu chính thức liệt kê hầm bí mật (nắp hầm) tại Bến Đình. Đây không phải hướng dẫn chế tạo, vận hành hay né tránh cạm bẫy.",
      "choices": [
        {"code": "kết luận thăm dò", "targetNodeCode": "outcome-partial"},
        {"code": "kết luận có kiểm chứng", "targetNodeCode": "outcome-verified"}
      ]
    },
    {
      "code": "outcome-partial",
      "type": "OUTCOME",
      "title": "Kết luận thăm dò",
      "minimumEvidence": 0,
      "prompt": "Bạn đã có một nhận định ban đầu; cần thêm chứng cứ trước khi xem đó là kết luận đã kiểm chứng.",
      "choices": []
    },
    {
      "code": "outcome-verified",
      "type": "OUTCOME",
      "title": "Kết luận có kiểm chứng",
      "minimumEvidence": 1,
      "prompt": "Kết luận dựa trên quan sát, chứng cứ đã thu thập và tư liệu chính thức đã trích dẫn.",
      "choices": []
    }
  ],
  "evidenceNodeCodes": ["evidence-lid"],
  "outcomeNodeCodes": ["outcome-partial", "outcome-verified"]
}
$graph$,
    updated_at = NOW()
WHERE quest_id = '33333333-3333-3333-3333-333333333338'::uuid;
