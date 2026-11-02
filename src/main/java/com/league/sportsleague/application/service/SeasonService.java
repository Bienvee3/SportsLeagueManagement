package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CreateSeasonRequest;
import com.league.sportsleague.application.dto.SeasonResponse;
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

    public SeasonService(SeasonRepository seasonRepository,
                         LeagueRepository leagueRepository,
                         TeamRepository teamRepository,
                         RoundRepository roundRepository) {
        this.seasonRepository = seasonRepository;
        this.leagueRepository = leagueRepository;
        this.teamRepository = teamRepository;
        this.roundRepository = roundRepository;
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
        List<Team> confirmedTeams = season.getSeasonTeams().stream()
                .filter(st -> st.getStatus() == SeasonTeamStatus.CONFIRMED)
                .map(SeasonTeam::getTeam)
                .toList();
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

    // ── Private helpers ───────────────────────────────────────────────────────

    private Season requireSeason(UUID id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Season", id));
    }
}
