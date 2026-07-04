package com.histar.be.location.service;

import com.histar.be.common.exception.BusinessRuleException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationEraValidationService {

    private static final int MIN_ERAS = 3;

    private final JdbcTemplate jdbcTemplate;

    public int countDistinctEras(UUID locationId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM (
                  SELECT DISTINCT tl.era::text AS era_key
                  FROM time_layers tl
                  INNER JOIN photo_scenes ps ON ps.id = tl.scene_id
                  WHERE ps.location_id = ?
                  UNION
                  SELECT DISTINCT pp.year::text
                  FROM photo_pairs pp
                  WHERE pp.location_id = ?
                ) eras
                """,
                Integer.class,
                locationId,
                locationId);
        return count != null ? count : 0;
    }

    public boolean hasMinimumEras(UUID locationId) {
        return countDistinctEras(locationId) >= MIN_ERAS;
    }

    public void ensureMinimumThreeEras(UUID locationId) {
        int count = countDistinctEras(locationId);
        if (count < MIN_ERAS) {
            throw new BusinessRuleException(
                    "Mỗi di tích cần ít nhất 3 thời kỳ (era) theo Value Proposition — hiện có "
                            + count);
        }
    }
}
