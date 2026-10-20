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
 * Builds offline "hành trang" manifests from assets that are published on the media host.
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
        if (site.equals("cu-chi")) {
            return buildCuChi(tier, site, base);
        }

        // Other pilot sites have not published an offline bundle yet. Keep their manifest honest:
        // a single cover is preferable to a list of placeholder URLs that inevitably fail.
        List<PackAssetDto> assets = new ArrayList<>();
        assets.add(asset(base + "/map/hero.jpg", true));
        if (tier.equals("full")) {
            addOfflineFaq(assets, site);
        }
        return new PackManifestResponse(tier, site, Instant.now(clock), List.copyOf(assets));
    }

    private PackManifestResponse buildCuChi(String tier, String site, String base) {
        List<PackAssetDto> assets = new ArrayList<>();
        assets.add(asset(base + "/map/hero.jpg", true));
        assets.add(asset(base + "/scenes/cua-ham-2026.jpg", true));
        assets.add(asset(base + "/scenes/bep-hoang-cam-2026.jpg", true));
        assets.add(asset(base + "/scenes/phong-hop-2026.jpg", true));
        assets.add(asset(base + "/scenes/thong-gio-2026.jpg", true));
        assets.add(asset(base + "/scenes/gieng-2026.jpg", true));

        if (tier.equals("full")) {
            assets.add(asset(base + "/panoramas/duong-vao.jpg", false));
            assets.add(asset(base + "/panoramas/trung-bay-vu-khi.jpg", false));
            assets.add(asset(base + "/panoramas/xe-thiet-giap.jpg", false));
            assets.add(asset(base + "/panoramas/san-le-tuong-niem.jpg", false));
            assets.add(asset(base + "/panoramas/den-tuong-niem.jpg", false));
            assets.add(asset(base + "/artifacts/trung-bay-vu-khi.png", false));
            addOfflineFaq(assets, site);
        }
        return new PackManifestResponse(tier, site, Instant.now(clock), List.copyOf(assets));
    }

    private void addOfflineFaq(List<PackAssetDto> assets, String site) {
        String faqPath = "/api/sites/" + site + "/pack/faq_offline.json";
        assets.add(new PackAssetDto(faqPath, apiBaseUrl + faqPath, null, false));
    }

    private PackAssetDto asset(String path, boolean required) {
        return new PackAssetDto(path, mediaBaseUrl + path, null, required);
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
