package com.league.sportsleague.domain.repository;

import com.league.sportsleague.domain.model.Fixture;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain contract for Fixture persistence.
 * MatchEvent and ResultAudit are managed through Fixture aggregate (CascadeType.ALL).
 * Read queries for events and audits use custom JPQL on the implementation side.
 */
public interface FixtureRepository {
    Optional<Fixture> findById(UUID id);
    List<Fixture> findByRoundId(UUID roundId);

    /**
     * Returns all FINISHED fixtures for a season with homeTeam and awayTeam
     * eagerly loaded via JOIN FETCH to prevent N+1 queries.
     */
    List<Fixture> findFinishedBySeasonWithTeams(UUID seasonId);

    Fixture save(Fixture fixture);
}
