package com.league.sportsleague.infrastructure.persistence;

import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.model.MatchChronicle;
import com.league.sportsleague.domain.repository.ChronicleRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA implementation of {@link ChronicleRepository}. */
public interface JpaChronicleRepository extends JpaRepository<MatchChronicle, UUID>, ChronicleRepository {

    @Override
    Optional<MatchChronicle> findByFixtureIdAndLanguage(UUID fixtureId, Language language);

    @Override
    List<MatchChronicle> findByFixtureId(UUID fixtureId);

    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MatchChronicle c where c.fixtureId = :fixtureId")
    void deleteByFixtureId(@org.springframework.data.repository.query.Param("fixtureId") UUID fixtureId);
}
