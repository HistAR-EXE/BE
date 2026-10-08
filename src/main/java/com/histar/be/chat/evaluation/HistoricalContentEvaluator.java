package com.histar.be.chat.evaluation;

import com.histar.be.chat.guided.GuidedAnswerBlock;
import com.histar.be.chat.guided.GuidedAnswerType;
import com.histar.be.chat.guided.GuidedConfidence;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Deterministic publish gate; it complements, but never delegates truth to, an LLM judge. */
@Component
public class HistoricalContentEvaluator {

    public static final Set<String> REQUIRED_LOCALES = Set.of("vi", "en", "ko", "zh-CN");

    public HistoricalEvaluation evaluate(List<GuidedAnswerBlock> blocks, Set<String> approvedLocales) {
        List<String> reasons = new ArrayList<>();
        for (GuidedAnswerBlock block : blocks == null ? List.<GuidedAnswerBlock>of() : blocks) {
            if (block.type() == GuidedAnswerType.VERIFIED_FACT && (block.sources() == null || block.sources().isEmpty())) {
                reasons.add("VERIFIED_FACT requires at least one citation.");
            }
            if (block.type() == GuidedAnswerType.VERIFIED_FACT && block.confidence() != GuidedConfidence.VERIFIED) {
                reasons.add("VERIFIED_FACT must use VERIFIED confidence.");
            }
        }
        Set<String> locales = approvedLocales == null ? Set.of() : approvedLocales;
        REQUIRED_LOCALES.stream().filter(locale -> !locales.contains(locale)).sorted()
                .forEach(locale -> reasons.add("Missing approved locale: " + locale));
        return new HistoricalEvaluation(reasons.isEmpty(), List.copyOf(reasons));
    }
}
