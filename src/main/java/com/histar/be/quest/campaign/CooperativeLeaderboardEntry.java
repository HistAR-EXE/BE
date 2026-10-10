package com.histar.be.quest.campaign;
import java.util.UUID;
public record CooperativeLeaderboardEntry(int rank, UUID groupId, String groupName, int members, double completionRatePct, double evidenceCoveragePct, double verifiedOutcomePct, double cooperativeScore) {}
