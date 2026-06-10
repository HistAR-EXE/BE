package com.histar.be.panorama.service;

import com.histar.be.panorama.dto.PanoramaResponse;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface PanoramaAppService {

    PanoramaResponse upload(UUID locationId, String title, MultipartFile file);

    PanoramaResponse replaceImage(UUID panoramaId, MultipartFile file);
}
