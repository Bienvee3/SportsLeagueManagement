package com.league.sportsleague.domain.repository;

import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.model.MatchChronicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Domain contract for stored AI chronicles (at most one per fixture and language). */
public interface ChronicleRepository {
    Optional<MatchChronicle> findByFixtureIdAndLanguage(UUID fixtureId, Language language);
    List<MatchChronicle> findByFixtureId(UUID fixtureId);
    void deleteByFixtureId(UUID fixtureId);
    MatchChronicle save(MatchChronicle chronicle);
}
