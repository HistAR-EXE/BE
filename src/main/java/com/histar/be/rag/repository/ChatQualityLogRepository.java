package com.histar.be.rag.repository;

import com.histar.be.rag.entity.ChatQualityLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatQualityLogRepository extends JpaRepository<ChatQualityLog, UUID> {}
