package com.histar.be.quest.dto;

import com.histar.be.gamification.dto.QuestCompletedDto;
import java.util.List;

/** Kết quả nhận nhiệm vụ: tiến độ sau khi nhận và các nhiệm vụ vừa hoàn thành (nếu đã đủ bước từ trước). */
public record QuestStartOutcome(QuestProgressResponse progress, List<QuestCompletedDto> completed) {}
