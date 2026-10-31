package com.league.sportsleague.infrastructure.persistence;

import com.league.sportsleague.domain.model.Round;
import com.league.sportsleague.domain.repository.RoundRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA implementation of RoundRepository.
 *
 * findBySeasonIdOrderByRoundNumber(UUID) navigates Round.season.id,
 * ordered by Round.roundNumber ascending.
 */
public interface JpaRoundRepository extends JpaRepository<Round, UUID>, RoundRepository {

    @Override
    List<Round> findBySeasonIdOrderByRoundNumber(UUID seasonId);
}
