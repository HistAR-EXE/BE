package com.histar.be.siteworkspace.dto;

public record SiteStationQrResponse(
        String siteCode,
        String stationCode,
        long timestamp,
        String signature,
        String payload,
        boolean signed,
        boolean staticQr) {}
