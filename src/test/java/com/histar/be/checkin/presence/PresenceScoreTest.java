package com.histar.be.checkin.presence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.PresenceProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PresenceScoreTest {

    @Test
    void scoring_combinations() {
        assertEquals(0, PresenceScore.compute(false, false, false));
        assertEquals(60, PresenceScore.compute(true, false, false));
        assertEquals(30, PresenceScore.compute(false, true, false));
        assertEquals(90, PresenceScore.compute(true, true, false));
        assertEquals(100, PresenceScore.compute(true, true, true));
    }

    @Test
    void sequence_requiresDirectSuccessor() {
        assertTrue(PresenceScore.isSequential(2, 3));
        assertFalse(PresenceScore.isSequential(2, 4));
        assertFalse(PresenceScore.isSequential(3, 3));
        assertFalse(PresenceScore.isSequential(null, 1));
        assertFalse(PresenceScore.isSequential(1, null));
    }

    @Test
    void sortOrder_fromTrailingDigits() {
        assertEquals(3, PresenceScore.sortOrderOf("ST-03"));
        assertEquals(12, PresenceScore.sortOrderOf("station12"));
        assertNull(PresenceScore.sortOrderOf("gate"));
        assertNull(PresenceScore.sortOrderOf(null));
    }

    @Test
    void method_prefersQrThenGps() {
        assertEquals(PresenceMethod.QR, PresenceScore.methodFor(true, true));
        assertEquals(PresenceMethod.GPS, PresenceScore.methodFor(false, true));
        assertEquals(PresenceMethod.MANUAL, PresenceScore.methodFor(false, false));
    }

    @Test
    void qr_signedRoundTrip_andSkewRejected() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        PresenceProperties props = new PresenceProperties();
        props.setQrHmacSecret("test-secret");
        StationQrService issuer = new StationQrService(props, Clock.fixed(now, ZoneOffset.UTC));

        var token = issuer.issue("ST-01");
        assertTrue(token.signed());
        assertEquals("ST-01", issuer.verify(token.payload()));

        StationQrService later =
                new StationQrService(props, Clock.fixed(now.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));
        assertThrows(BusinessRuleException.class, () -> later.verify(token.payload()));

        StationQrService withinSkew =
                new StationQrService(props, Clock.fixed(now.plus(Duration.ofMinutes(14)), ZoneOffset.UTC));
        assertEquals("ST-01", withinSkew.verify(token.payload()));
    }

    @Test
    void qr_badSignatureOrMissingSignature_rejectedWhenSecretSet() {
        PresenceProperties props = new PresenceProperties();
        props.setQrHmacSecret("test-secret");
        StationQrService service = new StationQrService(props);
        long ts = Instant.now().getEpochSecond();

        assertThrows(BusinessRuleException.class, () -> service.verify("ST-01:" + ts + ":deadbeef"));
        assertThrows(BusinessRuleException.class, () -> service.verify("ST-01:" + ts));
        // Tampered station code with a signature issued for another station.
        String valid = service.issue("ST-01").payload();
        String sig = valid.substring(valid.lastIndexOf(':') + 1);
        assertThrows(BusinessRuleException.class, () -> service.verify("ST-02:" + ts + ":" + sig));
    }

    @Test
    void qr_unsignedAccepted_whenSecretBlank() {
        StationQrService service = new StationQrService(new PresenceProperties());
        assertEquals("ST-07", service.verify("ST-07:1"));
        assertFalse(service.issue("ST-07").signed());
    }
}
