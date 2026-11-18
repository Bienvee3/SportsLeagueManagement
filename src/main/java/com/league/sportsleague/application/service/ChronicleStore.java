package com.league.sportsleague.application.service;

import com.league.sportsleague.domain.ai.ChronicleRequest;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.ai.MatchEventSummary;
import com.league.sportsleague.domain.ai.TeamTableEffect;
import com.league.sportsleague.domain.StandingsCalculator;
import com.league.sportsleague.domain.StandingsEntry;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.MatchChronicle;
import com.league.sportsleague.domain.repository.ChronicleRepository;
import com.league.sportsleague.domain.repository.FixtureRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Short database transactions used by {@link ChronicleService}. They live in their own bean so the
 * (slow, paid) AI call in the service runs outside any transaction — a call through {@code this} would
 * bypass the Spring proxy and keep a pooled connection busy for the whole request to the provider.
 */
@Component
public class ChronicleStore {

    private final FixtureRepository fixtureRepository;
    private final ChronicleRepository chronicleRepository;

    public ChronicleStore(FixtureRepository fixtureRepository, ChronicleRepository chronicleRepository) {
        this.fixtureRepository = fixtureRepository;
        this.chronicleRepository = chronicleRepository;
    }

    /** Reads the fixture and builds the provider request (events are lazy, hence the transaction). */
    @Transactional(readOnly = true)
    public ChronicleRequest buildRequest(UUID fixtureId, Language language) {
        Fixture fixture = requireFixture(fixtureId);
        List<MatchEventSummary> events = fixture.getEvents().stream()
                .map(e -> new MatchEventSummary(
                        e.getMatchMinute().value(),
                        e.getEventType().name(),
                        e.getTeam().getName(),
                        e.getPlayerName()))
                .toList();
        return new ChronicleRequest(
                fixture.getHomeTeam().getName(),
                fixture.getAwayTeam().getName(),
                fixture.getHomeScore() != null ? fixture.getHomeScore() : 0,
                fixture.getAwayScore() != null ? fixture.getAwayScore() : 0,
                events,
                language,
                tableEffects(fixture),
                List.of());
    }

    /**
     * What the match did to the season table for each of its teams: the table computed without this match
     * against the table with it (the standings are always derived, so "before" is just "without"). Empty when
     * the match is not finished, because an unfinished match is not in the table.
     */
    private List<TeamTableEffect> tableEffects(Fixture fixture) {
        UUID seasonId = fixture.getRound().getSeason().getId();
        List<Fixture> finished = fixtureRepository.findFinishedBySeasonWithTeams(seasonId);
        if (finished.stream().noneMatch(f -> f.getId().equals(fixture.getId()))) {
            return List.of();
        }
        List<StandingsEntry> after = StandingsCalculator.calculate(finished);
        List<StandingsEntry> before = StandingsCalculator.calculate(
                finished.stream().filter(f -> !f.getId().equals(fixture.getId())).toList());
        return List.of(effectOf(fixture.getHomeTeam(), before, after), effectOf(fixture.getAwayTeam(), before, after));
    }

    private static TeamTableEffect effectOf(Team team, List<StandingsEntry> before, List<StandingsEntry> after) {
        int posAfter = positionOf(team, after);
        int posBefore = positionOf(team, before);
        return new TeamTableEffect(
                team.getName(),
                posBefore == 0 ? null : posBefore,
                posAfter,
                pointsOf(team, before),
                pointsOf(team, after));
    }

    /** 1-based position of the team in the table, 0 when it is not in it. */
    private static int positionOf(Team team, List<StandingsEntry> table) {
        for (int i = 0; i < table.size(); i++) {
            if (table.get(i).teamId().equals(team.getId())) {
                return i + 1;
            }
        }
        return 0;
    }

    private static int pointsOf(Team team, List<StandingsEntry> table) {
        return table.stream().filter(e -> e.teamId().equals(team.getId())).mapToInt(StandingsEntry::points).findFirst().orElse(0);
    }

    /** The stored chronicle of the fixture in {@code language}, if any. */
    @Transactional(readOnly = true)
    public Optional<ChronicleResult> findStored(UUID fixtureId, Language language) {
        requireFixture(fixtureId);
        return chronicleRepository.findByFixtureIdAndLanguage(fixtureId, language).map(ChronicleStore::toResult);
    }

    /** Any stored chronicle of the fixture (the source to translate from), if there is one. */
    @Transactional(readOnly = true)
    public Optional<ChronicleResult> findAnyStored(UUID fixtureId) {
        requireFixture(fixtureId);
        return chronicleRepository.findByFixtureId(fixtureId).stream().findFirst().map(ChronicleStore::toResult);
    }

    /** Replaces every stored version (translations of the old text tell a different story) with the new one. */
    @Transactional
    public void replace(UUID fixtureId, Language language, String text) {
        chronicleRepository.deleteByFixtureId(fixtureId);
        chronicleRepository.save(new MatchChronicle(fixtureId, language, text));
    }

    /**
     * Stores {@code translation} unless {@code source} is no longer the stored chronicle (it was
     * regenerated while the provider was translating), in which case the translation is stale.
     * A translation another request stored first is kept.
     *
     * @return false when the translation was discarded as stale
     */
    @Transactional
    public boolean saveTranslation(UUID fixtureId, ChronicleResult source, ChronicleResult translation) {
        boolean sourceUnchanged = chronicleRepository.findByFixtureIdAndLanguage(fixtureId, source.language())
                .map(c -> c.getContent().equals(source.text()))
                .orElse(false);
        if (!sourceUnchanged) {
            return false;
        }
        if (chronicleRepository.findByFixtureIdAndLanguage(fixtureId, translation.language()).isEmpty()) {
            chronicleRepository.save(new MatchChronicle(fixtureId, translation.language(), translation.text()));
        }
        return true;
    }

    private Fixture requireFixture(UUID fixtureId) {
        return fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new EntityNotFoundException("Fixture", fixtureId));
    }

    private static ChronicleResult toResult(MatchChronicle c) {
        return new ChronicleResult(c.getContent(), c.getLanguage());
    }
}
