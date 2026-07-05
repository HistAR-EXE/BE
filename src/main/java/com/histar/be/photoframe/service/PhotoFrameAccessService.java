package com.histar.be.photoframe.service;

import com.histar.be.config.HistarOrgProperties;
import com.histar.be.photoframe.entity.PhotoFrame;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhotoFrameAccessService {

    private final HistarOrgProperties histarOrgProperties;

    public boolean isFrameFree(PhotoFrame frame, int index) {
        String configured = histarOrgProperties.getTier().getFreePhotoFrameIds();
        if (configured == null || configured.isBlank()) {
            return index < 2;
        }
        Set<String> freeIds = Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        String nameKey = frame.getName() == null ? "" : frame.getName().trim().toLowerCase();
        return freeIds.contains(nameKey) || index < freeIds.size();
    }
}
