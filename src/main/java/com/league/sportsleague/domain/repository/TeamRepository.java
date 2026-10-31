package com.league.sportsleague.domain.repository;

import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.valueobject.TeamCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain contract for Team persistence.
 * Team is an independent entity — not owned by any aggregate root.
 */
public interface TeamRepository {
    Optional<Team> findById(UUID id);
    Optional<Team> findByShortCode(TeamCode shortCode);
    List<Team> findAll();
    Team save(Team team);
}
