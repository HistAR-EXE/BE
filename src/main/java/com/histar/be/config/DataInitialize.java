package com.histar.be.config;

import com.histar.be.character.service.CharacterService;
import com.histar.be.hotspot.service.HotspotService;
import com.histar.be.location.service.LocationService;
import com.histar.be.panorama.service.PanoramaService;
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
    private final PanoramaService panoramaService;
    private final HotspotService hotspotService;

    @Override
    public void run(ApplicationArguments args) {
        long locations = locationService.count();
        long characters = characterService.count();
        long photoPairs = photoPairService.count();
        long panoramas = panoramaService.count();
        long hotspots = hotspotService.count();

        log.info(
                "TimeLens seed check — locations: {}, characters: {}, photo_pairs: {}, panoramas: {}, hotspots: {}",
                locations,
                characters,
                photoPairs,
                panoramas,
                hotspots);

        if (locations == 0) {
            log.warn("No locations in DB. Run docs/database/TimeLens_DB_Schema.sql or docker compose up -d");
        }
        if (panoramas == 0 || hotspots == 0) {
            log.warn(
                    "Panorama/hotspot seed missing (panoramas={}, hotspots={}). "
                            + "If DB volume existed before week-1 seed: docker compose down -v && docker compose up -d",
                    panoramas,
                    hotspots);
        }
    }
}
