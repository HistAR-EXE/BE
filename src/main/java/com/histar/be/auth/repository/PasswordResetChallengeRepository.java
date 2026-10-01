package com.histar.be.auth.repository;

import com.histar.be.auth.entity.PasswordResetChallenge;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetChallengeRepository extends JpaRepository<PasswordResetChallenge, UUID> {

    Optional<PasswordResetChallenge> findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<PasswordResetChallenge> findByResetTokenHashAndConsumedAtIsNull(String resetTokenHash);

    List<PasswordResetChallenge> findByUserIdAndConsumedAtIsNull(UUID userId);
}
