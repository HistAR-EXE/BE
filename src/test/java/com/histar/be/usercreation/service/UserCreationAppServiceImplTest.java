package com.histar.be.usercreation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.histar.be.config.GamificationProperties;
import com.histar.be.config.ViralProperties;
import com.histar.be.media.service.MediaStorageService;
import com.histar.be.photoframe.service.PhotoFrameAccessService;
import com.histar.be.photoframe.service.PhotoFrameService;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.profile.service.TierAccessService;
import com.histar.be.usercreation.entity.UserCreation;
import com.histar.be.usercreation.repository.UserCreationRepository;
import com.histar.be.usercreation.service.impl.UserCreationAppServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class UserCreationAppServiceImplTest {

    @Mock PhotoFrameService photoFrameService;
    @Mock PhotoFrameAccessService photoFrameAccessService;
    @Mock TierAccessService tierAccessService;
    @Mock UserCreationRepository userCreationRepository;
    @Mock ProfileRepository profileRepository;
    @Mock MediaStorageService mediaStorageService;
    @Mock ViralProperties viralProperties;
    @Mock GamificationProperties gamificationProperties;
    @Mock ProfilePointsService points;
    @InjectMocks UserCreationAppServiceImpl service;

    @Test
    void uploadsOriginalImageWithoutFrame() {
        UUID userId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "station.jpg", "image/jpeg", new byte[] {1, 2, 3});
        when(mediaStorageService.uploadImage(any(), any(), any())).thenReturn("https://media.example/original.jpg");
        when(userCreationRepository.save(any())).thenAnswer(invocation -> {
            UserCreation creation = invocation.getArgument(0);
            creation.setId(UUID.randomUUID());
            return creation;
        });

        var result = service.upload(userId, null, "square", file);

        assertThat(result.frameId()).isNull();
        assertThat(result.outputUrl()).isEqualTo("https://media.example/original.jpg");
        verifyNoInteractions(photoFrameService, photoFrameAccessService, tierAccessService);
    }
}
