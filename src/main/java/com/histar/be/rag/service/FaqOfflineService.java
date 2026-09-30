package com.histar.be.rag.service;

import com.histar.be.rag.dto.FaqOfflinePack;
import com.histar.be.rag.repository.FaqOfflineRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FaqOfflineService {

    private final FaqOfflineRepository repository;
    private final Clock clock = Clock.systemUTC();

    @Transactional(readOnly = true)
    public FaqOfflinePack build(String siteCode) {
        String site = siteCode == null ? "" : siteCode.trim().toLowerCase(Locale.ROOT);
        var items = repository.findBySiteCodeOrderByStationCodeAscQuestionAsc(site).stream()
                .map(f -> new FaqOfflinePack.Item(f.getStationCode(), f.getQuestion(), f.getAnswer(), f.getSourceRefs()))
                .toList();
        return new FaqOfflinePack(site, Instant.now(clock), items);
    }
}
