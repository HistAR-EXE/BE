package com.histar.be.photoframe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.histar.be.config.HistarOrgProperties;
import com.histar.be.photoframe.entity.PhotoFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhotoFrameAccessServiceTest {

    @Mock
    private HistarOrgProperties histarOrgProperties;

    @Mock
    private HistarOrgProperties.Tier tier;

    private PhotoFrameAccessService service;

    @BeforeEach
    void setUp() {
        when(histarOrgProperties.getTier()).thenReturn(tier);
        service = new PhotoFrameAccessService(histarOrgProperties);
    }

    @Test
    void freeIds_configured_matchByName() {
        when(tier.getFreePhotoFrameIds()).thenReturn("classic,heritage");
        PhotoFrame classic = PhotoFrame.builder().name("Classic").build();
        PhotoFrame premium = PhotoFrame.builder().name("Gold").build();
        assertThat(service.isFrameFree(classic, 0)).isTrue();
        assertThat(service.isFrameFree(premium, 5)).isFalse();
    }

    @Test
    void blankConfig_firstTwoIndexesFree() {
        when(tier.getFreePhotoFrameIds()).thenReturn("");
        PhotoFrame any = PhotoFrame.builder().name("X").build();
        assertThat(service.isFrameFree(any, 0)).isTrue();
        assertThat(service.isFrameFree(any, 1)).isTrue();
        assertThat(service.isFrameFree(any, 2)).isFalse();
    }
}
