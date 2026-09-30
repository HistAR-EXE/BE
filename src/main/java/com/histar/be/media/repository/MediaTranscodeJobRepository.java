package com.histar.be.media.repository;

import com.histar.be.media.entity.MediaTranscodeJob;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaTranscodeJobRepository extends JpaRepository<MediaTranscodeJob, UUID> {

    List<MediaTranscodeJob> findTop5ByStatusOrderByCreatedAtAsc(String status);
}
