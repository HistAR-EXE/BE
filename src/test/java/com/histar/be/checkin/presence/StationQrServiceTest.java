package com.histar.be.checkin.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.histar.be.checkin.presence.StationQrService.SignedStationQr;
import com.histar.be.checkin.presence.StationQrService.VerifiedStationQr;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.PresenceProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StationQrServiceTest {

    private static final String SECRET = "histar-test-secret";

    private StationQrService service;

    @BeforeEach
    void setUp() {
        PresenceProperties properties = new PresenceProperties();
        properties.setQrHmacSecret(SECRET);
        service = new StationQrService(
                properties, Clock.fixed(Instant.parse("2026-10-24T09:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void staticPayloadRoundTripsAndDoesNotEmbedTheSecret() {
        SignedStationQr qr = service.issueStatic("Cu-Chi", "st01");

        assertThat(qr.staticQr()).isTrue();
        assertThat(qr.signed()).isTrue();
        assertThat(qr.timestamp()).isZero();
        assertThat(qr.siteCode()).isEqualTo("cu-chi");
        assertThat(qr.stationCode()).isEqualTo("ST01");
        assertThat(qr.signature()).matches("[0-9a-f]{64}");
        assertThat(qr.payload()).isEqualTo("cu-chi:ST01:0:" + qr.signature());
        assertThat(qr.payload()).doesNotContain(SECRET);
        assertThat(service.verifyDetailed(qr.payload())).isEqualTo(new VerifiedStationQr("cu-chi", "ST01"));
        assertThat(service.verify(qr.payload())).isEqualTo("ST01");
    }

    @Test
    void dynamicAndLegacyPayloadsStayCompatible() {
        SignedStationQr dynamic = service.issue("imperial-city", "ST02");
        assertThat(dynamic.staticQr()).isFalse();
        assertThat(dynamic.timestamp()).isEqualTo(Instant.parse("2026-10-24T09:00:00Z").getEpochSecond());
        assertThat(service.verifyDetailed(dynamic.payload()))
                .isEqualTo(new VerifiedStationQr("imperial-city", "ST02"));

        SignedStationQr legacyClock = service.issue("ST03");
        assertThat(legacyClock.siteCode()).isEqualTo(StationQrService.DEFAULT_SITE);
        assertThat(service.verify(legacyClock.payload())).isEqualTo("ST03");

        assertThatThrownBy(() -> service.verifyDetailed("cu-chi:ST01:0:deadbeef"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
