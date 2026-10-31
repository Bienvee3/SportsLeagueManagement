package com.league.sportsleague.infrastructure.persistence;

import com.league.sportsleague.domain.model.League;
import com.league.sportsleague.domain.repository.LeagueRepository;
import com.league.sportsleague.domain.valueobject.Slug;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA implementation of LeagueRepository.
 *
 * Dual inheritance: JpaRepository<League, UUID> provides findById, findAll, save, delete,
 * which satisfy the LeagueRepository contract signatures at runtime.
 *
 * findBySlug(Slug) is a Spring Data derived query. Hibernate uses SlugConverter
 * (autoApply=true) to convert the Slug parameter to a String for the WHERE clause.
 */
public interface JpaLeagueRepository extends JpaRepository<League, UUID>, LeagueRepository {

    @Override
    Optional<League> findBySlug(Slug slug);
}
