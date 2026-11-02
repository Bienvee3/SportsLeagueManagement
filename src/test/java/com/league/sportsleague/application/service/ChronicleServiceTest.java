package com.league.sportsleague.application.service;

import com.league.sportsleague.domain.ai.ChroniclePort;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.League;
import com.league.sportsleague.domain.model.Round;
import com.league.sportsleague.domain.model.Season;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.repository.FixtureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChronicleServiceTest {

    @Mock FixtureRepository fixtureRepository;
    @Mock ChroniclePort chroniclePort;

    @InjectMocks ChronicleService chronicleService;

    private Fixture fixture;

    @BeforeEach
    void setUp() {
        League league = new League("Test League", "test-league", "FOOTBALL", "Croatia");
        Season season = new Season(league, "2026/2027", 2026, 2027, null);
        Round round = new Round(season, 1, "R1", null, null, null);
        Team home = new Team("Dinamo Zagreb", "DZG", "Maksimir", "Zagreb", 1945);
        Team away = new Team("Hajduk Split", "HAJ", "Poljud", "Split", 1911);
        fixture = new Fixture(round, home, away, Instant.now());
    }

    @Test
    @DisplayName("throws EntityNotFoundException when fixture not found")
    void throwsWhenFixtureNotFound() {
        when(fixtureRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> chronicleService.generateChronicle(UUID.randomUUID(), Language.ENGLISH));
    }

    @Test
    @DisplayName("delegates to ChroniclePort with correct home and away team names")
    void delegatesToPortWithCorrectTeamNames() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(chroniclePort.generate(any())).thenReturn(new ChronicleResult("Match report.", Language.ENGLISH));

        chronicleService.generateChronicle(fixture.getId(), Language.ENGLISH);

        var captor = ArgumentCaptor.forClass(com.league.sportsleague.domain.ai.ChronicleRequest.class);
        verify(chroniclePort).generate(captor.capture());
        assertEquals("Dinamo Zagreb", captor.getValue().homeTeam());
        assertEquals("Hajduk Split",  captor.getValue().awayTeam());
    }

    @Test
    @DisplayName("passes CROATIAN language to the port when requested")
    void passesCroatianLanguage() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(chroniclePort.generate(any())).thenReturn(new ChronicleResult("Izvještaj.", Language.CROATIAN));

        var result = chronicleService.generateChronicle(fixture.getId(), Language.CROATIAN);

        var captor = ArgumentCaptor.forClass(com.league.sportsleague.domain.ai.ChronicleRequest.class);
        verify(chroniclePort).generate(captor.capture());
        assertEquals(Language.CROATIAN, captor.getValue().language());
        assertEquals(Language.CROATIAN, result.language());
    }

    @Test
    @DisplayName("defaults to ENGLISH when language is null")
    void defaultsToEnglishWhenNull() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(chroniclePort.generate(any())).thenReturn(new ChronicleResult("Report.", Language.ENGLISH));

        chronicleService.generateChronicle(fixture.getId(), null);

        var captor = ArgumentCaptor.forClass(com.league.sportsleague.domain.ai.ChronicleRequest.class);
        verify(chroniclePort).generate(captor.capture());
        assertEquals(Language.ENGLISH, captor.getValue().language());
    }

    @Test
    @DisplayName("returns ChronicleResult from the port")
    void returnsPortResult() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(chroniclePort.generate(any())).thenReturn(new ChronicleResult("Great match!", Language.ENGLISH));

        var result = chronicleService.generateChronicle(fixture.getId(), Language.ENGLISH);

        assertNotNull(result);
        assertEquals("Great match!", result.text());
    }
}
