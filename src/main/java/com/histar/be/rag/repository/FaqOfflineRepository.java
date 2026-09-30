package com.histar.be.rag.repository;

import com.histar.be.rag.entity.FaqOffline;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqOfflineRepository extends JpaRepository<FaqOffline, UUID> {

    List<FaqOffline> findBySiteCodeOrderByStationCodeAscQuestionAsc(String siteCode);
}
