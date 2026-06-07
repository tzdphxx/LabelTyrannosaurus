package com.labelhub.modules.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AiReviewSchemaReadiness {

    private static final Logger log = LoggerFactory.getLogger(AiReviewSchemaReadiness.class);
    private static final int REQUIRED_TABLE_COUNT = 2;
    private static final String SQL = """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name IN ('submissions', 'ai_review_results')
            """;

    private final JdbcTemplate jdbcTemplate;

    public AiReviewSchemaReadiness(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isReady() {
        try {
            Integer count = jdbcTemplate.queryForObject(SQL, Integer.class);
            return count != null && count == REQUIRED_TABLE_COUNT;
        } catch (DataAccessException e) {
            log.warn("AI review schema readiness check failed; skipping AI startup/background work", e);
            return false;
        }
    }
}
