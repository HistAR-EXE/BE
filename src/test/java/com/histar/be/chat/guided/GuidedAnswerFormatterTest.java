package com.histar.be.chat.guided;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.chat.dto.ChatSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuidedAnswerFormatterTest {

    private final GuidedAnswerFormatter formatter = new GuidedAnswerFormatter();

    @Test
    void marks_a_cited_answer_as_verified_fact() {
        GuidedAnswer response = formatter.format(
                "Địa đạo được mở rộng trong thời kỳ kháng chiến.",
                List.of(new ChatSource("Tư liệu Ban quản lý", "Trích dẫn", null)),
                ChatMode.DEEP_EXPLAIN);

        assertThat(response.blocks()).singleElement().satisfies(block -> {
            assertThat(block.type()).isEqualTo(GuidedAnswerType.VERIFIED_FACT);
            assertThat(block.confidence()).isEqualTo(GuidedConfidence.VERIFIED);
            assertThat(block.sources()).isNotEmpty();
        });
    }

    @Test
    void never_marks_an_uncited_answer_as_verified() {
        GuidedAnswer response = formatter.format("Mình chưa có đủ tư liệu để khẳng định điều này.", List.of(), ChatMode.DEEP_EXPLAIN);

        assertThat(response.blocks()).singleElement().satisfies(block -> {
            assertThat(block.type()).isEqualTo(GuidedAnswerType.INTERPRETATION);
            assertThat(block.confidence()).isEqualTo(GuidedConfidence.CAUTION);
        });
    }
}
