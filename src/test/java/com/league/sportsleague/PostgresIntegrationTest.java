package com.league.sportsleague;

import com.league.sportsleague.application.dto.CorrectResultRequest;
import com.league.sportsleague.application.dto.StandingsResponse;
import com.league.sportsleague.application.service.FixtureService;
import com.league.sportsleague.application.service.SeasonService;
import com.league.sportsleague.domain.StandingsEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test: boots the full application against a real PostgreSQL 17 container managed by
 * Testcontainers. Hibernate runs in {@code validate} mode, so a context that loads proves the entities match
 * the schema the Flyway migrations built on the real engine; the other tests exercise the services on it.
 *
 * <p>Excluded from the default Surefire run ({@code mvnw test}).
 * Run explicitly with: {@code mvnw verify -Pintegration} (Docker must be running).
 */
@Tag("integration")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
// The test application.yml (H2) forces the H2 dialect; on a PostgreSQL container Hibernate must use its own.
@TestPropertySource(properties = "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect")
class PostgresIntegrationTest {

    private static final UUID SEASON_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID DERBY_ID = UUID.fromString("66666666-6666-6666-6666-666666666601");

    @Autowired JdbcClient jdbcClient;
    @Autowired SeasonService seasonService;
    @Autowired FixtureService fixtureService;

    @Test
    @DisplayName("application context loads with real PostgreSQL 17 container")
    void contextLoadsWithRealPostgres() {
        assertNotNull(jdbcClient, "JdbcClient should be wired");
    }

    @Test
    @DisplayName("Flyway applied every migration, including the Quartz tables")
    void flywayMigrationsApplied() {
        Integer applied = jdbcClient
                .sql("SELECT COUNT(*) FROM flyway_schema_history WHERE success")
                .query(Integer.class).single();
        Integer quartzTables = jdbcClient
                .sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_name LIKE 'qrtz\\_%'")
                .query(Integer.class).single();

        assertTrue(applied >= 11, "V1 to V11 must be applied, found " + applied);
        assertEquals(11, quartzTables, "the 11 standard Quartz tables must exist (V11)");
    }

    @Test
    @DisplayName("the seeded users table exists (V3)")
    void usersTableExists() {
        Integer count = jdbcClient
                .sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'users'")
                .query(Integer.class)
                .single();
        assertTrue(count >= 1, "users table must exist after V3 migration");
    }

    @Test
    @Transactional
    @DisplayName("standings are derived from results, so a late correction reorders the table and is audited")
    void lateCorrectionReordersTheTableAndLeavesAnAudit() {
        List<String> before = standingsOrder();
        assertEquals("GNK Dinamo Zagreb", before.get(0), "the seeded 2-1 derby win puts Dinamo on top");

        fixtureService.correctResult(DERBY_ID, new CorrectResultRequest(0, 3, "Result corrected weeks later"), "referee");

        List<String> after = standingsOrder();
        assertEquals("HNK Hajduk Split", after.get(0), "after 0-3 the visiting team has the three points");
        Integer audits = jdbcClient
                .sql("SELECT COUNT(*) FROM result_audits WHERE fixture_id = :id AND old_home_score = 2 AND new_away_score = 3")
                .param("id", DERBY_ID)
                .query(Integer.class).single();
        assertEquals(1, audits, "the correction keeps an audit row with the old and the new score");
    }

    private List<String> standingsOrder() {
        StandingsResponse standings = seasonService.getStandings(SEASON_ID);
        return standings.entries().stream().map(StandingsEntry::teamName).toList();
    }
}
