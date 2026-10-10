package com.histar.be.photopair;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PhotoPairSchemaCompatibilityTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsPhotoPairsTableOnH2() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from photo_pairs", Integer.class);

        assertThat(count).isNotNull();
    }
}
