package com.histar.be.usercreation.service.impl;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.gamification.LevelCalculator;
import com.histar.be.config.GamificationProperties;
import com.histar.be.config.ViralProperties;
import com.histar.be.media.service.MediaStorageService;
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
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
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

    @Override
    @Transactional
    public UserCreationResponse upload(UUID userId, UUID frameId, String variant, MultipartFile file) {
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
                    .createdAt(Instant.now())
                    .build());
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

    @Override
    @Transactional(readOnly = true)
    public List<UserCreationResponse> listMine(UUID userId) {
        return userCreationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(UserCreationResponse::from)
                .toList();
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
