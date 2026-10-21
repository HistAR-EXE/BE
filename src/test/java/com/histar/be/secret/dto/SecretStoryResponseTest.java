package com.histar.be.secret.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecretStoryResponseTest {

    @Test
    void lockedResponseExplainsTheRealUnlockCondition() {
        var response = new SecretStoryResponse(
                true,
                "Câu chuyện bí mật",
                null,
                "COMPLETE_LOCATION_QUEST",
                "Hoàn thành ít nhất một nhiệm vụ tại địa điểm để mở câu chuyện bí mật.");

        assertThat(response.lockReason()).isEqualTo("COMPLETE_LOCATION_QUEST");
        assertThat(response.unlockCondition()).contains("Hoàn thành ít nhất một nhiệm vụ");
    }
}
