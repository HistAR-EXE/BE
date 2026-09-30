package com.histar.be.pack.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.pack.dto.PackAssetDto;
import com.histar.be.pack.dto.PackManifestResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Builds offline "hành trang" manifests. Asset paths are placeholders under {@code /media/{siteCode}/};
 * real files are served from the media host (Vercel or R2, see docs/MEDIA_R2.md).
 */
@Service
public class PackService {

    private static final Pattern SITE_CODE = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");

    private final String mediaBaseUrl;
    private final String apiBaseUrl;
    private final Clock clock;

    @Autowired
    public PackService(
            @Value("${media.base-url:https://fe-lake-five.vercel.app}") String mediaBaseUrl,
            @Value("${pack.api-base-url:}") String apiBaseUrl) {
        this(mediaBaseUrl, apiBaseUrl, Clock.systemUTC());
    }

    PackService(String mediaBaseUrl, Clock clock) {
        this(mediaBaseUrl, "", clock);
    }

    PackService(String mediaBaseUrl, String apiBaseUrl, Clock clock) {
        this.mediaBaseUrl = stripTrailingSlash(mediaBaseUrl == null ? "" : mediaBaseUrl.trim());
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl == null ? "" : apiBaseUrl.trim());
        this.clock = clock;
    }

    public PackManifestResponse build(String siteCode, String tierRaw) {
        String site = siteCode == null ? "" : siteCode.trim().toLowerCase(Locale.ROOT);
        if (!SITE_CODE.matcher(site).matches()) {
            throw new BusinessRuleException("siteCode không hợp lệ");
        }
        String tier = tierRaw == null || tierRaw.isBlank() ? "lite" : tierRaw.trim().toLowerCase(Locale.ROOT);
        if (!tier.equals("lite") && !tier.equals("full")) {
            throw new BusinessRuleException("tier phải là lite hoặc full");
        }

        String base = "/media/" + site;
        List<PackAssetDto> assets = new ArrayList<>();
        assets.add(asset(base + "/map/hero.jpg", true));
        assets.add(asset(base + "/panoramas/thumbs/panorama-1-thumb.jpg", true));
        // Per-station lite thumbs / placeholders for ST01–ST06
        for (int i = 1; i <= 6; i++) {
            String st = "st0" + i;
            assets.add(asset(base + "/" + st + "/video/intro.mp4", false));
            if (tier.equals("full")) {
                assets.add(asset(base + "/" + st + "/audio/ambient.mp3", false));
            }
        }
        if (tier.equals("full")) {
            assets.add(asset(base + "/video/intro.mp4", false));
            assets.add(asset(base + "/audio/narration-vi.mp3", false));
            // B5: offline FAQ served by the BE (chat AI needs connectivity; these answers do not).
            String faqPath = "/api/sites/" + site + "/pack/faq_offline.json";
            assets.add(new PackAssetDto(faqPath, apiBaseUrl + faqPath, null, false));
        }
        return new PackManifestResponse(tier, site, Instant.now(clock), List.copyOf(assets));
    }

    private PackAssetDto asset(String path, boolean required) {
        return new PackAssetDto(path, mediaBaseUrl + path, null, required);
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
