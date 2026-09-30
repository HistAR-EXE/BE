package com.histar.be.checkin.presence;

import java.util.UUID;

/** Presence evidence gathered before the GPS/gamification step. */
public record PresenceInput(String siteCode, String stationCode, boolean qrVerified, UUID clientUuid) {

    public static final PresenceInput NONE = new PresenceInput(null, null, false, null);

    public PresenceInput(String stationCode, boolean qrVerified, UUID clientUuid) {
        this(null, stationCode, qrVerified, clientUuid);
    }
}
