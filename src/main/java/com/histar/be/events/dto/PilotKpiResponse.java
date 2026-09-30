package com.histar.be.events.dto;

import java.util.Map;

/**
 * Pilot KPIs for the last 7 days. {@code byEventType} has a count for every supported pilot event type
 * (session_start, session_end, station_arrived, station_completed, export_created, pack_loaded,
 * checkin_result, nps_submitted, landing_visit, paywall_shown, purchase_success, share_initiated, ...).
 */
public record PilotKpiResponse(
        long sessionStart,
        long stationArrived,
        long shareInitiated,
        long checkinsLast7Days,
        long sessionEnd,
        long stationCompleted,
        long exportCreated,
        long packLoaded,
        long checkinResult,
        long npsSubmitted,
        long landingVisit,
        long paywallShown,
        long purchaseSuccess,
        Map<String, Long> byEventType) {}