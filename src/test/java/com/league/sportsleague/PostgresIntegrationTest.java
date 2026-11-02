package com.league.sportsleague;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration smoke test: boots the full application context against a real
 * PostgreSQL 17 container managed by Testcontainers.
 *
 * <p>Excluded from the default Surefire run ({@code mvnw test}).
 * Run explicitly with: {@code mvnw verify -Pintegration}
 * Requires Docker Desktop to be running.
 */
@Tag("integration")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PostgresIntegrationTest {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("application context loads with real PostgreSQL 17 container")
    void contextLoadsWithRealPostgres() {
        assertNotNull(jdbcClient, "JdbcClient should be wired");
    }

    @Test
    @DisplayName("Flyway migrations applied: leagues table exists")
    void flywayMigrationsApplied() {
        Integer count = jdbcClient
                .sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'leagues'")
                .query(Integer.class)
                .single();
        assertTrue(count >= 1, "leagues table must exist after Flyway migrations");
    }

    @Test
    @DisplayName("Flyway migrations applied: users table exists (V3)")
    void usersTableExists() {
        Integer count = jdbcClient
                .sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'users'")
                .query(Integer.class)
                .single();
        assertTrue(count >= 1, "users table must exist after V3 migration");
    }
}
