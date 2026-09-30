package com.histar.be.squad.service;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.squad.dto.CreateSquadRequest;
import com.histar.be.squad.dto.SquadCreatedResponse;
import com.histar.be.squad.dto.SquadMeResponse;
import com.histar.be.squad.dto.SquadMemberStateResponse;
import com.histar.be.squad.entity.Squad;
import com.histar.be.squad.entity.SquadMember;
import com.histar.be.squad.repository.SquadMemberRepository;
import com.histar.be.squad.repository.SquadRepository;
import com.histar.be.squad.sync.SquadLiveStateService;
import com.histar.be.squad.sync.SquadMemberLiveState;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SquadService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SquadRepository squadRepository;
    private final SquadMemberRepository squadMemberRepository;
    private final ProfileRepository profileRepository;
    private final SquadLiveStateService liveStateService;
    private final SquadService self;

    public SquadService(
            SquadRepository squadRepository,
            SquadMemberRepository squadMemberRepository,
            ProfileRepository profileRepository,
            SquadLiveStateService liveStateService,
            @Lazy SquadService self) {
        this.squadRepository = squadRepository;
        this.squadMemberRepository = squadMemberRepository;
        this.profileRepository = profileRepository;
        this.liveStateService = liveStateService;
        this.self = self;
    }

    public SquadCreatedResponse createSquad(UUID userId, CreateSquadRequest request) {
        profileRepository.findById(userId).orElseThrow(() -> new AuthException("Unauthorized"));
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                return self.createSquadOnce(userId, request);
            } catch (DataIntegrityViolationException ex) {
                // retry unique code
            }
        }
        throw new BusinessRuleException("Không thể tạo mã tiểu đội, vui lòng thử lại");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SquadCreatedResponse createSquadOnce(UUID userId, CreateSquadRequest request) {
        String siteCode = normalizeSiteCode(request == null ? null : request.siteCode());
        Instant now = Instant.now();
        String code = generateUniqueCode(6);
        Squad squad = Squad.builder()
                .code(code)
                .siteCode(siteCode)
                .leaderUserId(userId)
                .createdAt(now)
                .build();
        squadRepository.save(squad);
        joinInternal(squad, userId, now);
        return new SquadCreatedResponse(squad.getId(), squad.getCode(), squad.getSiteCode(), squad.getLeaderUserId(), squad.getCreatedAt(), 1);
    }

    @Transactional
    public SquadCreatedResponse joinSquad(UUID userId, String codeRaw) {
        profileRepository.findById(userId).orElseThrow(() -> new AuthException("Unauthorized"));
        String code = codeRaw == null ? "" : codeRaw.trim().toUpperCase();
        if (code.length() != 6) {
            throw new BusinessRuleException("Mã tiểu đội phải có 6 ký tự");
        }
        Squad squad = squadRepository
                .findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessRuleException("Mã tiểu đội không hợp lệ"));
        if (!squadMemberRepository.existsBySquadIdAndUserId(squad.getId(), userId)) {
            joinInternal(squad, userId, Instant.now());
        }
        int count = squadMemberRepository.countBySquadId(squad.getId());
        return new SquadCreatedResponse(
                squad.getId(), squad.getCode(), squad.getSiteCode(), squad.getLeaderUserId(), squad.getCreatedAt(), count);
    }

    @Transactional(readOnly = true)
    public SquadMeResponse getMySquad(UUID userId) {
        SquadMember membership = squadMemberRepository
                .findFirstByUserIdOrderByJoinedAtDesc(userId)
                .orElseThrow(() -> new BusinessRuleException("Bạn chưa tham gia tiểu đội nào"));
        Squad squad = squadRepository
                .findById(membership.getSquadId())
                .orElseThrow(() -> new BusinessRuleException("Tiểu đội không tồn tại"));
        return toMeResponse(squad);
    }

    @Transactional(readOnly = true)
    public SquadMeResponse getSquadForMember(UUID userId, UUID squadId) {
        if (!squadMemberRepository.existsBySquadIdAndUserId(squadId, userId)) {
            throw new AuthException("Forbidden");
        }
        Squad squad = squadRepository.findById(squadId).orElseThrow(() -> new BusinessRuleException("Tiểu đội không tồn tại"));
        return toMeResponse(squad);
    }

    private SquadMeResponse toMeResponse(Squad squad) {
        List<SquadMember> members = squadMemberRepository.findBySquadIdOrderByJoinedAtAsc(squad.getId());
        Map<UUID, SquadMemberLiveState> live = liveStateService.snapshot(squad.getId());
        List<SquadMemberStateResponse> memberStates = new ArrayList<>();
        for (SquadMember member : members) {
            Profile profile = profileRepository.findById(member.getUserId()).orElse(null);
            SquadMemberLiveState state = live.getOrDefault(member.getUserId(), SquadMemberLiveState.empty(member.getUserId()));
            memberStates.add(new SquadMemberStateResponse(
                    member.getUserId(),
                    profile != null ? profile.getDisplayName() : "Khách",
                    profile != null ? profile.getAvatarUrl() : null,
                    member.getJoinedAt(),
                    state.stationCode(),
                    state.progressPercent(),
                    state.progressLabel()));
        }
        return new SquadMeResponse(
                squad.getId(),
                squad.getCode(),
                squad.getSiteCode(),
                squad.getLeaderUserId(),
                squad.getCreatedAt(),
                memberStates);
    }

    private void joinInternal(Squad squad, UUID userId, Instant joinedAt) {
        squadMemberRepository.save(SquadMember.builder()
                .squadId(squad.getId())
                .userId(userId)
                .joinedAt(joinedAt)
                .build());
    }

    private String normalizeSiteCode(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateUniqueCode(int length) {
        for (int attempt = 0; attempt < 50; attempt++) {
            String code = randomCode(length);
            if (!squadRepository.existsByCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new BusinessRuleException("Không thể tạo mã tiểu đội");
    }

    private static String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
