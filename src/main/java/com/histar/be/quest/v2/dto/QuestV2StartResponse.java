package com.histar.be.quest.v2.dto;

import com.histar.be.gamification.dto.QuestCompletedDto;
import java.util.List;

/** Kết quả nhận nhiệm vụ. {@code completed} khác rỗng khi đã đủ bước bắt buộc từ trước. */
public record QuestV2StartResponse(QuestV2Detail detail, List<QuestCompletedDto> completed) {}
