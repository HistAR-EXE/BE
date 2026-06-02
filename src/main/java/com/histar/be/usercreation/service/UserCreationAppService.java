package com.histar.be.usercreation.service;

import com.histar.be.usercreation.dto.ShareRecordedResponse;
import com.histar.be.usercreation.dto.UserCreationResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserCreationAppService {

    UserCreationResponse upload(UUID userId, UUID frameId, String variant, MultipartFile file);

    List<UserCreationResponse> listMine(UUID userId);

    ShareRecordedResponse recordShare(UUID userId, UUID creationId);
}
