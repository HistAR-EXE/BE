package com.histar.be.chat.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserProgressAnswerRendererTest {

    private final Map<String, Object> state = Map.of(
            "checkedInStations", List.of("ST01", "ST02"),
            "remainingQuestNames", List.of("Mật thư dưới lòng đất"),
            "unlockedArtifactNames", List.of("Đèn pin quân dụng"));

    @Test void rendersStationCountFromLiveState() {
        assertThat(UserProgressAnswerRenderer.render("Tôi đã hoàn thành bao nhiêu trạm?", state, null))
                .isEqualTo("Bạn đã hoàn thành 2 trạm tại địa điểm này. Các trạm: ST01, ST02.");
    }

    @Test void rendersQuestArtifactAndCheckinWithoutCallingAModel() {
        assertThat(UserProgressAnswerRenderer.render("Tôi còn nhiệm vụ nào chưa làm?", state, null))
                .contains("1 nhiệm vụ", "Mật thư dưới lòng đất");
        assertThat(UserProgressAnswerRenderer.render("Tôi đã mở khóa cổ vật nào?", state, null))
                .contains("1 cổ vật", "Đèn pin quân dụng");
        assertThat(UserProgressAnswerRenderer.render("Tôi check-in ST03 chưa?", state, null))
                .isEqualTo("Bạn chưa check-in ST03.");
    }

    @Test void rewardStateIsNotInferredFromCheckin() {
        assertThat(UserProgressAnswerRenderer.render("Tôi đã nhận thưởng trạm này chưa?", state, "ST02"))
                .contains("không được suy ra", "chưa có dữ liệu live đủ");
    }
}
