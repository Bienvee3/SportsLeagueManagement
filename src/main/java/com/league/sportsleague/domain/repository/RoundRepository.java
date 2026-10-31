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
    Round save(Round round);
}
