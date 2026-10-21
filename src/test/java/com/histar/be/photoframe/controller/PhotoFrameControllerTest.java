package com.histar.be.photoframe.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.photoframe.entity.PhotoFrame;
import com.histar.be.photoframe.service.PhotoFrameAccessService;
import com.histar.be.photoframe.service.PhotoFrameService;
import com.histar.be.profile.service.TierAccessService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhotoFrameControllerTest {

    @Mock PhotoFrameService photoFrameService;
    @Mock PhotoFrameAccessService photoFrameAccessService;
    @Mock TierAccessService tierAccessService;
    @Mock CurrentUserAccessor currentUserAccessor;
    @InjectMocks PhotoFrameController controller;

    @Test
    void excludesBlankAndPlaceholderFrames() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.empty());
        when(photoFrameService.findAllOrdered()).thenReturn(List.of(
                frame("Mẫu", "https://placehold.co/1080?text=Frame1", 1),
                frame("Trống", "  ", 2),
                frame("Khung thật", "https://cdn.example/frame.png", 3)));
        when(photoFrameAccessService.isFrameFree(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(true);

        var response = controller.list();

        assertThat(response.getData()).extracting(item -> item.name()).containsExactly("Khung thật");
    }

    private static PhotoFrame frame(String name, String imageUrl, int order) {
        return PhotoFrame.builder().id(UUID.randomUUID()).name(name).imageUrl(imageUrl).sortOrder(order).build();
    }
}
