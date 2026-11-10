package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CreateTeamRequest;
import com.league.sportsleague.application.dto.PlayerRequest;
import com.league.sportsleague.application.dto.PlayerResponse;
import com.league.sportsleague.application.dto.TeamResponse;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.model.Player;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    public UUID createTeam(CreateTeamRequest request) {
        Team team = new Team(
                request.name(),
                request.shortCode(),
                request.sportType(),
                request.stadiumName(),
                request.city(),
                request.foundedYear()
        );
        teamRepository.save(team);
        return team.getId();
    }

    public void updateProfile(UUID id, String name, String stadiumName, String city, Integer foundedYear) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team", id));
        team.updateProfile(name, stadiumName, city, foundedYear);
        teamRepository.save(team);
    }

    public void deleteTeam(UUID id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team", id));
        teamRepository.delete(team);
    }

    public UUID addPlayer(UUID teamId, PlayerRequest request) {
        Team team = requireTeam(teamId);
        Player player = team.addPlayer(request.name(), request.shirtNumber(), request.position());
        teamRepository.save(team);
        return player.getId();
    }

    public void updatePlayer(UUID teamId, UUID playerId, PlayerRequest request) {
        Team team = requireTeam(teamId);
        team.updatePlayer(requireActivePlayer(team, playerId),
                request.name(), request.shirtNumber(), request.position());
        teamRepository.save(team);
    }

    /** Removes the player from the squad; the row is kept (deactivated) for match history. */
    public void removePlayer(UUID teamId, UUID playerId) {
        Team team = requireTeam(teamId);
        team.removePlayer(requireActivePlayer(team, playerId));
        teamRepository.save(team);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TeamResponse findById(UUID id) {
        return teamRepository.findById(id)
                .map(TeamResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Team", id));
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> findAll() {
        return teamRepository.findAll().stream()
                .map(TeamResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> findPlayers(UUID teamId) {
        return requireTeam(teamId).getActivePlayers().stream()
                .map(PlayerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlayerResponse findPlayer(UUID teamId, UUID playerId) {
        return PlayerResponse.from(requireActivePlayer(requireTeam(teamId), playerId));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Team requireTeam(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team", id));
    }

    private Player requireActivePlayer(Team team, UUID playerId) {
        return team.findPlayer(playerId)
                .filter(Player::isActive)
                .orElseThrow(() -> new EntityNotFoundException("Player", playerId));
    }
}
