package com.histar.be.rag.repository;

import com.histar.be.rag.entity.StationChatPrompt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationChatPromptRepository extends JpaRepository<StationChatPrompt, UUID> {

    List<StationChatPrompt> findBySiteCodeAndStationCodeAndPersonaOrderBySortOrderAsc(
            String siteCode, String stationCode, String persona);
}
