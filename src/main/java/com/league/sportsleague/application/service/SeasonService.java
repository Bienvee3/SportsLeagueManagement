package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CreateSeasonRequest;
import com.league.sportsleague.application.dto.SeasonResponse;
import com.league.sportsleague.application.dto.TeamResponse;
import com.league.sportsleague.application.dto.StandingsResponse;
import com.league.sportsleague.domain.StandingsCalculator;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.RoundRobinScheduler;
import com.league.sportsleague.domain.model.*;
import com.league.sportsleague.domain.model.enums.SeasonTeamStatus;
import com.league.sportsleague.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        SeasonTeam seasonTeam = new SeasonTeam(season, team, initialSeed, SeasonTeamStatus.CONFIRMED);
        season.addSeasonTeam(seasonTeam); // domain guard: throws DuplicateTeamException if already enrolled
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
            throw new IllegalStateException(
                "Schedule already exists (%d rounds). Delete this season and create a new one to regenerate."
                    .formatted(season.getRounds().size())
            );
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
        return requireSeason(seasonId).getSeasonTeams().stream()
                .map(st -> st.getTeam().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Season requireSeason(UUID id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Season", id));
    }
}
