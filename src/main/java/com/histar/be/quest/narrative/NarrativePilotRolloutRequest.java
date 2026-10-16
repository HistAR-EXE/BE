package com.histar.be.quest.narrative;

public record NarrativePilotRolloutRequest(
        Integer version,
        Long rowVersion,
        String reason,
        String reasonPreset,
        String reasonDetail) {
    public String resolvedReason(String fallback) {
        if (reasonDetail != null && !reasonDetail.isBlank()) return reasonDetail.trim();
        if (reason != null && !reason.isBlank()) return reason.trim();
        if (reasonPreset != null && !reasonPreset.isBlank()) return reasonPreset.trim();
        return fallback;
    }

    public String requiredPauseReason() {
        if (reasonPreset == null || reasonPreset.isBlank()) {
            throw new com.histar.be.common.exception.BusinessRuleException("Pause reason preset is required");
        }
        String preset = reasonPreset.trim();
        java.util.Set<String> allowed = java.util.Set.of(
                "Sai nguồn", "Lỗi kỹ thuật", "Trùng nội dung", "Yêu cầu pháp lý", "Khác");
        if (!allowed.contains(preset)) {
            throw new com.histar.be.common.exception.BusinessRuleException("Pause reason preset is invalid");
        }
        String detail = reasonDetail == null ? "" : reasonDetail.trim();
        if ("Khác".equals(preset) && detail.length() < 10) {
            throw new com.histar.be.common.exception.BusinessRuleException(
                    "Custom pause reason must be at least 10 characters");
        }
        return detail.isEmpty() ? preset : preset + ": " + detail;
    }
}
