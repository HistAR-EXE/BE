package com.histar.be.story.repository;

import com.histar.be.story.entity.StoryChapter;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryChapterRepository extends JpaRepository<StoryChapter, UUID> {

    List<StoryChapter> findBySiteCodeOrderBySortOrderAscChapterNumberAsc(String siteCode);
}
