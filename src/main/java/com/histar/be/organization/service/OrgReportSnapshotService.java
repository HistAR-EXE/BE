package com.histar.be.organization.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.liveboard.dto.LiveBoardSnapshot;
import com.histar.be.liveboard.service.LiveBoardService;
import com.histar.be.organization.dto.OrgReportSnapshotResponse;
import com.histar.be.organization.entity.OrgReportSnapshot;
import com.histar.be.organization.repository.OrgReportSnapshotRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrgReportSnapshotService {
    private final OrgReportSnapshotRepository repository;
    private final OrganizationEntitlementService entitlements;
    private final LiveBoardService liveBoard;
    private final ObjectMapper mapper;

    @Transactional
    public OrgReportSnapshotResponse capture(UUID actorId, UUID orgId) {
        entitlements.requireCapability(actorId, orgId, "reports");
        LiveBoardSnapshot snapshot = liveBoard.snapshot(orgId);
        try {
            OrgReportSnapshot saved = repository.save(OrgReportSnapshot.builder()
                    .organizationId(orgId).createdBy(actorId).createdAt(Instant.now())
                    .snapshotJson(mapper.writeValueAsString(snapshot)).build());
            return new OrgReportSnapshotResponse(saved.getId(), saved.getCreatedAt(), snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể lưu báo cáo", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<OrgReportSnapshotResponse> list(UUID actorId, UUID orgId) {
        entitlements.requireCapability(actorId, orgId, "reports");
        return repository.findTop50ByOrganizationIdOrderByCreatedAtDesc(orgId).stream().map(saved -> {
            try {
                return new OrgReportSnapshotResponse(saved.getId(), saved.getCreatedAt(),
                        mapper.readValue(saved.getSnapshotJson(), LiveBoardSnapshot.class));
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Không thể đọc báo cáo", ex);
            }
        }).toList();
    }
}
