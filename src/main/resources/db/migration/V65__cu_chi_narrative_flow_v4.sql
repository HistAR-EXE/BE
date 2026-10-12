-- Rich Vietnamese-only narrative flow. It remains disabled/DRAFT until reviewed in Admin Console.
UPDATE narrative_investigations
SET version = 4,
    enabled = FALSE,
    review_status = 'DRAFT',
    approved_locales_json = '[]',
    reviewed_by = NULL,
    reviewed_at = NULL,
    updated_at = NOW(),
    graph_json = $graph$
{
  "startNodeCode": "tour-360",
  "sources": [
    {
      "sourceId": "dsvh-cu-chi-history-2026-10-12",
      "version": "accessed-2026-10-12",
      "locale": "vi",
      "citation": "Cục Di sản văn hóa, Di tích lịch sử Địa đạo Củ Chi: thông tin hình thành, cấu trúc và giá trị di tích.",
      "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"
    },
    {
      "sourceId": "dsvh-cu-chi-ben-dinh-2026-10-12",
      "version": "accessed-2026-10-12",
      "locale": "vi",
      "citation": "Cục Di sản văn hóa, Di tích lịch sử Địa đạo Củ Chi: hạng mục hầm bí mật/nắp hầm tại Bến Đình.",
      "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"
    }
  ],
  "nodes": [
    {
      "code": "tour-360",
      "type": "TOUR_360_OBSERVATION",
      "title": "Truy tìm dấu hiệu trên mặt đất",
      "prompt": "Bạn là điều tra viên hiện trường. Mở Tour 360° và ghi lại đúng những gì mắt thấy quanh miệng hầm. Không đoán vật liệu hay cơ chế nếu chưa có nguồn xác nhận.",
      "actionHref": "/tour-360",
      "factLabel": "NHIỆM VỤ QUAN SÁT",
      "observationChecklist": ["Xác định vị trí miệng/nắp hầm trong khung hình", "Ghi một chi tiết nhìn thấy rõ", "Phân biệt quan sát trực tiếp với suy đoán"],
      "choices": [{"code": "Tôi đã ghi nhận dấu hiệu", "targetNodeCode": "ai-interview"}]
    },
    {
      "code": "ai-interview",
      "type": "AI_INTERVIEW",
      "title": "Phỏng vấn Chrono",
      "prompt": "Đặt một câu hỏi kiểm chứng cho Chrono về bối cảnh của nắp hầm. Câu trả lời chỉ là giả thuyết dẫn đường; hãy mở nguồn để xác nhận.",
      "factLabel": "CÂU HỎI KIỂM CHỨNG",
      "observationChecklist": ["Hỏi một điều có thể đối chiếu bằng nguồn", "Đánh dấu phần Chrono nói là fact hay diễn giải", "Không yêu cầu hướng dẫn chế tạo hoặc né tránh cạm bẫy"],
      "choices": [{"code": "Mở hồ sơ nguồn", "targetNodeCode": "source-comparison"}]
    },
    {
      "code": "source-comparison",
      "type": "SOURCE_COMPARISON",
      "title": "Đối chiếu tư liệu chính thống",
      "prompt": "So sánh ghi chép hiện trường với nguồn Cục Di sản văn hóa. Chỉ đánh dấu chứng cứ khi nguồn nói rõ điều đó.",
      "factLabel": "FACT CÓ CITATION",
      "sourceIds": ["dsvh-cu-chi-history-2026-10-12", "dsvh-cu-chi-ben-dinh-2026-10-12"],
      "observationChecklist": ["Mở ít nhất một source", "Tìm câu nói về hầm bí mật/nắp hầm", "Ghi lại URL nguồn trong hồ sơ"],
      "choices": [{"code": "Ghi mảnh bối cảnh", "targetNodeCode": "evidence-context"}]
    },
    {
      "code": "evidence-context",
      "type": "EVIDENCE",
      "title": "Mảnh chứng cứ: bối cảnh địa đạo",
      "prompt": "Bạn đã đối chiếu được bối cảnh của hầm và nắp hầm với nguồn chính thống. Ghi mảnh này vào hồ sơ, rồi hoàn thành Soi nắp để kiểm chứng dấu hiệu quan sát.",
      "factLabel": "BỐI CẢNH ĐÃ ĐỐI CHIẾU",
      "sourceIds": ["dsvh-cu-chi-history-2026-10-12", "dsvh-cu-chi-ben-dinh-2026-10-12"],
      "evidenceKey": "tunnel-context",
      "rewardXp": 10,
      "choices": [{"code": "Mở thử thách Soi nắp", "targetNodeCode": "evidence-lid"}]
    },
    {
      "code": "evidence-lid",
      "type": "EVIDENCE",
      "title": "Mảnh chứng cứ: nắp hầm",
      "prompt": "Hoàn thành Soi nắp để chứng minh bạn đã quan sát đúng dấu hiệu. Sau đó quay lại đây để ghi nhận mảnh chứng cứ.",
      "factLabel": "ĐÃ ĐỐI CHIẾU",
      "sourceIds": ["dsvh-cu-chi-ben-dinh-2026-10-12"],
      "evidenceKey": "lid-observation",
      "linkedMinigameId": "a1000001-0000-4000-8000-000000000012",
      "rewardXp": 10,
      "choices": [{"code": "Kết luận thăm dò", "targetNodeCode": "outcome-partial"}, {"code": "Kết luận hồ sơ", "targetNodeCode": "outcome-verified"}]
    },
    {
      "code": "outcome-partial",
      "type": "OUTCOME",
      "title": "Kết luận thăm dò",
      "minimumEvidence": 0,
      "prompt": "Bạn đã hình thành giả thuyết ban đầu. Hãy quay lại Tour 360° hoặc mở nguồn để củng cố hồ sơ.",
      "factLabel": "CHƯA ĐỦ CHỨNG CỨ",
      "choices": []
    },
    {
      "code": "outcome-verified",
      "type": "OUTCOME",
      "title": "Kết luận có kiểm chứng",
      "minimumEvidence": 2,
      "prompt": "Bạn đã nối được quan sát hiện trường, bối cảnh tư liệu, mini-game và citation chính thống thành một kết luận có kiểm chứng.",
      "factLabel": "HỒ SƠ ĐÃ XÁC MINH",
      "rewardXp": 20,
      "choices": []
    }
  ],
  "evidenceNodeCodes": ["evidence-context", "evidence-lid"],
  "outcomeNodeCodes": ["outcome-partial", "outcome-verified"]
}
$graph$
WHERE quest_id = '33333333-3333-3333-3333-333333333338'::uuid;
