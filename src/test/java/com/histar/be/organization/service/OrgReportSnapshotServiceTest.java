package com.histar.be.organization.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.liveboard.dto.LiveBoardSnapshot;
import com.histar.be.liveboard.service.LiveBoardService;
import com.histar.be.organization.entity.OrgReportSnapshot;
import com.histar.be.organization.repository.OrgReportSnapshotRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrgReportSnapshotServiceTest {
    private final OrgReportSnapshotRepository repository = mock(OrgReportSnapshotRepository.class);
    private final OrganizationEntitlementService entitlements = mock(OrganizationEntitlementService.class);
    private final LiveBoardService liveBoard = mock(LiveBoardService.class);
    private final ObjectMapper mapper = mock(ObjectMapper.class);
    private final OrgReportSnapshotService service = new OrgReportSnapshotService(repository, entitlements, liveBoard, mapper);
    private final UUID actorId = UUID.randomUUID(), orgId = UUID.randomUUID();

    @Test void microCannotCaptureOrReadSnapshots() {
        doThrow(new BusinessRuleException("Reports require Standard/Premium")).when(entitlements)
                .requireCapability(actorId, orgId, "reports");
        assertThatThrownBy(() -> service.capture(actorId, orgId)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.list(actorId, orgId)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(repository, liveBoard, mapper);
    }

    @Test void captureUsesServerLiveBoardAndReturnsImmutableSavedSnapshot() throws Exception {
        Instant now = Instant.now();
        var board = new LiveBoardSnapshot(orgId, now, now.minusSeconds(86400), 0, 0, null, List.of());
        when(liveBoard.snapshot(orgId)).thenReturn(board);
        when(mapper.writeValueAsString(board)).thenReturn("server-snapshot-json");
        when(repository.save(any())).thenAnswer(call -> {
            OrgReportSnapshot snapshot = call.getArgument(0);
            snapshot.setId(UUID.randomUUID());
            return snapshot;
        });
        var result = service.capture(actorId, orgId);
        assertThat(result.id()).isNotNull();
        assertThat(result.snapshot()).isSameAs(board);
        verify(repository).save(argThat(snapshot -> orgId.equals(snapshot.getOrganizationId())
                && actorId.equals(snapshot.getCreatedBy()) && "server-snapshot-json".equals(snapshot.getSnapshotJson())));
    }

    @Test void listReadsOnlyRequestedOrganizationAndIsBoundedToLatestFifty() throws Exception {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        var board = new LiveBoardSnapshot(orgId, now, now, 0, 0, null, List.of());
        when(repository.findTop50ByOrganizationIdOrderByCreatedAtDesc(orgId)).thenReturn(List.of(
                OrgReportSnapshot.builder().id(id).organizationId(orgId).createdAt(now).snapshotJson("stored-json").build()));
        when(mapper.readValue("stored-json", LiveBoardSnapshot.class)).thenReturn(board);
        var result = service.list(actorId, orgId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
        verify(entitlements).requireCapability(actorId, orgId, "reports");
        verify(repository).findTop50ByOrganizationIdOrderByCreatedAtDesc(orgId);
        verifyNoInteractions(liveBoard);
    }
}
