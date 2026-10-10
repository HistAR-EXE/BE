package com.histar.be.chat.guided;

import java.util.List;

public record GuidedAnswer(List<GuidedAnswerBlock> blocks, String followUpQuestion) {}
