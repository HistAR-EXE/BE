package com.histar.be.chat.guided;

import com.histar.be.chat.dto.ChatSource;
import java.util.List;

public record GuidedAnswerBlock(
        GuidedAnswerType type, GuidedConfidence confidence, String content, List<ChatSource> sources) {}
