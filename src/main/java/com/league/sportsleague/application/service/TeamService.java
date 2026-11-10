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

    private static final java.util.regex.Pattern PLAYER_LINE = java.util.regex.Pattern.compile(
            "^(\\d{1,3})[\\s.:;\\-]+([^,;\\t]+?)\\s*(?:[,;\\t]\\s*(.*?))?\\s*$");

    /**
     * Adds a whole squad from pasted text, one player per line: {@code <number> <name>[, <position>]}
     * (separators {@code ,} {@code ;} or tab). Blank lines are skipped. All players are added or none:
     * the first invalid line aborts the transaction.
     *
     * @return the number of players added
     * @throws IllegalArgumentException naming the offending line
     */
    public int addPlayersFromText(UUID teamId, String text) {
        Team team = requireTeam(teamId);
        String[] lines = text == null ? new String[0] : text.split("\\R");
        int added = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty()) {
                continue;
            }
            java.util.regex.Matcher m = PLAYER_LINE.matcher(line);
            if (!m.matches()) {
                throw new IllegalArgumentException(
                        "Line %d: expected \"<number> <name>, <position>\" but got \"%s\"".formatted(i + 1, line));
            }
            try {
                team.addPlayer(m.group(2), Integer.parseInt(m.group(1)), m.group(3));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Line %d: %s".formatted(i + 1, invalid.getMessage()));
            }
            added++;
        }
        if (added == 0) {
            throw new IllegalArgumentException("Paste at least one player, one per line: \"<number> <name>, <position>\"");
        }
        teamRepository.save(team);
        return added;
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
