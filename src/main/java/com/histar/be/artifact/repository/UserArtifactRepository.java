package com.histar.be.artifact.repository;

import com.histar.be.artifact.entity.UserArtifact;
import com.histar.be.artifact.entity.UserArtifactId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserArtifactRepository extends JpaRepository<UserArtifact, UserArtifactId> {

    List<UserArtifact> findByUserId(UUID userId);

    boolean existsByUserIdAndArtifactId(UUID userId, UUID artifactId);

    @Query("""
            SELECT CASE WHEN COUNT(ua) > 0 THEN true ELSE false END
            FROM UserArtifact ua, Artifact a
            WHERE ua.artifactId = a.id
              AND ua.userId = :userId
              AND a.locationId = :locationId
              AND a.unlockKey = :unlockKey
            """)
    boolean existsByUserIdAndUnlockKeyAtLocation(
            @Param("userId") UUID userId, @Param("locationId") UUID locationId, @Param("unlockKey") String unlockKey);
}
