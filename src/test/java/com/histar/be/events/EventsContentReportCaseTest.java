package com.histar.be.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.admin.contentreport.ContentReportCase;
import com.histar.be.admin.contentreport.ContentReportCaseRepository;
import com.histar.be.admin.contentreport.ContentReportStatus;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.events.dto.EventBatchRequest;
import com.histar.be.events.entity.PilotEvent;
import com.histar.be.events.repository.PilotEventRepository;
import com.histar.be.events.service.EventsService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventsContentReportCaseTest {

    @Mock private PilotEventRepository events;
    @Mock private CheckinRepository checkins;
    @Mock private ContentReportCaseRepository cases;

    @Test
    void createsNewDurableCaseForNewContentReportWithoutPayloadInference() {
        when(events.save(any(PilotEvent.class))).thenAnswer(invocation -> {
            PilotEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });
        EventBatchRequest.PilotEventItem item = new EventBatchRequest.PilotEventItem(
                UUID.randomUUID(), "content_report", "ST01",
                "{\"status\":\"resolved\"}", Instant.now(), UUID.randomUUID());
        EventsService service = new EventsService(events, checkins, cases);

        service.ingestBatch(new EventBatchRequest(List.of(item)), UUID.randomUUID());

        ArgumentCaptor<ContentReportCase> captor = ArgumentCaptor.forClass(ContentReportCase.class);
        verify(cases).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ContentReportStatus.NEW);
    }
}
