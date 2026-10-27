package com.histar.be.usercreation.service.impl;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.gamification.LevelCalculator;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.config.GamificationProperties;
import com.histar.be.config.ViralProperties;
import com.histar.be.media.service.MediaStorageService;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.photoframe.entity.PhotoFrame;
import com.histar.be.photoframe.service.PhotoFrameAccessService;
import com.histar.be.photoframe.service.PhotoFrameService;
import com.histar.be.profile.service.TierAccessService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.usercreation.dto.ShareRecordedResponse;
import com.histar.be.usercreation.dto.UserCreationResponse;
import com.histar.be.usercreation.entity.UserCreation;
import com.histar.be.usercreation.repository.UserCreationRepository;
import com.histar.be.usercreation.service.UserCreationAppService;
import com.histar.be.usercreation.HeritageCreationContext;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCreationAppServiceImpl implements UserCreationAppService {

    private static final Set<String> ALLOWED_VARIANTS = Set.of("square", "story");
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final PhotoFrameService photoFrameService;
    private final PhotoFrameAccessService photoFrameAccessService;
    private final TierAccessService tierAccessService;
    private final UserCreationRepository userCreationRepository;
    private final ProfileRepository profileRepository;
    private final MediaStorageService mediaStorageService;
    private final ViralProperties viralProperties;
    private final GamificationProperties gamificationProperties;
    private final com.histar.be.profile.service.ProfilePointsService points;
    private final CheckinRepository checkinRepository;
    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public UserCreationResponse upload(UUID userId, UUID frameId, String variant, String kind, String siteCode,
            String stationCode, String roleId, Integer era, String source, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("File ảnh không được rỗng");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessRuleException("Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP");
        }
        String normalizedVariant = variant == null ? "square" : variant.toLowerCase();
        if (!ALLOWED_VARIANTS.contains(normalizedVariant)) {
            throw new BusinessRuleException("variant phải là square hoặc story");
        }
        HeritageCreationContext context = HeritageCreationContext.validate(kind, siteCode, stationCode, roleId, era, source);

        if (frameId != null) {
            PhotoFrame frame = photoFrameService.findById(frameId);
            List<PhotoFrame> usableFrames = photoFrameService.findAllOrdered().stream()
                    .filter(PhotoFrameAccessService::isUsableFrame)
                    .toList();
            int index = usableFrames.indexOf(frame);
            if (index < 0) {
                throw new BusinessRuleException("Khung ảnh không khả dụng");
            }
            if (!tierAccessService.hasPremiumAccess(userId)) {
                if (!photoFrameAccessService.isFrameFree(frame, index)) {
                    throw new BusinessRuleException("Khung ảnh Premium — nâng cấp tại /pricing để sử dụng.");
                }
            }
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessRuleException("Không đọc được file ảnh");
        }

        String extension = contentType.equals("image/png") ? "png" : contentType.equals("image/webp") ? "webp" : "jpg";
        String objectKey = "creations/" + userId + "/" + UUID.randomUUID() + "." + extension;
        String outputUrl = mediaStorageService.uploadImage(bytes, contentType, objectKey);

        try {
            UserCreation saved = userCreationRepository.save(UserCreation.builder()
                    .userId(userId)
                    .frameId(frameId)
                    .outputUrl(outputUrl)
                    .variant(normalizedVariant)
                    .kind(context.kind())
                    .siteCode(context.siteCode())
                    .stationCode(context.stationCode())
                    .roleId(context.roleId())
                    .era(context.era())
                    .source(context.source())
                    .createdAt(Instant.now())
                    .build());
            awardHeritagePhotoBonus(userId, context);
            return UserCreationResponse.from(saved);
        } catch (RuntimeException ex) {
            // The object is uploaded before the DB row. Avoid leaving orphaned
            // media when persistence fails and preserve the original failure.
            try {
                mediaStorageService.deleteObject(outputUrl);
            } catch (RuntimeException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }
            throw ex;
        }
    }

    private void awardHeritagePhotoBonus(UUID userId, HeritageCreationContext context) {
        if (!"heritage".equals(context.kind()) || context.stationCode() == null || "battle-diorama".equals(context.source())) return;
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);
        Instant from = today.atStartOfDay(zone).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zone).toInstant();
        var location = locationRepository.findBySiteCodeIgnoreCase(context.siteCode()).orElse(null);
        if (location != null && checkinRepository.existsByUserIdAndLocationIdAndStationCodeIgnoreCaseAndPresenceMethodAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                userId, location.getId(), context.stationCode(), "QR", from, to)) {
            points.awardOnce(userId, "heritage_photo:" + context.siteCode() + ":" + context.stationCode(), gamificationProperties.getHeritagePhotoBonusPoints());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCreationResponse> listMine(UUID userId) {
        return userCreationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(UserCreationResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteMine(UUID userId, UUID creationId) {
        UserCreation creation = userCreationRepository.findById(creationId)
                .orElseThrow(() -> new ResourceNotFoundException("UserCreation not found: " + creationId));
        if (!creation.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("UserCreation not found: " + creationId);
        }
        String outputUrl = creation.getOutputUrl();
        userCreationRepository.delete(creation);
        // The database owns the creation. Remove media only after its deletion commits.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        mediaStorageService.deleteObject(outputUrl);
                    } catch (RuntimeException ex) {
                        log.error("CREATION_MEDIA_DELETE_FAILED creationId={}", creationId);
                    }
                }
            });
        } else {
            mediaStorageService.deleteObject(outputUrl);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadMine(UUID userId, UUID creationId) {
        UserCreation creation = userCreationRepository.findById(creationId)
                .orElseThrow(() -> new ResourceNotFoundException("UserCreation not found: " + creationId));
        if (!creation.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("UserCreation not found: " + creationId);
        }
        return mediaStorageService.downloadImage(creation.getOutputUrl());
    }

    @Override
    @Transactional
    public ShareRecordedResponse recordShare(UUID userId, UUID creationId) {
        points.lockAccount(userId);
        UserCreation creation = userCreationRepository
                .findById(creationId)
                .orElseThrow(() -> new ResourceNotFoundException("UserCreation not found: " + creationId));
        if (!creation.getUserId().equals(userId)) {
            throw new BusinessRuleException("Không có quyền với creation này");
        }
        if (creation.getSharedAt() != null) {
            Profile profile = profileRepository.findById(userId).orElseThrow();
            return new ShareRecordedResponse(
                    0, profile.getTotalPoints() == null ? 0 : profile.getTotalPoints(), viralProperties.getShareCaption());
        }

        creation.setSharedAt(Instant.now());
        userCreationRepository.save(creation);

        int bonus = viralProperties.getShareBonusPoints();
        bonus = points.awardOnce(userId, "share:" + creationId, bonus);
        int newPoints = profileRepository.findById(userId).orElseThrow().getTotalPoints();

        return new ShareRecordedResponse(bonus, newPoints, viralProperties.getShareCaption());
    }
}
