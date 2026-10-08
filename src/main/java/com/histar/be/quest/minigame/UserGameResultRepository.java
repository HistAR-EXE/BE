package com.histar.be.quest.minigame;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserGameResultRepository extends JpaRepository<UserGameResult, UUID> {

    Optional<UserGameResult> findByUserIdAndGameId(UUID userId, UUID gameId);

    @Modifying(clearAutomatically = true)
    @Query(
            value = """
            INSERT INTO user_game_results (id, user_id, game_id, best_stars, attempts, last_level_id)
            VALUES (:id, :userId, :gameId, :stars, 1, :levelId)
            ON CONFLICT (user_id, game_id) DO UPDATE
            SET best_stars = GREATEST(user_game_results.best_stars, EXCLUDED.best_stars),
                attempts = user_game_results.attempts + 1,
                last_level_id = EXCLUDED.last_level_id
            """,
            nativeQuery = true)
    void upsert(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("gameId") UUID gameId,
            @Param("stars") int stars,
            @Param("levelId") String levelId);
}
