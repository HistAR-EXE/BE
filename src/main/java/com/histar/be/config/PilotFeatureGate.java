package com.histar.be.config;

import com.histar.be.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** One server-side gate for pilot-only capabilities. UI flags are never trusted. */
@Component
@RequiredArgsConstructor
public class PilotFeatureGate {
    private final PilotFeatureProperties features;

    public void requireTeacherAiDrafts() { require(features.isTeacherAiDrafts(), "Teacher AI drafts chưa được bật cho cohort này."); }
    public void requireNarrativeInvestigation() { require(features.isNarrativeInvestigation(), "Narrative investigation chưa được bật cho cohort này."); }
    public void requireQuestCampaigns() { require(features.isQuestCampaigns(), "Quest campaigns chưa được bật cho cohort này."); }

    private static void require(boolean enabled, String message) {
        if (!enabled) throw new BusinessRuleException(message);
    }
}
