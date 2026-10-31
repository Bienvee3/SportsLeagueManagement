package com.league.sportsleague.domain.repository;

import com.league.sportsleague.domain.model.Season;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain contract for Season persistence.
 * SeasonTeam is managed through Season aggregate (CascadeType.ALL).
 */
public interface SeasonRepository {
    Optional<Season> findById(UUID id);
    List<Season> findByLeagueId(UUID leagueId);
    Season save(Season season);
}
