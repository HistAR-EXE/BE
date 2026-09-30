package com.histar.be.media.controller;

import com.histar.be.common.response.ApiResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expected onsite media paths per pilot site (R2 / VITE_MEDIA_BASE_URL).
 * Admin/FE use this to show “Đang cập nhật nội dung” when files are missing after HEAD probe.
 */
@RestController
@RequestMapping("/api/sites/{siteCode}/media-manifest")
public class MediaManifestController {

    private static final Set<String> PILOT_SITES =
            Set.of("cu-chi", "hoang-thanh-thang-long", "dai-noi-hue");
    private static final String[] STATIONS = {"ST01", "ST02", "ST03", "ST04", "ST05", "ST06"};

    public record MediaAsset(String stationCode, String kind, String path, boolean required) {}

    public record MediaManifest(String siteCode, List<MediaAsset> assets) {}

    @GetMapping
    public ApiResponse<MediaManifest> manifest(@PathVariable String siteCode) {
        String site = siteCode == null ? "" : siteCode.trim().toLowerCase(Locale.ROOT);
        if (!PILOT_SITES.contains(site)) {
            site = "cu-chi";
        }
        List<MediaAsset> assets = new ArrayList<>();
        for (String st : STATIONS) {
            String folder = st.toLowerCase(Locale.ROOT);
            assets.add(new MediaAsset(st, "video", "/media/" + site + "/" + folder + "/video/intro.mp4", true));
            assets.add(new MediaAsset(st, "audio", "/media/" + site + "/" + folder + "/audio/ambient.mp3", true));
        }
        return ApiResponse.ok(new MediaManifest(site, assets));
    }
}
