package com.histar.be.photopair.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.photopair.entity.PhotoPair;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PhotoPairRepositoryTest {

    @Autowired
    private PhotoPairRepository photoPairRepository;

    @Test
    void savesAndReadsYearColumnWithH2() {
        PhotoPair saved = photoPairRepository.saveAndFlush(PhotoPair.builder()
                .locationId(UUID.randomUUID())
                .historicalImage("historical.jpg")
                .currentImage("current.jpg")
                .year(1968)
                .caption("Tết Mậu Thân")
                .sortOrder(1)
                .build());

        assertThat(photoPairRepository.findById(saved.getId()))
                .get()
                .extracting(PhotoPair::getYear)
                .isEqualTo(1968);
    }
}
