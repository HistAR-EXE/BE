package com.histar.be.siteworkspace.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SiteContentDraftRequestJsonTest {

    @Test
    void springJacksonCanDeserializeObjectSnapshot() throws Exception {
        String json = """
                {"snapshot":{"name":"Di tích E2E","description":"Mô tả"},"expectedRevision":0}
                """;

        SiteContentDraftRequest request = JsonMapper.builder().build().readValue(json, SiteContentDraftRequest.class);

        assertThat(request.expectedRevision()).isZero();
        assertThat(request.snapshot()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) request.snapshot()).get("name")).isEqualTo("Di tích E2E");
    }
}
