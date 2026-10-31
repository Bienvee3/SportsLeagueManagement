package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CorrectResultRequest;
import com.league.sportsleague.application.dto.FixtureResponse;
import com.league.sportsleague.application.dto.RecordMatchEventRequest;
import com.league.sportsleague.application.dto.RecordResultRequest;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.MatchEvent;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.repository.FixtureRepository;
import com.league.sportsleague.domain.repository.TeamRepository;
import com.league.sportsleague.domain.valueobject.Score;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FixtureService {

    private final FixtureRepository fixtureRepository;
    private final TeamRepository teamRepository;

    public FixtureService(FixtureRepository fixtureRepository,
                          TeamRepository teamRepository) {
        this.fixtureRepository = fixtureRepository;
        this.teamRepository = teamRepository;
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    public void recordResult(UUID fixtureId, RecordResultRequest request) {
        Fixture fixture = requireFixture(fixtureId);
        fixture.recordResult(new Score(request.homeScore(), request.awayScore()));
        fixtureRepository.save(fixture);
    }

    public void correctResult(UUID fixtureId, CorrectResultRequest request, String correctedBy) {
        Fixture fixture = requireFixture(fixtureId);
        fixture.correctResult(
                new Score(request.newHomeScore(), request.newAwayScore()),
                request.reason(),
                correctedBy
        );
        fixtureRepository.save(fixture);
    }

    public void postponeFixture(UUID fixtureId, String reason) {
        Fixture fixture = requireFixture(fixtureId);
        fixture.postpone(reason);
        fixtureRepository.save(fixture);
    }

    public void cancelFixture(UUID fixtureId) {
        Fixture fixture = requireFixture(fixtureId);
        fixture.cancel();
        fixtureRepository.save(fixture);
    }

    public void rescheduleFixture(UUID fixtureId, Instant newTime) {
        Fixture fixture = requireFixture(fixtureId);
        fixture.reschedule(newTime);
        fixtureRepository.save(fixture);
    }

    public UUID recordMatchEvent(UUID fixtureId, RecordMatchEventRequest request) {
        Fixture fixture = requireFixture(fixtureId);
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new EntityNotFoundException("Team", request.teamId()));
        MatchEvent event = new MatchEvent(
                fixture,
                team,
                request.eventType(),
                request.matchMinute(),
                request.playerName(),
                request.assistingPlayer(),
                request.notes()
        );
        fixture.addMatchEvent(event);
        fixtureRepository.save(fixture);
        return event.getId();
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FixtureResponse findById(UUID fixtureId) {
        return fixtureRepository.findById(fixtureId)
                .map(FixtureResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Fixture", fixtureId));
    }

    @Transactional(readOnly = true)
    public List<FixtureResponse> findByRound(UUID roundId) {
        return fixtureRepository.findByRoundId(roundId).stream()
                .map(FixtureResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FixtureResponse> findBySeasonWithTeams(UUID seasonId) {
        return fixtureRepository.findFinishedBySeasonWithTeams(seasonId).stream()
                .map(FixtureResponse::from)
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Fixture requireFixture(UUID id) {
        return fixtureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fixture", id));
    }
}
