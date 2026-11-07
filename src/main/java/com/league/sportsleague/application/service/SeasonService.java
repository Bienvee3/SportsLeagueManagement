package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CreateSeasonRequest;
import com.league.sportsleague.application.dto.SeasonResponse;
import com.league.sportsleague.application.dto.TeamResponse;
import com.league.sportsleague.application.dto.RoundResponse;
import com.league.sportsleague.application.dto.StandingsResponse;
import com.league.sportsleague.domain.StandingsCalculator;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.RoundRobinScheduler;
import com.league.sportsleague.domain.model.*;
import com.league.sportsleague.domain.model.enums.SeasonStatus;
import com.league.sportsleague.domain.model.enums.SeasonTeamStatus;
import com.league.sportsleague.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final FixtureRepository fixtureRepository;

    public SeasonService(SeasonRepository seasonRepository,
                         LeagueRepository leagueRepository,
                         TeamRepository teamRepository,
                         RoundRepository roundRepository,
                         FixtureRepository fixtureRepository) {
        this.seasonRepository = seasonRepository;
        this.leagueRepository = leagueRepository;
        this.teamRepository = teamRepository;
        this.roundRepository = roundRepository;
        this.fixtureRepository = fixtureRepository;
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    public UUID createSeason(UUID leagueId, CreateSeasonRequest request) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new EntityNotFoundException("League", leagueId));
        Season season = new Season(league, request.name(), request.yearStart(), request.yearEnd(), null);
        seasonRepository.save(season);
        return season.getId();
    }

    public void activateSeason(UUID id) {
        Season season = requireSeason(id);
        season.activate();
        seasonRepository.save(season);
    }

    public void completeSeason(UUID id) {
        Season season = requireSeason(id);
        season.complete();
        seasonRepository.save(season);
    }

    public void deleteSeason(UUID id) {
        Season season = requireSeason(id);
        seasonRepository.delete(season);
    }

    public void registerTeam(UUID seasonId, UUID teamId, int initialSeed) {
        Season season = requireSeason(seasonId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team", teamId));
        String leagueSport = season.getLeague().getSportType();
        if (!team.getSportType().equalsIgnoreCase(leagueSport)) {
            throw new IllegalArgumentException(
                    "%s plays %s but this league is %s.".formatted(
                            team.getName(), team.getSportType(), leagueSport));
        }
        // If the team was previously withdrawn, reinstate its existing entry instead of
        // creating a new row (uk_season_teams_season_team prevents duplicate inserts).
        season.getSeasonTeams().stream()
                .filter(st -> st.getTeam().getId().equals(teamId)
                           && st.getStatus() == SeasonTeamStatus.WITHDRAWN)
                .findFirst()
                .ifPresentOrElse(
                        st -> st.reinstate(),
                        () -> season.addSeasonTeam(new SeasonTeam(season, team, initialSeed, SeasonTeamStatus.CONFIRMED))
                );
        seasonRepository.save(season);
    }

    public void withdrawTeam(UUID seasonId, UUID teamId) {
        Season season = requireSeason(seasonId);
        SeasonTeam seasonTeam = season.getSeasonTeams().stream()
                .filter(st -> st.getTeam().getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("SeasonTeam", teamId));
        seasonTeam.withdraw();
        seasonRepository.save(season);
    }

    public void completeRound(UUID roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new EntityNotFoundException("Round", roundId));
        round.completeRound();
        roundRepository.save(round);
    }

    public void generateSchedule(UUID seasonId, Instant firstMatchTime) {
        Season season = requireSeason(seasonId);
        if (!season.getRounds().isEmpty()) {
            season.clearRounds();
            seasonRepository.flush(); // force DELETEs before INSERTs to avoid uk_rounds_season_round_number violation
        }
        List<Team> confirmedTeams = season.getSeasonTeams().stream()
                .filter(st -> st.getStatus() == SeasonTeamStatus.CONFIRMED)
                .map(SeasonTeam::getTeam)
                .toList();
        if (confirmedTeams.size() < 2) {
            throw new IllegalStateException(
                "Cannot generate schedule: need at least 2 confirmed teams, found %d."
                    .formatted(confirmedTeams.size())
            );
        }
        RoundRobinScheduler.generateSchedule(season, confirmedTeams, firstMatchTime)
                .forEach(season::addRound);
        seasonRepository.save(season);
    }

    /** Adds the next numbered round (empty) to a season; matches are then added one by one. */
    public UUID createRound(UUID seasonId, String name, LocalDate startDate) {
        Season season = requireSeason(seasonId);
        requireOpen(season);
        int next = season.getRounds().stream().mapToInt(Round::getRoundNumber).max().orElse(0) + 1;
        String roundName = (name == null || name.isBlank()) ? "Round " + next : name.trim();
        Round round = new Round(season, next, roundName, startDate, null, null);
        season.addRound(round);
        seasonRepository.save(season);
        return round.getId();
    }

    /** Adds one match to a round; both teams must be CONFIRMED in the season and free in that round. */
    public void addFixture(UUID seasonId, UUID roundId, UUID homeTeamId, UUID awayTeamId, Instant scheduledTime) {
        Season season = requireSeason(seasonId);
        requireOpen(season);
        Round round = season.getRounds().stream()
                .filter(r -> r.getId().equals(roundId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Round", roundId));
        Team home = requireConfirmedTeam(season, homeTeamId);
        Team away = requireConfirmedTeam(season, awayTeamId);
        boolean busy = round.getFixtures().stream().anyMatch(f ->
                f.getHomeTeam().getId().equals(home.getId()) || f.getAwayTeam().getId().equals(home.getId())
                || f.getHomeTeam().getId().equals(away.getId()) || f.getAwayTeam().getId().equals(away.getId()));
        if (busy) {
            throw new IllegalArgumentException("A selected team already plays in " + round.getName() + ".");
        }
        round.addFixture(new Fixture(round, home, away, scheduledTime));
        seasonRepository.save(season);
    }

    @Transactional(readOnly = true)
    public List<RoundResponse> findRounds(UUID seasonId) {
        return requireSeason(seasonId).getRounds().stream()
                .sorted(java.util.Comparator.comparingInt(Round::getRoundNumber))
                .map(RoundResponse::from)
                .toList();
    }

    /** Removes a match that has not been played; FINISHED matches carry results and cannot be deleted. */
    public void deleteFixture(UUID seasonId, UUID roundId, UUID fixtureId) {
        Season season = requireSeason(seasonId);
        Round round = requireRound(season, roundId);
        Fixture fixture = round.getFixtures().stream()
                .filter(f -> f.getId().equals(fixtureId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Fixture", fixtureId));
        if (fixture.isFinished()) {
            throw new IllegalStateException("A finished match cannot be deleted; correct its result instead.");
        }
        round.removeFixture(fixture);
        seasonRepository.save(season);
    }

    /** Removes a round with its unplayed matches; refused if any match in it is FINISHED. */
    public void deleteRound(UUID seasonId, UUID roundId) {
        Season season = requireSeason(seasonId);
        Round round = requireRound(season, roundId);
        if (round.getFixtures().stream().anyMatch(Fixture::isFinished)) {
            throw new IllegalStateException(
                    round.getName() + " has finished matches and cannot be deleted.");
        }
        season.removeRound(round);
        seasonRepository.save(season);
    }

    private Round requireRound(Season season, UUID roundId) {
        return season.getRounds().stream()
                .filter(r -> r.getId().equals(roundId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Round", roundId));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SeasonResponse findById(UUID id) {
        return seasonRepository.findById(id)
                .map(SeasonResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Season", id));
    }

    @Transactional(readOnly = true)
    public List<SeasonResponse> findByLeague(UUID leagueId) {
        return seasonRepository.findByLeagueId(leagueId).stream()
                .map(SeasonResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StandingsResponse getStandings(UUID seasonId) {
        Season season = requireSeason(seasonId);
        var fixtures = fixtureRepository.findFinishedBySeasonWithTeams(seasonId);
        var entries  = StandingsCalculator.calculate(fixtures);
        return new StandingsResponse(season.getId(), season.getName(), Instant.now(), entries);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> findRegisteredTeams(UUID seasonId) {
        return requireSeason(seasonId).getSeasonTeams().stream()
                .filter(st -> st.getStatus() == SeasonTeamStatus.CONFIRMED)
                .map(st -> TeamResponse.from(st.getTeam()))
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.Set<UUID> findAllSeasonTeamIds(UUID seasonId) {
        // Exclude CONFIRMED and DISQUALIFIED — WITHDRAWN teams appear in the dropdown
        // so the admin can re-register (reinstate) them.
        return requireSeason(seasonId).getSeasonTeams().stream()
                .filter(st -> st.getStatus() != SeasonTeamStatus.WITHDRAWN)
                .map(st -> st.getTeam().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Season requireSeason(UUID id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Season", id));
    }

    private void requireOpen(Season season) {
        if (season.getStatus() == SeasonStatus.COMPLETED) {
            throw new IllegalStateException("The season is completed; rounds and matches can no longer be added.");
        }
    }

    private Team requireConfirmedTeam(Season season, UUID teamId) {
        return season.getSeasonTeams().stream()
                .filter(st -> st.getStatus() == SeasonTeamStatus.CONFIRMED)
                .map(SeasonTeam::getTeam)
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Team is not a confirmed participant of this season."));
    }
}
