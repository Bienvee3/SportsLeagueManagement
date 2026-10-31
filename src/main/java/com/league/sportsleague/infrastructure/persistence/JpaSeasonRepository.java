package com.league.sportsleague.infrastructure.persistence;

import com.league.sportsleague.domain.model.Season;
import com.league.sportsleague.domain.repository.SeasonRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA implementation of SeasonRepository.
 *
 * findByLeagueId(UUID) is a derived query navigating Season.league.id.
 * Spring Data resolves "LeagueId" as the nested property path league → id.
 */
public interface JpaSeasonRepository extends JpaRepository<Season, UUID>, SeasonRepository {

    @Override
    List<Season> findByLeagueId(UUID leagueId);
}
