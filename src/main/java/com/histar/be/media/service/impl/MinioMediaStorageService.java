package com.histar.be.media.service.impl;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.MinioProperties;
import com.histar.be.media.service.MediaStorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MinioMediaStorageService implements MediaStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public String uploadImage(byte[] data, String contentType, String objectKey) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .stream(new java.io.ByteArrayInputStream(data), data.length, -1)
                    .contentType(contentType)
                    .build());
            String base = minioProperties.getPublicUrl().replaceAll("/$", "");
            return base + "/" + minioProperties.getBucket() + "/" + objectKey;
        } catch (Exception ex) {
            throw new BusinessRuleException("Upload ảnh thất bại: " + ex.getMessage());
        }
    }

    @Override
    public void deleteObject(String objectUrl) {
        if (objectUrl == null || objectUrl.isBlank()) {
            return;
        }
        String marker = "/" + minioProperties.getBucket() + "/";
        int markerIndex = objectUrl.indexOf(marker);
        String objectKey = markerIndex >= 0 ? objectUrl.substring(markerIndex + marker.length()) : objectUrl;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new BusinessRuleException("Xóa ảnh thất bại: " + ex.getMessage());
        }
    }
}
