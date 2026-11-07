package com.league.sportsleague.application.service;

import com.league.sportsleague.domain.ai.ChroniclePort;
import com.league.sportsleague.domain.ai.ChronicleRequest;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.ai.MatchEventSummary;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.MatchChronicle;
import com.league.sportsleague.domain.repository.ChronicleRepository;
import com.league.sportsleague.domain.repository.FixtureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChronicleService {

    private final FixtureRepository fixtureRepository;
    private final ChronicleRepository chronicleRepository;
    private final ChroniclePort chroniclePort;

    public ChronicleService(FixtureRepository fixtureRepository,
                            ChronicleRepository chronicleRepository,
                            ChroniclePort chroniclePort) {
        this.fixtureRepository = fixtureRepository;
        this.chronicleRepository = chronicleRepository;
        this.chroniclePort = chroniclePort;
    }

    /**
     * Writes a new chronicle for the fixture, replacing any stored version (including translations
     * of the previous text, which would otherwise tell a different story).
     *
     * @param language output language (defaults to ENGLISH if null)
     */
    @Transactional
    public ChronicleResult generateChronicle(UUID fixtureId, Language language) {
        Language lang = (language != null) ? language : Language.ENGLISH;
        Fixture fixture = requireFixture(fixtureId);

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

        ChronicleResult result = chroniclePort.generate(request);
        chronicleRepository.deleteByFixtureId(fixtureId);
        chronicleRepository.save(new MatchChronicle(fixtureId, lang, result.text()));
        return result;
    }

    /**
     * Returns the chronicle in the requested language: the stored one, or — when it only exists in
     * another language — a faithful translation of it (stored for next time). Empty when no chronicle
     * has been generated yet; never invents a new story.
     */
    @Transactional
    public Optional<ChronicleResult> findChronicle(UUID fixtureId, Language language) {
        Language lang = (language != null) ? language : Language.ENGLISH;
        requireFixture(fixtureId);

        Optional<MatchChronicle> exact = chronicleRepository.findByFixtureIdAndLanguage(fixtureId, lang);
        if (exact.isPresent()) {
            return exact.map(c -> new ChronicleResult(c.getContent(), c.getLanguage()));
        }
        Optional<MatchChronicle> other = chronicleRepository.findByFixtureId(fixtureId).stream().findFirst();
        if (other.isEmpty()) {
            return Optional.empty();
        }
        ChronicleResult source = new ChronicleResult(other.get().getContent(), other.get().getLanguage());
        ChronicleResult translated = chroniclePort.translate(source, lang);
        chronicleRepository.save(new MatchChronicle(fixtureId, lang, translated.text()));
        return Optional.of(translated);
    }

    private Fixture requireFixture(UUID fixtureId) {
        return fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new EntityNotFoundException("Fixture", fixtureId));
    }
}
