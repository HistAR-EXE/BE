package com.histar.be.userquestprogress.repository;

import com.histar.be.userquestprogress.entity.UserQuestProgress;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuestProgressRepository extends JpaRepository<UserQuestProgress, UUID> {
}
