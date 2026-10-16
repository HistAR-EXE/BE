package com.histar.be.panorama.service.impl;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.location.service.LocationService;
import com.histar.be.media.service.MediaStorageService;
import com.histar.be.panorama.dto.PanoramaResponse;
import com.histar.be.panorama.entity.Panorama;
import com.histar.be.panorama.service.PanoramaAppService;
import com.histar.be.panorama.service.PanoramaService;
import java.io.IOException;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PanoramaAppServiceImpl implements PanoramaAppService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 8 * 1024 * 1024;

    private final PanoramaService panoramaService;
    private final LocationService locationService;
    private final MediaStorageService mediaStorageService;
    private final CurrentUserAccessor currentUserAccessor;
    private final AdminAuditService audit;

    @Override
    @Transactional
    public PanoramaResponse upload(UUID locationId, String title, MultipartFile file) {
        locationService.findById(locationId);
        byte[] bytes = readImage(file);
        UUID actorId = actorId();
        String contentType = file.getContentType();
        String imageUrl = storePanorama(locationId, bytes, contentType, file.getOriginalFilename());
        boolean rollbackCleanupRegistered = registerRollbackCleanup(imageUrl);
        try {
            Panorama saved = panoramaService.save(Panorama.builder()
                    .locationId(locationId)
                    .title(title != null && !title.isBlank() ? title.trim() : "Panorama 360°")
                    .imageUrl(imageUrl)
                    .build());
            audit.append(new AdminAuditCommand(actorId, "PANORAMA", saved.getId(), "PANORAMA_UPLOADED",
                    null, null, null, null, Map.of("locationId", locationId, "imageUrl", imageUrl)));
            return PanoramaResponse.from(saved);
        } catch (RuntimeException ex) {
            if (!rollbackCleanupRegistered) safelyDelete(imageUrl);
            throw ex;
        }
    }

    @Override
    @Transactional
    public PanoramaResponse replaceImage(UUID panoramaId, MultipartFile file) {
        Panorama panorama = panoramaService.findById(panoramaId);
        String previousImageUrl = panorama.getImageUrl();
        byte[] bytes = readImage(file);
        UUID actorId = actorId();
        String contentType = file.getContentType();
        String imageUrl = storePanorama(panorama.getLocationId(), bytes, contentType, file.getOriginalFilename());
        boolean rollbackCleanupRegistered = registerRollbackCleanup(imageUrl);
        try {
            panorama.setImageUrl(imageUrl);
            Panorama saved = panoramaService.save(panorama);
            audit.append(new AdminAuditCommand(actorId, "PANORAMA", panoramaId, "PANORAMA_IMAGE_REPLACED",
                    null, null, null, null,
                    Map.of("oldImageUrl", previousImageUrl == null ? "" : previousImageUrl, "newImageUrl", imageUrl)));
            return PanoramaResponse.from(saved);
        } catch (RuntimeException ex) {
            panorama.setImageUrl(previousImageUrl);
            if (!rollbackCleanupRegistered) safelyDelete(imageUrl);
            throw ex;
        }
    }

    private UUID actorId() {
        return currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }

    private byte[] readImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("File ảnh 360 không được rỗng");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessRuleException("Ảnh 360 phải là JPEG, PNG hoặc WebP (equirectangular 2:1)");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleException("Ảnh 360 tối đa 8MB — hãy nén trước khi upload");
        }
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BusinessRuleException("Không đọc được file ảnh 360");
        }
    }

    private String storePanorama(UUID locationId, byte[] bytes, String contentType, String originalName) {
        String extension = extensionFor(contentType);
        String safeName = originalName != null ? originalName.replaceAll("[^a-zA-Z0-9._-]", "_") : "panorama";
        String objectKey = "panoramas/" + locationId + "/" + UUID.randomUUID() + "-" + safeName;
        if (!objectKey.endsWith("." + extension)) {
            objectKey = objectKey + "." + extension;
        }
        return mediaStorageService.uploadImage(bytes, contentType, objectKey);
    }

    private boolean registerRollbackCleanup(String imageUrl) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) safelyDelete(imageUrl);
            }
        });
        return true;
    }

    private void safelyDelete(String imageUrl) {
        try {
            mediaStorageService.deleteObject(imageUrl);
        } catch (RuntimeException ignored) {
            // Preserve the original database/audit failure; cleanup can be retried operationally.
        }
    }

    private static String extensionFor(String contentType) {
        if ("image/png".equals(contentType)) {
            return "png";
        }
        if ("image/webp".equals(contentType)) {
            return "webp";
        }
        return "jpg";
    }
}
