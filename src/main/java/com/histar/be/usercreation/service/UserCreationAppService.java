package com.histar.be.usercreation.service;

import com.histar.be.usercreation.dto.ShareRecordedResponse;
import com.histar.be.usercreation.dto.UserCreationResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserCreationAppService {

    default UserCreationResponse upload(UUID userId, UUID frameId, String variant, MultipartFile file) {
        return upload(userId, frameId, variant, "original", null, null, null, null, null, file);
    }

    UserCreationResponse upload(UUID userId, UUID frameId, String variant, String kind, String siteCode,
            String stationCode, String roleId, Integer era, String source, MultipartFile file);

    List<UserCreationResponse> listMine(UUID userId);

    void deleteMine(UUID userId, UUID creationId);

    byte[] downloadMine(UUID userId, UUID creationId);

    ShareRecordedResponse recordShare(UUID userId, UUID creationId);
}
