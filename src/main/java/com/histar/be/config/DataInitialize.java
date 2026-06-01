package com.histar.be.config;

import com.histar.be.character.service.CharacterService;
import com.histar.be.location.service.LocationService;
import com.histar.be.photopair.service.PhotoPairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitialize implements ApplicationRunner {

    private final LocationService locationService;
    private final CharacterService characterService;
    private final PhotoPairService photoPairService;

    @Override
    public void run(ApplicationArguments args) {
        log.info(
                "TimeLens seed check — locations: {}, characters: {}, photo_pairs: {}",
                locationService.count(),
                characterService.count(),
                photoPairService.count());
    }
}
