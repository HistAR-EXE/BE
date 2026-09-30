package com.histar.be.referral.service;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.referral.dto.CreatorPublicStats;
import com.histar.be.referral.dto.ReferralLandingResponse;
import com.histar.be.referral.dto.ReferralStatsItem;
import com.histar.be.referral.entity.ReferralCode;
import com.histar.be.referral.entity.ReferralVisit;
import com.histar.be.referral.repository.ReferralCodeRepository;
import com.histar.be.referral.repository.ReferralVisitRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralCodeRepository codeRepository;
    private final ReferralVisitRepository visitRepository;

    @Transactional(readOnly = true)
    public ReferralLandingResponse landing(String code) {
        ReferralCode rc = requireActive(code);
        return new ReferralLandingResponse(rc.getCode(), rc.getCreatorName(), rc.getHeadline());
    }

    /** Records a landing visit; repeated calls for the same (code, session) are ignored. */
    @Transactional
    public boolean recordVisit(String code, UUID userId, String sessionId) {
        ReferralCode rc = requireActive(code);
        String session = sessionId == null || sessionId.isBlank() ? null : sessionId.trim();
        if (session != null && visitRepository.existsByReferralCodeAndSessionId(rc.getCode(), session)) {
            return false;
        }
        visitRepository.save(ReferralVisit.builder()
                .referralCode(rc.getCode())
                .userId(userId)
                .sessionId(session)
                .visitedAt(Instant.now())
                .build());
        return true;
    }

    @Transactional(readOnly = true)
    public CreatorPublicStats publicStats(String code, int days) {
        ReferralCode rc = requireActive(code);
        Instant since = Instant.now().minus(Math.max(1, days), ChronoUnit.DAYS);
        long visits = 0;
        long uniqueUsers = 0;
        for (Object[] row : visitRepository.aggregateSince(since)) {
            if (rc.getCode().equalsIgnoreCase((String) row[0])) {
                visits = ((Number) row[1]).longValue();
                uniqueUsers = ((Number) row[2]).longValue();
                break;
            }
        }
        return new CreatorPublicStats(rc.getCode(), rc.getCreatorName(), visits, uniqueUsers);
    }

    @Transactional(readOnly = true)
    public List<ReferralStatsItem> stats(int days) {
        Instant since = Instant.now().minus(Math.max(1, days), ChronoUnit.DAYS);
        Map<String, Object[]> agg = new HashMap<>();
        for (Object[] row : visitRepository.aggregateSince(since)) {
            agg.put((String) row[0], row);
        }
        return codeRepository.findAll().stream()
                .map(rc -> {
                    Object[] row = agg.get(rc.getCode());
                    return new ReferralStatsItem(
                            rc.getCode(),
                            rc.getCreatorName(),
                            rc.isActive(),
                            row == null ? 0 : ((Number) row[1]).longValue(),
                            row == null ? 0 : ((Number) row[2]).longValue(),
                            row == null ? 0 : ((Number) row[3]).longValue(),
                            row == null ? null : (Instant) row[4]);
                })
                .sorted((a, b) -> Long.compare(b.visits(), a.visits()))
                .toList();
    }

    private ReferralCode requireActive(String code) {
        return codeRepository
                .findByCodeIgnoreCase(code == null ? "" : code.trim())
                .filter(ReferralCode::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Referral code not found"));
    }
}
