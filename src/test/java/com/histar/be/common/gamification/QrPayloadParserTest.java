package com.histar.be.common.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class QrPayloadParserTest {

  private static final UUID LOC = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Test
  void parse_locationPrefix() {
    QrPayload payload = QrPayloadParser.parse("timelens:location:" + LOC);
    assertEquals(QrPayloadType.LOCATION, payload.type());
    assertEquals(LOC, payload.locationId());
  }

  @Test
  void parse_secretPrefix() {
    QrPayload payload = QrPayloadParser.parse("timelens:secret:" + LOC);
    assertEquals(QrPayloadType.SECRET, payload.type());
    assertEquals(LOC, payload.locationId());
  }

  @Test
  void parse_rawUuid() {
    QrPayload payload = QrPayloadParser.parse(LOC.toString());
    assertEquals(QrPayloadType.LOCATION, payload.type());
    assertEquals(LOC, payload.locationId());
  }
}
