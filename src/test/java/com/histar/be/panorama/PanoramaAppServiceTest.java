package com.histar.be.panorama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.location.entity.Location;
import com.histar.be.location.service.LocationService;
import com.histar.be.media.service.MediaStorageService;
import com.histar.be.panorama.dto.PanoramaResponse;
import com.histar.be.panorama.entity.Panorama;
import com.histar.be.panorama.service.PanoramaService;
import com.histar.be.panorama.service.impl.PanoramaAppServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PanoramaAppServiceTest {

    @Mock
    private PanoramaService panoramaService;

    @Mock
    private LocationService locationService;

    @Mock
    private MediaStorageService mediaStorageService;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @Mock
    private AdminAuditService audit;

    @InjectMocks
    private PanoramaAppServiceImpl panoramaAppService;

    @Test
    void upload_storesToMinioAndCreatesPanorama() {
        UUID locationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(currentUserAccessor.getUserId()).thenReturn(java.util.Optional.of(actorId));
        when(locationService.findById(locationId)).thenReturn(Location.builder().id(locationId).name("Củ Chi").build());
        when(mediaStorageService.uploadImage(any(), anyString(), anyString()))
                .thenReturn("http://localhost:9000/timelens-media/panoramas/test.jpg");
        when(panoramaService.save(any())).thenAnswer(inv -> {
            Panorama p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        MockMultipartFile file =
                new MockMultipartFile("file", "scene.jpg", "image/jpeg", new byte[] {1, 2, 3});
        PanoramaResponse response = panoramaAppService.upload(locationId, "Cổng vào", file);

        assertThat(response.imageUrl()).contains("panoramas");
        assertThat(response.title()).isEqualTo("Cổng vào");
        verify(mediaStorageService).uploadImage(any(), anyString(), anyString());
        ArgumentCaptor<AdminAuditCommand> command = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(audit).append(command.capture());
        assertThat(command.getValue().action()).isEqualTo("PANORAMA_UPLOADED");
        assertThat(command.getValue().actorId()).isEqualTo(actorId);
    }

    @Test
    void replaceImageAppendsExactlyOneAdminAuditEvent() {
        UUID panoramaId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Panorama panorama = Panorama.builder().id(panoramaId).locationId(UUID.randomUUID())
                .title("Scene").imageUrl("old.jpg").build();
        when(currentUserAccessor.getUserId()).thenReturn(java.util.Optional.of(actorId));
        when(panoramaService.findById(panoramaId)).thenReturn(panorama);
        when(mediaStorageService.uploadImage(any(), anyString(), anyString())).thenReturn("new.jpg");
        when(panoramaService.save(panorama)).thenReturn(panorama);

        panoramaAppService.replaceImage(panoramaId,
                new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[] {1}));

        ArgumentCaptor<AdminAuditCommand> command = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(audit).append(command.capture());
        assertThat(command.getValue().action()).isEqualTo("PANORAMA_IMAGE_REPLACED");
    }

    @Test
    void upload_rejectsEmptyFile() {
        UUID locationId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> panoramaAppService.upload(locationId, "T", file))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void uploadDeletesNewObjectWhenDatabaseWriteFails() {
        UUID locationId = UUID.randomUUID();
        when(locationService.findById(locationId)).thenReturn(Location.builder().id(locationId).build());
        when(currentUserAccessor.getUserId()).thenReturn(java.util.Optional.of(UUID.randomUUID()));
        when(mediaStorageService.uploadImage(any(), anyString(), anyString())).thenReturn("new-object.jpg");
        when(panoramaService.save(any())).thenThrow(new IllegalStateException("db failed"));

        assertThatThrownBy(() -> panoramaAppService.upload(locationId, "T",
                new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[] {1})))
                .isInstanceOf(IllegalStateException.class);

        verify(mediaStorageService).deleteObject("new-object.jpg");
    }

    @Test
    void replaceDeletesNewObjectWhenAuditFails() {
        UUID panoramaId = UUID.randomUUID();
        Panorama panorama = Panorama.builder().id(panoramaId).locationId(UUID.randomUUID())
                .title("Scene").imageUrl("old.jpg").build();
        when(currentUserAccessor.getUserId()).thenReturn(java.util.Optional.of(UUID.randomUUID()));
        when(panoramaService.findById(panoramaId)).thenReturn(panorama);
        when(mediaStorageService.uploadImage(any(), anyString(), anyString())).thenReturn("new.jpg");
        when(panoramaService.save(panorama)).thenReturn(panorama);
        doThrow(new IllegalStateException("audit failed")).when(audit).append(any());

        assertThatThrownBy(() -> panoramaAppService.replaceImage(panoramaId,
                new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[] {1})))
                .isInstanceOf(IllegalStateException.class);

        verify(mediaStorageService).deleteObject("new.jpg");
    }
}
