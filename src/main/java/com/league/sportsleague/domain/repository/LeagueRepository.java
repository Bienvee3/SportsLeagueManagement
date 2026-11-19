package com.league.sportsleague.domain.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.league.sportsleague.domain.model.League;
import com.league.sportsleague.domain.valueobject.Slug;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain contract for League persistence.
 * No Spring or JPA imports — application services depend only on this interface.
 */
public interface LeagueRepository {
    Optional<League> findById(UUID id);
    Optional<League> findBySlug(Slug slug);
    List<League> findAll();
    Page<League> findAll(Pageable pageable);
    League save(League league);
    void delete(League league);
}
