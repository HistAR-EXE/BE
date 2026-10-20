package com.histar.be.pack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.pack.dto.PackManifestResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PackServiceTest {

    private final PackService service = new PackService(
            "https://media.example.com/", Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void liteContainsOnlyPublishedOfflineAssets() {
        PackManifestResponse res = service.build("cu-chi", "lite");
        assertThat(res.tier()).isEqualTo("lite");
        assertThat(res.siteCode()).isEqualTo("cu-chi");
        assertThat(res.generatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        // Hero + five current-day scene images that are published in the Củ Chi media manifest.
        assertThat(res.assets()).hasSize(6);
        assertThat(res.assets().get(0).url()).isEqualTo("https://media.example.com/media/cu-chi/map/hero.jpg");
        assertThat(res.assets()).allMatch(a -> a.required());
        assertThat(res.assets()).noneMatch(a ->
                a.path().contains("/thumbs/") || a.path().endsWith("/video/intro.mp4"));
    }

    @Test
    void fullAddsPublishedPanoramasArtifactAndFaq() {
        PackManifestResponse res = service.build("cu-chi", "FULL");
        assertThat(res.tier()).isEqualTo("full");
        // Lite (6) + five published panoramas + one artifact + offline FAQ.
        assertThat(res.assets()).hasSize(13);
        assertThat(res.assets()).anyMatch(a -> a.path().endsWith("/panoramas/duong-vao.jpg"));
        assertThat(res.assets()).anyMatch(a -> a.path().endsWith("/artifacts/trung-bay-vu-khi.png"));
        assertThat(res.assets()).noneMatch(a ->
                a.path().endsWith("/audio/narration-vi.mp3") || a.path().endsWith("/audio/ambient.mp3"));
    }

    @Test
    void fullIncludesFaqOfflineFromApiBase() {
        PackService withApi = new PackService("https://media.example.com", "https://api.example.com/", Clock.systemUTC());
        PackManifestResponse res = withApi.build("cu-chi", "full");
        assertThat(res.assets()).anyMatch(a ->
                a.url().equals("https://api.example.com/api/sites/cu-chi/pack/faq_offline.json") && !a.required());
        assertThat(service.build("cu-chi", "lite").assets()).noneMatch(a -> a.path().endsWith("faq_offline.json"));
    }

    @Test
    void rejectsBadTierAndSite() {
        assertThatThrownBy(() -> service.build("cu-chi", "huge")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.build("../etc", "lite")).isInstanceOf(BusinessRuleException.class);
    }
}
