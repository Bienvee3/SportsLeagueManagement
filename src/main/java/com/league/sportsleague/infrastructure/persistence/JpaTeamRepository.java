package com.league.sportsleague.infrastructure.persistence;

import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.repository.TeamRepository;
import com.league.sportsleague.domain.valueobject.TeamCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA implementation of TeamRepository.
 *
 * findByShortCode(TeamCode) is a derived query. Hibernate uses TeamCodeConverter
 * (autoApply=true) to convert the TeamCode parameter to a String for the WHERE clause.
 */
public interface JpaTeamRepository extends JpaRepository<Team, UUID>, TeamRepository {

    @Override
    Optional<Team> findByShortCode(TeamCode shortCode);
}
