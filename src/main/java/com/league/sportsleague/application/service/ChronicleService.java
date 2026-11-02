package com.league.sportsleague.application.service;

import com.league.sportsleague.domain.ai.ChroniclePort;
import com.league.sportsleague.domain.ai.ChronicleRequest;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.ai.MatchEventSummary;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.repository.FixtureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ChronicleService {

    private final FixtureRepository fixtureRepository;
    private final ChroniclePort chroniclePort;

    public ChronicleService(FixtureRepository fixtureRepository, ChroniclePort chroniclePort) {
        this.fixtureRepository = fixtureRepository;
        this.chroniclePort = chroniclePort;
    }

    /**
     * Generates a post-match chronicle for the given fixture.
     *
     * @param fixtureId UUID of a FINISHED fixture
     * @param language  output language (defaults to ENGLISH if null)
     */
    @Transactional(readOnly = true)
    public ChronicleResult generateChronicle(UUID fixtureId, Language language) {
        Language lang = (language != null) ? language : Language.ENGLISH;
        Fixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new EntityNotFoundException("Fixture", fixtureId));

        List<MatchEventSummary> events = fixture.getEvents().stream()
                .map(e -> new MatchEventSummary(
                        e.getMatchMinute().value(),
                        e.getEventType().name(),
                        e.getTeam().getName(),
                        e.getPlayerName()))
                .toList();

        ChronicleRequest request = new ChronicleRequest(
                fixture.getHomeTeam().getName(),
                fixture.getAwayTeam().getName(),
                fixture.getHomeScore() != null ? fixture.getHomeScore() : 0,
                fixture.getAwayScore() != null ? fixture.getAwayScore() : 0,
                events,
                lang);

        return chroniclePort.generate(request);
    }
}
