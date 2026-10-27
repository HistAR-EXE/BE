package com.histar.be.usercreation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.config.GamificationProperties;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.ViralProperties;
import com.histar.be.media.service.MediaStorageService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.photoframe.service.PhotoFrameAccessService;
import com.histar.be.photoframe.service.PhotoFrameService;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.profile.service.TierAccessService;
import com.histar.be.usercreation.entity.UserCreation;
import com.histar.be.usercreation.repository.UserCreationRepository;
import com.histar.be.usercreation.service.impl.UserCreationAppServiceImpl;
import java.util.UUID;
import java.util.Optional;
import org.assertj.core.api.Assertions;
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
    @Mock CheckinRepository checkinRepository;
    @Mock LocationRepository locationRepository;
    @InjectMocks UserCreationAppServiceImpl service;

    @Test
    void downloadsOnlyOwnersSavedImage() {
        UUID ownerId = UUID.randomUUID();
        UUID creationId = UUID.randomUUID();
        String url = "https://media.example/creations/photo.jpg";
        UserCreation creation = UserCreation.builder().userId(ownerId).outputUrl(url).build();
        when(userCreationRepository.findById(creationId)).thenReturn(Optional.of(creation));
        when(mediaStorageService.downloadImage(url)).thenReturn(new byte[] {1, 2, 3});

        assertThat(service.downloadMine(ownerId, creationId)).containsExactly(1, 2, 3);
        verify(mediaStorageService).downloadImage(url);
    }

    @Test
    void rejectsDownloadFromAnotherUserBeforeReadingMedia() {
        UUID ownerId = UUID.randomUUID();
        UUID creationId = UUID.randomUUID();
        UserCreation creation = UserCreation.builder().userId(ownerId).outputUrl("https://media.example/photo.jpg").build();
        when(userCreationRepository.findById(creationId)).thenReturn(Optional.of(creation));

        Assertions.assertThatThrownBy(() -> service.downloadMine(UUID.randomUUID(), creationId))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(mediaStorageService);
    }

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

    @Test
    void freeUserHeritageUploadPersistsContextAndAwardsQrBonusOncePerStationKey() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "portrait.jpg", "image/jpeg", new byte[] {1, 2, 3});
        when(mediaStorageService.uploadImage(any(), any(), any())).thenReturn("https://media.example/heritage.jpg");
        when(userCreationRepository.save(any())).thenAnswer(invocation -> {
            UserCreation creation = invocation.getArgument(0);
            creation.setId(UUID.randomUUID());
            return creation;
        });
        when(locationRepository.findBySiteCodeIgnoreCase("cu-chi")).thenReturn(Optional.of(Location.builder().id(locationId).siteCode("cu-chi").build()));
        when(checkinRepository.existsByUserIdAndLocationIdAndStationCodeIgnoreCaseAndPresenceMethodAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.eq(locationId), org.mockito.ArgumentMatchers.eq("ST01"),
                org.mockito.ArgumentMatchers.eq("QR"), any(), any())).thenReturn(true);
        when(gamificationProperties.getHeritagePhotoBonusPoints()).thenReturn(10);

        var result = service.upload(userId, null, "story", "heritage", "cu-chi", "ST01",
                "st01-can-bo-lien-lac-nam", 1968, "station", file);

        assertThat(result.kind()).isEqualTo("heritage");
        assertThat(result.stationCode()).isEqualTo("ST01");
        assertThat(result.roleId()).isEqualTo("st01-can-bo-lien-lac-nam");
        verify(points).awardOnce(userId, "heritage_photo:cu-chi:ST01", 10);
        verifyNoInteractions(photoFrameService, photoFrameAccessService, tierAccessService);
    }

    @Test
    void remoteHeritageUploadDoesNotAwardPoints() {
        UUID userId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "portrait.jpg", "image/jpeg", new byte[] {1});
        when(mediaStorageService.uploadImage(any(), any(), any())).thenReturn("https://media.example/heritage.jpg");
        when(userCreationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.upload(userId, null, "story", "heritage", "cu-chi", "ST01",
                "st01-can-bo-lien-lac-nam", 1968, "battle-diorama", file);

        verifyNoInteractions(points, checkinRepository);
    }

    @Test
    void heritageWithoutQrCheckinSavesButDoesNotAwardPoints() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "portrait.jpg", "image/jpeg", new byte[] {1});
        when(mediaStorageService.uploadImage(any(), any(), any())).thenReturn("https://media.example/heritage.jpg");
        when(userCreationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(locationRepository.findBySiteCodeIgnoreCase("cu-chi")).thenReturn(Optional.of(Location.builder().id(locationId).siteCode("cu-chi").build()));

        service.upload(userId, null, "story", "heritage", "cu-chi", "ST01",
                "st01-can-bo-lien-lac-nam", 1968, "station", file);

        verify(checkinRepository).existsByUserIdAndLocationIdAndStationCodeIgnoreCaseAndPresenceMethodAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.eq(locationId), org.mockito.ArgumentMatchers.eq("ST01"),
                org.mockito.ArgumentMatchers.eq("QR"), any(), any());
        verifyNoInteractions(points);
    }

    @Test
    void heritageWithoutMatchingSiteCannotUseAnotherSitesStationQr() {
        UUID userId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "portrait.jpg", "image/jpeg", new byte[] {1});
        when(mediaStorageService.uploadImage(any(), any(), any())).thenReturn("https://media.example/heritage.jpg");
        when(userCreationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(locationRepository.findBySiteCodeIgnoreCase("cu-chi")).thenReturn(Optional.empty());

        service.upload(userId, null, "story", "heritage", "cu-chi", "ST01",
                "st01-can-bo-lien-lac-nam", 1968, "station", file);

        verifyNoInteractions(checkinRepository, points);
    }

    @Test
    void ownerCanDeleteCreationAndItsMedia() {
        UUID userId = UUID.randomUUID();
        UUID creationId = UUID.randomUUID();
        UserCreation creation = UserCreation.builder().id(creationId).userId(userId)
                .outputUrl("https://media.example/creations/photo.jpg").build();
        when(userCreationRepository.findById(creationId)).thenReturn(Optional.of(creation));

        service.deleteMine(userId, creationId);

        verify(userCreationRepository).delete(creation);
        verify(mediaStorageService).deleteObject(creation.getOutputUrl());
    }

    @Test
    void cannotDeleteAnotherUsersCreation() {
        UUID userId = UUID.randomUUID();
        UUID creationId = UUID.randomUUID();
        when(userCreationRepository.findById(creationId)).thenReturn(Optional.of(
                UserCreation.builder().id(creationId).userId(UUID.randomUUID()).build()));

        Assertions.assertThatThrownBy(() -> service.deleteMine(userId, creationId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userCreationRepository, org.mockito.Mockito.never()).delete(any());
        verifyNoInteractions(mediaStorageService);
    }

    @Test
    void missingCreationCannotBeDeleted() {
        UUID creationId = UUID.randomUUID();
        when(userCreationRepository.findById(creationId)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> service.deleteMine(UUID.randomUUID(), creationId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(mediaStorageService);
    }

    @Test
    void invalidHeritageContextStopsBeforeUpload() {
        UUID userId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "portrait.jpg", "image/jpeg", new byte[] {1});

        Assertions.assertThatThrownBy(() -> service.upload(userId, null, "story", "heritage", "cu-chi",
                "ST02", "st01-can-bo-lien-lac-nam", 1968, "station", file))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(mediaStorageService, userCreationRepository, points);
    }
}
