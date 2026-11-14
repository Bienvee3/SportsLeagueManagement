package com.league.sportsleague.infrastructure.persistence;

import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.enums.FixtureStatus;
import com.league.sportsleague.domain.repository.FixtureRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA implementation of FixtureRepository.
 *
 * findByRoundId(UUID) navigates Fixture.round.id.
 *
 * findFinishedBySeasonWithTeams delegates to a private parametrized query via
 * a default method, keeping the domain interface signature (no status param)
 * while using a proper @Query with JOIN FETCH to prevent N+1 queries on teams.
 */
public interface JpaFixtureRepository extends JpaRepository<Fixture, UUID>, FixtureRepository {

    @Override
    List<Fixture> findByRoundId(UUID roundId);

    @Query("SELECT f FROM Fixture f " +
           "JOIN FETCH f.homeTeam " +
           "JOIN FETCH f.awayTeam " +
           "JOIN FETCH f.round " +
           "WHERE f.round.season.id = :seasonId " +
           "ORDER BY f.round.roundNumber, f.scheduledTime")
    List<Fixture> findAllBySeasonIdWithTeamsAndRound(@Param("seasonId") UUID seasonId);

    @Override
    default List<Fixture> findAllBySeasonWithTeams(UUID seasonId) {
        return findAllBySeasonIdWithTeamsAndRound(seasonId);
    }

    /**
     * Internal parametrized query used by the default override below.
     * JOIN FETCH ensures homeTeam and awayTeam are loaded in the same SQL query.
     */
    @Query("SELECT f FROM Fixture f " +
           "JOIN FETCH f.homeTeam " +
           "JOIN FETCH f.awayTeam " +
           "WHERE f.round.season.id = :seasonId " +
           "AND f.status = :status")
    List<Fixture> findBySeasonIdAndStatusWithTeams(
            @Param("seasonId") UUID seasonId,
            @Param("status") FixtureStatus status);

    /**
     * Satisfies the FixtureRepository domain contract.
     * Delegates to the JOIN FETCH query always passing FINISHED as the status.
     */
    @Override
    default List<Fixture> findFinishedBySeasonWithTeams(UUID seasonId) {
        return findBySeasonIdAndStatusWithTeams(seasonId, FixtureStatus.FINISHED);
    }

    @Query("SELECT f FROM Fixture f " +
           "JOIN FETCH f.homeTeam " +
           "JOIN FETCH f.awayTeam " +
           "JOIN FETCH f.round " +
           "WHERE f.status = :status AND f.scheduledTime < :cutoff " +
           "ORDER BY f.scheduledTime")
    List<Fixture> findByStatusAndScheduledBeforeWithTeams(
            @Param("status") FixtureStatus status,
            @Param("cutoff") Instant cutoff);

    /** SCHEDULED matches whose kick-off time is before {@code cutoff}; teams and round fetched in one query. */
    @Override
    default List<Fixture> findScheduledBefore(Instant cutoff) {
        return findByStatusAndScheduledBeforeWithTeams(FixtureStatus.SCHEDULED, cutoff);
    }
}
