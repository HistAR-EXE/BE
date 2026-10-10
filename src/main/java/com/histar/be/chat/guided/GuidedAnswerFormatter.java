package com.histar.be.chat.guided;

import com.histar.be.chat.dto.ChatSource;
import java.util.List;
import org.springframework.stereotype.Component;

/** Makes the evidence state explicit; a response without citations cannot be represented as verified. */
@Component
public class GuidedAnswerFormatter {

    public GuidedAnswer format(String reply, List<ChatSource> sources, ChatMode mode) {
        List<ChatSource> safeSources = sources == null ? List.of() : List.copyOf(sources);
        boolean verified = !safeSources.isEmpty();
        GuidedAnswerBlock block = new GuidedAnswerBlock(
                verified ? GuidedAnswerType.VERIFIED_FACT : GuidedAnswerType.INTERPRETATION,
                verified ? GuidedConfidence.VERIFIED : GuidedConfidence.CAUTION,
                reply == null ? "" : reply,
                safeSources);
        return new GuidedAnswer(List.of(block), followUp(mode, verified));
    }

    private String followUp(ChatMode mode, boolean verified) {
        if (!verified) {
            return "Bạn muốn khám phá thêm điểm nào tại di tích này?";
        }
        return switch (mode) {
            case LIGHT_HINT -> "Bạn muốn mình gợi ý một điểm kế tiếp để khám phá không?";
            case KNOWLEDGE_CHECK -> "Bạn thử tóm tắt điều quan trọng nhất vừa khám phá được nhé?";
            case DEEP_EXPLAIN -> "Bạn muốn tìm hiểu sâu hơn về nguyên nhân hay ý nghĩa của sự kiện này?";
        };
    }
}
