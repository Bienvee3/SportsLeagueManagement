package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CreateTeamRequest;
import com.league.sportsleague.application.dto.TeamResponse;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
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
                new CreateTeamRequest("GNK Dinamo Zagreb", "DIN", "Maksimir", "Zagreb", 1911));

        assertNotNull(id);
        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(captor.capture());
        assertEquals("DIN", captor.getValue().getShortCode().value());
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
}
