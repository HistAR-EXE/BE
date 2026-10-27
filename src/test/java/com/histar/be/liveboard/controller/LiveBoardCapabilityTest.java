package com.histar.be.liveboard.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.liveboard.service.LiveBoardService;
import com.histar.be.organization.service.OrgAccessService;
import com.histar.be.organization.service.OrganizationEntitlementService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LiveBoardCapabilityTest {
    @Test void microCannotReadSnapshotStreamCsvOrRallyThroughDirectApiCalls() {
        var liveBoard = mock(LiveBoardService.class);
        var access = mock(OrgAccessService.class);
        var currentUser = mock(CurrentUserAccessor.class);
        var entitlements = mock(OrganizationEntitlementService.class);
        UUID userId = UUID.randomUUID(), orgId = UUID.randomUUID();
        when(currentUser.getUserId()).thenReturn(Optional.of(userId));
        doThrow(new BusinessRuleException("Standard/Premium required")).when(entitlements)
                .requireCapability(userId, orgId, "liveBoard");
        var controller = new LiveBoardController(liveBoard, access, currentUser, entitlements);
        assertThatThrownBy(() -> controller.snapshot(orgId)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> controller.stream(orgId)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> controller.exportCsv(orgId)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> controller.rally(orgId)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(liveBoard);
    }
}
