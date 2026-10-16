package com.histar.be.admin.contentreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.NarrativeConflictException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.events.entity.PilotEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminContentReportServiceTest {

    @Mock private ContentReportCaseRepository cases;
    @Mock private CurrentUserAccessor currentUser;
    @Mock private AdminAuditService audit;

    @Test
    void updatesDurableStatusWithoutInspectingPayloadAndAuditsExactlyOnce() {
        UUID eventId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        PilotEvent event = PilotEvent.builder().id(eventId).payloadJson("{\"status\":\"reviewed\"}")
                .eventType("content_report").occurredAt(Instant.now()).build();
        ContentReportCase report = ContentReportCase.builder()
                .event(event).status(ContentReportStatus.NEW).rowVersion(4L).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(cases.findByIdForUpdate(eventId)).thenReturn(Optional.of(report));
        when(currentUser.getUserId()).thenReturn(Optional.of(actorId));
        when(cases.saveAndFlush(report)).thenReturn(report);

        AdminContentReportService service = new AdminContentReportService(cases, currentUser, audit);
        ContentReportResponse response = service.update(eventId,
                new UpdateContentReportRequest(ContentReportStatus.RESOLVED, "Nguồn đã được xác minh", 4L));

        assertThat(response.status()).isEqualTo(ContentReportStatus.RESOLVED);
        assertThat(report.getReviewedBy()).isEqualTo(actorId);
        ArgumentCaptor<AdminAuditCommand> command = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(audit).append(command.capture());
        assertThat(command.getValue().action()).isEqualTo("CONTENT_REPORT_STATUS_UPDATED");
        assertThat(command.getValue().actorId()).isEqualTo(actorId);
    }

    @Test
    void rejectsStaleRowVersionWithoutMutationOrAudit() {
        UUID eventId = UUID.randomUUID();
        ContentReportCase report = ContentReportCase.builder()
                .event(PilotEvent.builder().id(eventId).eventType("content_report").occurredAt(Instant.now()).build())
                .status(ContentReportStatus.NEW).rowVersion(5L).build();
        when(cases.findByIdForUpdate(eventId)).thenReturn(Optional.of(report));

        AdminContentReportService service = new AdminContentReportService(cases, currentUser, audit);
        assertThatThrownBy(() -> service.update(eventId,
                new UpdateContentReportRequest(ContentReportStatus.IN_REVIEW, "Đang kiểm tra nguồn", 4L)))
                .isInstanceOfSatisfying(NarrativeConflictException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));

        verify(cases, never()).saveAndFlush(any());
        verify(audit, never()).append(any());
    }
}
