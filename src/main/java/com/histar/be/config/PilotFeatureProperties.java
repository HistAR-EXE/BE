package com.histar.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Release switches: all pilot capabilities remain disabled until staging gates pass. */
@ConfigurationProperties(prefix = "histar.pilot")
public class PilotFeatureProperties {
    private boolean chatStreaming;
    private boolean guidedModes;
    private boolean journeyMemory;
    private boolean teacherAiDrafts;
    private boolean narrativeInvestigation;
    private boolean questCampaigns;

    public boolean isChatStreaming() { return chatStreaming; }
    public void setChatStreaming(boolean chatStreaming) { this.chatStreaming = chatStreaming; }
    public boolean isGuidedModes() { return guidedModes; }
    public void setGuidedModes(boolean guidedModes) { this.guidedModes = guidedModes; }
    public boolean isJourneyMemory() { return journeyMemory; }
    public void setJourneyMemory(boolean journeyMemory) { this.journeyMemory = journeyMemory; }
    public boolean isTeacherAiDrafts() { return teacherAiDrafts; }
    public void setTeacherAiDrafts(boolean teacherAiDrafts) { this.teacherAiDrafts = teacherAiDrafts; }
    public boolean isNarrativeInvestigation() { return narrativeInvestigation; }
    public void setNarrativeInvestigation(boolean narrativeInvestigation) { this.narrativeInvestigation = narrativeInvestigation; }
    public boolean isQuestCampaigns() { return questCampaigns; }
    public void setQuestCampaigns(boolean questCampaigns) { this.questCampaigns = questCampaigns; }
}
