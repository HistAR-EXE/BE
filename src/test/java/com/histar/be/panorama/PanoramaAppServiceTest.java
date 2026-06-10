package com.histar.be.panorama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.BusinessRuleException;
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

    @InjectMocks
    private PanoramaAppServiceImpl panoramaAppService;

    @Test
    void upload_storesToMinioAndCreatesPanorama() {
        UUID locationId = UUID.randomUUID();
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
    }

    @Test
    void upload_rejectsEmptyFile() {
        UUID locationId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> panoramaAppService.upload(locationId, "T", file))
                .isInstanceOf(BusinessRuleException.class);
    }
}
