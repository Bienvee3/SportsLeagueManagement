package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CorrectResultRequest;
import com.league.sportsleague.application.dto.FixtureResponse;
import com.league.sportsleague.application.dto.MatchSheetResponse;
import com.league.sportsleague.application.dto.MatchSheetResponse.LineupPlayer;
import com.league.sportsleague.application.dto.MatchSheetResponse.SquadOption;
import com.league.sportsleague.application.dto.MatchSheetResponse.TeamSheet;
import com.league.sportsleague.application.dto.RecordMatchEventRequest;
import com.league.sportsleague.application.dto.RecordResultRequest;
import com.league.sportsleague.application.dto.SubmitLineupRequest;
import com.league.sportsleague.domain.LineupRules;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.LineupEntry;
import com.league.sportsleague.domain.model.MatchEvent;
import com.league.sportsleague.domain.model.Player;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.repository.ChronicleRepository;
import com.league.sportsleague.domain.repository.FixtureRepository;
import com.league.sportsleague.domain.repository.TeamRepository;
import com.league.sportsleague.domain.valueobject.Score;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class FixtureService {

    private final FixtureRepository fixtureRepository;
    private final TeamRepository teamRepository;
    private final ChronicleRepository chronicleRepository;

    public FixtureService(FixtureRepository fixtureRepository,
                          TeamRepository teamRepository,
                          ChronicleRepository chronicleRepository) {
        this.fixtureRepository = fixtureRepository;
        this.teamRepository = teamRepository;
        this.chronicleRepository = chronicleRepository;
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    public void recordResult(UUID fixtureId, RecordResultRequest request) {
        Fixture fixture = requireFixture(fixtureId);
        fixture.recordResult(new Score(request.homeScore(), request.awayScore()));
        fixtureRepository.save(fixture);
        chronicleRepository.deleteByFixtureId(fixtureId);
    }

    public void correctResult(UUID fixtureId, CorrectResultRequest request, String correctedBy) {
        Fixture fixture = requireFixture(fixtureId);
        fixture.correctResult(
                new Score(request.newHomeScore(), request.newAwayScore()),
                request.reason(),
                correctedBy
        );
        fixtureRepository.save(fixture);
        chronicleRepository.deleteByFixtureId(fixtureId); // stored chronicles describe the old result
    }

    /** Starts the match: SCHEDULED → IN_PROGRESS. */
    public void kickOff(UUID fixtureId) {
        Fixture fixture = requireFixture(fixtureId);
        fixture.kickOff();
        fixtureRepository.save(fixture);
    }

    /** Final whistle: IN_PROGRESS → FINISHED with the score reached through the goal events. */
    public void finish(UUID fixtureId) {
        Fixture fixture = requireFixture(fixtureId);
        fixture.finish();
        fixtureRepository.save(fixture);
        chronicleRepository.deleteByFixtureId(fixtureId);
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

    /**
     * Records a match event. With {@code playerId} the event is linked to that squad player (and
     * {@code relatedPlayerId} to the assist provider or the player coming on); otherwise the free-text
     * {@code playerName} is used, which is only accepted while the team has no lineup in this fixture.
     */
    public UUID recordMatchEvent(UUID fixtureId, RecordMatchEventRequest request) {
        Fixture fixture = requireFixture(fixtureId);
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new EntityNotFoundException("Team", request.teamId()));
        MatchEvent event;
        if (request.playerId() != null) {
            Player player = requirePlayer(team, request.playerId());
            Player related = request.relatedPlayerId() == null
                    ? null : requirePlayer(team, request.relatedPlayerId());
            event = new MatchEvent(fixture, team, request.eventType(), request.matchMinute(),
                    player, related, request.notes());
        } else {
            if (request.playerName() == null || request.playerName().isBlank()) {
                throw new IllegalArgumentException("A player must be selected or named");
            }
            event = new MatchEvent(fixture, team, request.eventType(), request.matchMinute(),
                    request.playerName(), request.assistingPlayer(), request.notes());
        }
        fixture.addMatchEvent(event);
        fixtureRepository.save(fixture);
        chronicleRepository.deleteByFixtureId(fixtureId);
        return event.getId();
    }

    /** Removes an event recorded by mistake; stored AI chronicles described it, so they are discarded. */
    public void deleteMatchEvent(UUID fixtureId, UUID eventId) {
        Fixture fixture = requireFixture(fixtureId);
        MatchEvent event = fixture.getEvents().stream()
                .filter(e -> e.getId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("MatchEvent", eventId));
        fixture.removeMatchEvent(event);
        fixtureRepository.save(fixture);
        chronicleRepository.deleteByFixtureId(fixtureId);
    }

    /** Files or replaces the lineup of one of the two teams; only possible before kick-off. */
    public void submitLineup(UUID fixtureId, UUID teamId, SubmitLineupRequest request) {
        Fixture fixture = requireFixture(fixtureId);
        Team team = requireFixtureTeam(fixture, teamId);
        List<Player> starters = request.starterIds().stream().map(id -> requirePlayer(team, id)).toList();
        List<Player> bench = request.benchIds().stream().map(id -> requirePlayer(team, id)).toList();
        Player captain = request.captainId() == null ? null : requirePlayer(team, request.captainId());
        fixture.submitLineup(team, starters, bench, captain);
        fixtureRepository.save(fixture);
    }

    /** Withdraws the lineup of one of the two teams; only possible before kick-off. */
    public void clearLineup(UUID fixtureId, UUID teamId) {
        Fixture fixture = requireFixture(fixtureId);
        fixture.clearLineup(requireFixtureTeam(fixture, teamId));
        fixtureRepository.save(fixture);
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

    @Transactional(readOnly = true)
    public List<FixtureResponse> findAllBySeason(UUID seasonId) {
        return fixtureRepository.findAllBySeasonWithTeams(seasonId).stream()
                .map(FixtureResponse::from)
                .toList();
    }

    /** The match sheet: both lineups plus the squads they are picked from. */
    @Transactional(readOnly = true)
    public MatchSheetResponse getMatchSheet(UUID fixtureId) {
        Fixture fixture = requireFixture(fixtureId);
        LineupRules rules = fixture.getLineupRules();
        return new MatchSheetResponse(
                rules.starters(),
                rules.maxBench(),
                teamSheet(fixture, fixture.getHomeTeam()),
                teamSheet(fixture, fixture.getAwayTeam()));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Fixture requireFixture(UUID id) {
        return fixtureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fixture", id));
    }

    private Team requireFixtureTeam(Fixture fixture, UUID teamId) {
        if (fixture.getHomeTeam().getId().equals(teamId)) return fixture.getHomeTeam();
        if (fixture.getAwayTeam().getId().equals(teamId)) return fixture.getAwayTeam();
        throw new IllegalArgumentException("Team does not play in this fixture");
    }

    private Player requirePlayer(Team team, UUID playerId) {
        return team.findPlayer(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player", playerId));
    }

    private TeamSheet teamSheet(Fixture fixture, Team team) {
        Map<UUID, LineupEntry> entryByPlayer = fixture.getLineup(team).stream()
                .collect(Collectors.toMap(entry -> entry.getPlayer().getId(), Function.identity()));
        Comparator<LineupEntry> byShirtNumber = Comparator.comparingInt(entry -> entry.getPlayer().getShirtNumber());
        List<LineupPlayer> starters = entryByPlayer.values().stream()
                .filter(LineupEntry::isStarter)
                .sorted(byShirtNumber)
                .map(FixtureService::lineupPlayer)
                .toList();
        List<LineupPlayer> bench = entryByPlayer.values().stream()
                .filter(entry -> !entry.isStarter())
                .sorted(byShirtNumber)
                .map(FixtureService::lineupPlayer)
                .toList();
        List<SquadOption> squad = team.getActivePlayers().stream()
                .map(player -> {
                    LineupEntry entry = entryByPlayer.get(player.getId());
                    return new SquadOption(
                            player.getId(),
                            player.getShirtNumber(),
                            player.getName(),
                            player.getPlayingPosition(),
                            entry == null ? null : entry.getLineupRole().name(),
                            entry != null && entry.isCaptain(),
                            null);
                })
                .toList();
        List<SquadOption> suspended = squad.stream()
                .filter(option -> option.suspensionReason() != null)
                .toList();
        return new TeamSheet(team.getId(), team.getName(), !entryByPlayer.isEmpty(),
                starters, bench, squad, suspended);
    }

    private static LineupPlayer lineupPlayer(LineupEntry entry) {
        Player player = entry.getPlayer();
        return new LineupPlayer(player.getId(), player.getShirtNumber(), player.getName(),
                player.getPlayingPosition(), entry.isCaptain());
    }
}
