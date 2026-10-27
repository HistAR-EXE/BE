package com.histar.be.usercreation;

import com.histar.be.common.exception.BusinessRuleException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public record HeritageCreationContext(
        String kind, String siteCode, String stationCode, String roleId, Integer era, String source) {

    private static final Set<String> KINDS = Set.of("original", "heritage");
    private static final Pattern STATION = Pattern.compile("ST0[1-6]");
    private static final Pattern ROLE = Pattern.compile("^st0[1-6]-[a-z-]+-(nam|nu)$");

    public static HeritageCreationContext validate(
            String kind, String siteCode, String stationCode, String roleId, Integer era, String source) {
        String normalizedKind = kind == null || kind.isBlank() ? "original" : kind.trim().toLowerCase(Locale.ROOT);
        if (!KINDS.contains(normalizedKind)) throw new BusinessRuleException("kind không hợp lệ");
        String normalizedStation = stationCode == null || stationCode.isBlank() ? null : stationCode.trim().toUpperCase(Locale.ROOT);
        if (normalizedStation != null && !STATION.matcher(normalizedStation).matches()) throw new BusinessRuleException("stationCode không hợp lệ");
        String normalizedRole = roleId == null || roleId.isBlank() ? null : roleId.trim().toLowerCase(Locale.ROOT);
        if (normalizedRole != null && !ROLE.matcher(normalizedRole).matches()) throw new BusinessRuleException("roleId không hợp lệ");
        if ("original".equals(normalizedKind) && (normalizedRole != null || era != null)) throw new BusinessRuleException("Ảnh gốc không có vai hóa thân hoặc era");
        if (normalizedRole != null && normalizedStation != null && !normalizedRole.startsWith(normalizedStation.toLowerCase(Locale.ROOT) + "-")) throw new BusinessRuleException("roleId không khớp stationCode");
        if ("heritage".equals(normalizedKind) && !Integer.valueOf(1968).equals(era)) throw new BusinessRuleException("heritage phải có era=1968");
        return new HeritageCreationContext(normalizedKind, siteCode, normalizedStation, normalizedRole, era, source);
    }
}
