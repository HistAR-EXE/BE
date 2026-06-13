package com.histar.be.photoscene.dto;

import com.histar.be.photoscene.entity.PhotoScene;
import java.util.List;
import java.util.UUID;

public record PhotoSceneResponse(
        UUID id, String name, Integer sortOrder, String unlockKey, List<TimeLayerResponse> layers) {

    public static PhotoSceneResponse from(PhotoScene scene, List<TimeLayerResponse> layers) {
        return new PhotoSceneResponse(
                scene.getId(), scene.getName(), scene.getSortOrder(), scene.getUnlockKey(), layers);
    }
}
