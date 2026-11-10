package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CreateTeamRequest;
import com.league.sportsleague.application.dto.PlayerRequest;
import com.league.sportsleague.application.dto.PlayerResponse;
import com.league.sportsleague.application.dto.TeamResponse;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.model.Player;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock TeamRepository teamRepository;
    @InjectMocks TeamService teamService;

    private Team sampleTeam;

    @BeforeEach
    void setUp() {
        sampleTeam = new Team("GNK Dinamo Zagreb", "DIN", "Maksimir", "Zagreb", 1911);
    }

    @Test
    @DisplayName("createTeam saves a Team and returns its UUID")
    void createTeamSavesAndReturnsId() {
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID id = teamService.createTeam(
                new CreateTeamRequest("GNK Dinamo Zagreb", "DIN", "FOOTBALL", "Maksimir", "Zagreb", 1911));

        assertNotNull(id);
        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(captor.capture());
        assertEquals("DIN", captor.getValue().getShortCode().value());
    }

    @Test
    @DisplayName("createTeam stores the chosen sport on the team")
    void createTeamPersistsSportType() {
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        teamService.createTeam(
                new CreateTeamRequest("Cibona", "CIB", "basketball", "Drazen Petrovic Hall", "Zagreb", 1946));

        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(captor.capture());
        assertEquals("BASKETBALL", captor.getValue().getSportType());
    }

    @Test
    @DisplayName("updateProfile calls team.updateProfile() and saves")
    void updateProfileCallsDomainMethodAndSaves() {
        UUID id = sampleTeam.getId();
        when(teamRepository.findById(id)).thenReturn(Optional.of(sampleTeam));
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        teamService.updateProfile(id, "New Name", "New Stadium", "Split", 2000);

        assertEquals("New Name", sampleTeam.getName());
        verify(teamRepository).save(sampleTeam);
    }

    @Test
    @DisplayName("updateProfile throws EntityNotFoundException when team not found")
    void updateProfileThrowsWhenNotFound() {
        when(teamRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> teamService.updateProfile(UUID.randomUUID(), "N", "S", "C", null));
    }

    @Test
    @DisplayName("findById returns mapped TeamResponse")
    void findByIdReturnsResponse() {
        UUID id = sampleTeam.getId();
        when(teamRepository.findById(id)).thenReturn(Optional.of(sampleTeam));

        TeamResponse response = teamService.findById(id);

        assertEquals(id, response.id());
        assertEquals("DIN", response.shortCode());
    }

    @Test
    @DisplayName("findById throws EntityNotFoundException when not found")
    void findByIdThrowsWhenNotFound() {
        when(teamRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> teamService.findById(UUID.randomUUID()));
    }

    @Test
    @DisplayName("findAll returns list of mapped TeamResponse")
    void findAllReturnsResponses() {
        when(teamRepository.findAll()).thenReturn(List.of(sampleTeam));

        List<TeamResponse> all = teamService.findAll();

        assertEquals(1, all.size());
        assertEquals("DIN", all.get(0).shortCode());
    }

    // ── Squad ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addPlayer adds the player to the squad, saves and returns its id")
    void addPlayerAddsAndSaves() {
        UUID teamId = sampleTeam.getId();
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(sampleTeam));

        UUID playerId = teamService.addPlayer(teamId, new PlayerRequest("Bruno Petkovic", 9, "Forward"));

        Player stored = sampleTeam.findPlayer(playerId).orElseThrow();
        assertEquals("Bruno Petkovic", stored.getName());
        assertEquals(9, stored.getShirtNumber());
        verify(teamRepository).save(sampleTeam);
    }

    @Test
    @DisplayName("addPlayer throws EntityNotFoundException for an unknown team")
    void addPlayerThrowsWhenTeamNotFound() {
        when(teamRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> teamService.addPlayer(UUID.randomUUID(), new PlayerRequest("Player", 9, null)));
    }

    @Test
    @DisplayName("addPlayer with a taken shirt number is rejected and nothing is saved")
    void addPlayerDuplicateNumberNotSaved() {
        sampleTeam.addPlayer("Bruno Petkovic", 9, "Forward");
        when(teamRepository.findById(sampleTeam.getId())).thenReturn(Optional.of(sampleTeam));

        assertThrows(IllegalArgumentException.class,
                () -> teamService.addPlayer(sampleTeam.getId(), new PlayerRequest("Other", 9, null)));

        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePlayer changes the player and saves")
    void updatePlayerChangesAndSaves() {
        Player player = sampleTeam.addPlayer("Bruno Petkovic", 9, "Forward");
        when(teamRepository.findById(sampleTeam.getId())).thenReturn(Optional.of(sampleTeam));

        teamService.updatePlayer(sampleTeam.getId(), player.getId(),
                new PlayerRequest("Bruno Petković", 21, "Striker"));

        assertEquals("Bruno Petković", player.getName());
        assertEquals(21, player.getShirtNumber());
        assertEquals("Striker", player.getPlayingPosition());
        verify(teamRepository).save(sampleTeam);
    }

    @Test
    @DisplayName("updatePlayer throws EntityNotFoundException for an unknown player")
    void updatePlayerThrowsWhenPlayerNotFound() {
        when(teamRepository.findById(sampleTeam.getId())).thenReturn(Optional.of(sampleTeam));

        assertThrows(EntityNotFoundException.class,
                () -> teamService.updatePlayer(sampleTeam.getId(), UUID.randomUUID(),
                        new PlayerRequest("Player", 9, null)));

        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("removePlayer deactivates the player; a second removal reports it as not found")
    void removePlayerDeactivatesOnce() {
        Player player = sampleTeam.addPlayer("Bruno Petkovic", 9, "Forward");
        when(teamRepository.findById(sampleTeam.getId())).thenReturn(Optional.of(sampleTeam));

        teamService.removePlayer(sampleTeam.getId(), player.getId());

        assertFalse(player.isActive());
        verify(teamRepository).save(sampleTeam);
        assertThrows(EntityNotFoundException.class,
                () -> teamService.removePlayer(sampleTeam.getId(), player.getId()));
    }

    @Test
    @DisplayName("findPlayers returns only the active squad ordered by shirt number")
    void findPlayersReturnsActiveSquad() {
        sampleTeam.addPlayer("Martin Baturina", 10, "Midfielder");
        Player removed = sampleTeam.addPlayer("Bruno Petkovic", 9, "Forward");
        sampleTeam.addPlayer("Dominik Livakovic", 1, "Goalkeeper");
        sampleTeam.removePlayer(removed);
        when(teamRepository.findById(sampleTeam.getId())).thenReturn(Optional.of(sampleTeam));

        List<PlayerResponse> players = teamService.findPlayers(sampleTeam.getId());

        assertEquals(List.of(1, 10), players.stream().map(PlayerResponse::shirtNumber).toList());
        assertEquals(sampleTeam.getId(), players.get(0).teamId());
        assertEquals("Goalkeeper", players.get(0).position());
    }

    @Test
    @DisplayName("findPlayer throws EntityNotFoundException for a removed player")
    void findPlayerThrowsForRemovedPlayer() {
        Player player = sampleTeam.addPlayer("Bruno Petkovic", 9, "Forward");
        sampleTeam.removePlayer(player);
        when(teamRepository.findById(sampleTeam.getId())).thenReturn(Optional.of(sampleTeam));

        assertThrows(EntityNotFoundException.class,
                () -> teamService.findPlayer(sampleTeam.getId(), player.getId()));
    }
}
