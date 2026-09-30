package com.histar.be.rag.repository;

import com.histar.be.rag.entity.RagSource;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RagSourceRepository extends JpaRepository<RagSource, UUID> {}
