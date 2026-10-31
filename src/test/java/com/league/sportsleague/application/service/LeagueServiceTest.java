package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CreateLeagueRequest;
import com.league.sportsleague.application.dto.LeagueResponse;
import com.league.sportsleague.application.dto.UpdateLeagueRequest;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.model.League;
import com.league.sportsleague.domain.repository.LeagueRepository;
import com.league.sportsleague.domain.valueobject.Slug;
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
class LeagueServiceTest {

    @Mock LeagueRepository leagueRepository;
    @InjectMocks LeagueService leagueService;

    private League sampleLeague;

    @BeforeEach
    void setUp() {
        sampleLeague = new League("SuperSport HNL", "supersport-hnl", "FOOTBALL", "Croatia");
    }

    // ── createLeague ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createLeague saves a League and returns its UUID")
    void createLeagueSavesAndReturnsId() {
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateLeagueRequest req = new CreateLeagueRequest("SuperSport HNL", "supersport-hnl", "FOOTBALL", "Croatia");
        UUID id = leagueService.createLeague(req);

        assertNotNull(id);
        ArgumentCaptor<League> captor = ArgumentCaptor.forClass(League.class);
        verify(leagueRepository).save(captor.capture());
        assertEquals("SuperSport HNL", captor.getValue().getName());
        assertEquals("supersport-hnl", captor.getValue().getSlug().value());
    }

    @Test
    @DisplayName("createLeague uses FOOTBALL as default sport type when null")
    void createLeagueDefaultsSportType() {
        when(leagueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        leagueService.createLeague(new CreateLeagueRequest("Liga", "liga", null, "Spain"));

        ArgumentCaptor<League> captor = ArgumentCaptor.forClass(League.class);
        verify(leagueRepository).save(captor.capture());
        assertEquals("FOOTBALL", captor.getValue().getSportType());
    }

    // ── rename ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("rename calls league.rename() and saves")
    void renameCallsDomainMethodAndSaves() {
        UUID id = sampleLeague.getId();
        when(leagueRepository.findById(id)).thenReturn(Optional.of(sampleLeague));
        when(leagueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        leagueService.rename(id, new UpdateLeagueRequest("New Name", "Spain", "BASKETBALL"));

        assertEquals("New Name", sampleLeague.getName());
        assertEquals("Spain", sampleLeague.getCountry());
        verify(leagueRepository).save(sampleLeague);
    }

    @Test
    @DisplayName("rename throws EntityNotFoundException when league not found")
    void renameThrowsWhenNotFound() {
        when(leagueRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> leagueService.rename(UUID.randomUUID(), new UpdateLeagueRequest("N", "C", null)));
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns mapped LeagueResponse")
    void findByIdReturnsResponse() {
        UUID id = sampleLeague.getId();
        when(leagueRepository.findById(id)).thenReturn(Optional.of(sampleLeague));

        LeagueResponse response = leagueService.findById(id);

        assertEquals(id, response.id());
        assertEquals("SuperSport HNL", response.name());
        assertEquals("supersport-hnl", response.slug());
    }

    @Test
    @DisplayName("findById throws EntityNotFoundException when not found")
    void findByIdThrowsWhenNotFound() {
        when(leagueRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> leagueService.findById(UUID.randomUUID()));
    }

    // ── findBySlug ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findBySlug returns mapped LeagueResponse")
    void findBySlugReturnsResponse() {
        when(leagueRepository.findBySlug(new Slug("supersport-hnl")))
                .thenReturn(Optional.of(sampleLeague));

        LeagueResponse response = leagueService.findBySlug("supersport-hnl");
        assertEquals("supersport-hnl", response.slug());
    }

    @Test
    @DisplayName("findBySlug throws EntityNotFoundException when not found")
    void findBySlugThrowsWhenNotFound() {
        when(leagueRepository.findBySlug(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> leagueService.findBySlug("unknown-slug"));
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll returns list of mapped LeagueResponse")
    void findAllReturnsResponses() {
        when(leagueRepository.findAll()).thenReturn(List.of(sampleLeague));

        List<LeagueResponse> all = leagueService.findAll();

        assertEquals(1, all.size());
        assertEquals("supersport-hnl", all.get(0).slug());
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete calls repository.delete after finding league")
    void deleteCallsRepositoryDelete() {
        UUID id = sampleLeague.getId();
        when(leagueRepository.findById(id)).thenReturn(Optional.of(sampleLeague));

        leagueService.delete(id);

        verify(leagueRepository).delete(sampleLeague);
    }

    @Test
    @DisplayName("delete throws EntityNotFoundException when not found")
    void deleteThrowsWhenNotFound() {
        when(leagueRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> leagueService.delete(UUID.randomUUID()));
    }
}
