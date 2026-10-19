package com.histar.be.quest.v2.dto;

import com.histar.be.quest.dto.QuestRewardSummary;
import java.util.List;

/** Chi tiết nhiệm vụ: thẻ + các bước + phần thưởng có thật. */
public record QuestV2Detail(QuestV2Card quest, List<QuestV2Step> steps, QuestRewardSummary rewardSummary) {}
