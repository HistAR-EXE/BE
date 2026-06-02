package com.histar.be.usercreation.service.impl;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.gamification.LevelCalculator;
import com.histar.be.config.GamificationProperties;
import com.histar.be.config.ViralProperties;
import com.histar.be.media.service.MediaStorageService;
import com.histar.be.photoframe.service.PhotoFrameService;
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
    private final UserCreationRepository userCreationRepository;
    private final ProfileRepository profileRepository;
    private final MediaStorageService mediaStorageService;
    private final ViralProperties viralProperties;
    private final GamificationProperties gamificationProperties;

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

        photoFrameService.findById(frameId);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessRuleException("Không đọc được file ảnh");
        }

        String extension = contentType.equals("image/png") ? "png" : contentType.equals("image/webp") ? "webp" : "jpg";
        String objectKey = "creations/" + userId + "/" + UUID.randomUUID() + "." + extension;
        String outputUrl = mediaStorageService.uploadImage(bytes, contentType, objectKey);

        UserCreation saved = userCreationRepository.save(UserCreation.builder()
                .userId(userId)
                .frameId(frameId)
                .outputUrl(outputUrl)
                .variant(normalizedVariant)
                .createdAt(Instant.now())
                .build());
        return UserCreationResponse.from(saved);
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

        Profile profile = profileRepository.findById(userId).orElseThrow();
        int bonus = viralProperties.getShareBonusPoints();
        int newPoints = (profile.getTotalPoints() == null ? 0 : profile.getTotalPoints()) + bonus;
        profile.setTotalPoints(newPoints);
        profile.setLevel(LevelCalculator.levelFromPoints(newPoints, gamificationProperties.parseLevelThresholds()));
        profileRepository.save(profile);

        return new ShareRecordedResponse(bonus, newPoints, viralProperties.getShareCaption());
    }
}
