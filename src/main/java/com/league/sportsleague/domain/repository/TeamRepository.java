package com.league.sportsleague.domain.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
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
    Page<Team> findAll(Pageable pageable);
    Team save(Team team);
    void delete(Team team);
}
