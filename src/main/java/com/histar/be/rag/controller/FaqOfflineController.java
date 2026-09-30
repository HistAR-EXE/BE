package com.histar.be.rag.controller;

import com.histar.be.rag.dto.FaqOfflinePack;
import com.histar.be.rag.service.FaqOfflineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serves {@code faq_offline.json}, listed as an optional asset of the full offline pack. Plain JSON (no envelope). */
@RestController
@RequestMapping("/api/sites/{siteCode}/pack")
@RequiredArgsConstructor
public class FaqOfflineController {

    private final FaqOfflineService faqOfflineService;

    @GetMapping("/faq_offline.json")
    public FaqOfflinePack faqOffline(@PathVariable String siteCode) {
        return faqOfflineService.build(siteCode);
    }
}
