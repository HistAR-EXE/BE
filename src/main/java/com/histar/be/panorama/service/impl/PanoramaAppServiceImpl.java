package com.histar.be.panorama.service.impl;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.location.service.LocationService;
import com.histar.be.media.service.MediaStorageService;
import com.histar.be.panorama.dto.PanoramaResponse;
import com.histar.be.panorama.entity.Panorama;
import com.histar.be.panorama.service.PanoramaAppService;
import com.histar.be.panorama.service.PanoramaService;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PanoramaAppServiceImpl implements PanoramaAppService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 8 * 1024 * 1024;

    private final PanoramaService panoramaService;
    private final LocationService locationService;
    private final MediaStorageService mediaStorageService;

    @Override
    @Transactional
    public PanoramaResponse upload(UUID locationId, String title, MultipartFile file) {
        locationService.findById(locationId);
        byte[] bytes = readImage(file);
        String contentType = file.getContentType();
        String imageUrl = storePanorama(locationId, bytes, contentType, file.getOriginalFilename());

        Panorama saved = panoramaService.save(Panorama.builder()
                .locationId(locationId)
                .title(title != null && !title.isBlank() ? title.trim() : "Panorama 360°")
                .imageUrl(imageUrl)
                .build());
        return PanoramaResponse.from(saved);
    }

    @Override
    @Transactional
    public PanoramaResponse replaceImage(UUID panoramaId, MultipartFile file) {
        Panorama panorama = panoramaService.findById(panoramaId);
        byte[] bytes = readImage(file);
        String contentType = file.getContentType();
        String imageUrl = storePanorama(panorama.getLocationId(), bytes, contentType, file.getOriginalFilename());
        panorama.setImageUrl(imageUrl);
        return PanoramaResponse.from(panoramaService.save(panorama));
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
