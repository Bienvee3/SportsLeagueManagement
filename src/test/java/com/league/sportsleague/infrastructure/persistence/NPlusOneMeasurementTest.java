package com.league.sportsleague.infrastructure.persistence;

import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.League;
import com.league.sportsleague.domain.model.Round;
import com.league.sportsleague.domain.model.Season;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.model.enums.RoundStatus;
import com.league.sportsleague.domain.model.enums.SeasonStatus;
import com.league.sportsleague.domain.repository.FixtureRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LO4 evidence: the season calendar reads the teams and the round of every match. {@code Fixture.homeTeam},
 * {@code awayTeam} and {@code round} are LAZY on purpose, so a plain query that lists the matches triggers
 * one extra SELECT per distinct team and round (the N+1 problem). The repository method used by the
 * calendar fetches them with {@code JOIN FETCH} in the single query. This test counts the SQL statements
 * Hibernate really prepares for both variants and fails if the fix regresses. The numbers are written to
 * docs/performance.md.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:n_plus_one_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.SQL=DEBUG"
})
class NPlusOneMeasurementTest {

    private static final int TEAMS = 12;
    private static final int ROUNDS = 6;
    private static final int FIXTURES_PER_ROUND = 6;

    @Autowired TestEntityManager em;
    @Autowired FixtureRepository fixtureRepository;

    private UUID seasonId;

    private void seedSeason() {
        League league = em.persist(new League("N1 League", "n1-league", "FOOTBALL", "Croatia"));
        Season season = em.persist(new Season(league, "N1 season", 2030, 2031, SeasonStatus.ACTIVE));
        seasonId = season.getId();
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < TEAMS; i++) {
            teams.add(em.persist(new Team("Team " + i, "T" + (char) ('A' + i) + "X", "Stadium", "City", 2000)));
        }
        for (int r = 1; r <= ROUNDS; r++) {
            Round round = em.persist(new Round(season, r, "Round " + r, LocalDate.now(), LocalDate.now().plusDays(2), RoundStatus.SCHEDULED));
            for (int f = 0; f < FIXTURES_PER_ROUND; f++) {
                Team home = teams.get(f * 2);
                Team away = teams.get(f * 2 + 1);
                em.persist(new Fixture(round, home, away, Instant.parse("2030-08-01T18:00:00Z").plusSeconds(r * 86400L + f * 3600L)));
            }
        }
        em.flush();
        em.clear();
    }

    /** What the calendar page needs from every match: both team names and the round. */
    private static int touchWhatTheCalendarRenders(List<Fixture> fixtures) {
        int chars = 0;
        for (Fixture f : fixtures) {
            chars += f.getHomeTeam().getName().length();
            chars += f.getAwayTeam().getName().length();
            chars += f.getRound().getName().length();
        }
        return chars;
    }

    private long statementsFor(java.util.function.Supplier<List<Fixture>> query, long[] elapsedMs) {
        Statistics stats = em.getEntityManager().getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        em.clear();
        stats.clear();
        long t0 = System.nanoTime();
        List<Fixture> fixtures = query.get();
        touchWhatTheCalendarRenders(fixtures);
        elapsedMs[0] = (System.nanoTime() - t0) / 1_000_000;
        return stats.getPrepareStatementCount();
    }

    @Test
    @DisplayName("listing a season's matches: lazy loading needs one SELECT per team and round, JOIN FETCH needs one in total")
    void joinFetchRemovesTheNPlusOne() {
        seedSeason();
        int fixtures = ROUNDS * FIXTURES_PER_ROUND;

        java.util.function.Supplier<List<Fixture>> lazy = () -> em.getEntityManager()
                .createQuery("SELECT f FROM Fixture f WHERE f.round.season.id = :sid ORDER BY f.scheduledTime", Fixture.class)
                .setParameter("sid", seasonId)
                .getResultList();
        java.util.function.Supplier<List<Fixture>> fetched = () -> fixtureRepository.findAllBySeasonWithTeams(seasonId);
        long[] one = new long[1];

        long before = statementsFor(lazy, one);
        long after = statementsFor(fetched, one);

        // Timing: 5 warm-up runs, then the median of 25 runs of each variant (a single run is dominated by JIT warm-up).
        for (int i = 0; i < 5; i++) { statementsFor(lazy, one); statementsFor(fetched, one); }
        long[] lazyMs = new long[25];
        long[] fetchedMs = new long[25];
        for (int i = 0; i < 25; i++) {
            statementsFor(lazy, one); lazyMs[i] = one[0];
            statementsFor(fetched, one); fetchedMs[i] = one[0];
        }
        java.util.Arrays.sort(lazyMs);
        java.util.Arrays.sort(fetchedMs);

        System.out.printf("N+1-MEASUREMENT fixtures=%d teams=%d rounds=%d | before: %d statements, median %d ms | after: %d statements, median %d ms%n",
                fixtures, TEAMS, ROUNDS, before, lazyMs[12], after, fetchedMs[12]);

        assertEquals(1, after, "the calendar query must load matches, teams and rounds in a single statement");
        assertEquals(1 + TEAMS + ROUNDS, before, "without fetching: 1 query + one SELECT per distinct team and round");
        assertTrue(before > after * 10, "the fix must cut the statements by an order of magnitude here");
    }
}
