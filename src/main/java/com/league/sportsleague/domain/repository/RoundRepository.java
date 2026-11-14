package com.league.sportsleague.domain.repository;

import com.league.sportsleague.domain.model.Round;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain contract for Round persistence.
 * Fixtures are managed through Round aggregate (CascadeType.ALL).
 */
public interface RoundRepository {
    Optional<Round> findById(UUID id);
    List<Round> findBySeasonIdOrderByRoundNumber(UUID seasonId);

    /**
     * Rounds that are not COMPLETED yet and belong to an ACTIVE season, each with its fixtures loaded by
     * JOIN FETCH (the lifecycle job reads every fixture status, so lazy loading would be N+1).
     */
    List<Round> findOpenRoundsOfActiveSeasons();
    Round save(Round round);
}
