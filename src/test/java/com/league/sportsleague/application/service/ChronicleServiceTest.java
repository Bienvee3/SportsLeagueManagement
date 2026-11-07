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
import com.league.sportsleague.domain.exception.ChronicleUnavailableException;
import com.league.sportsleague.domain.model.MatchChronicle;
import com.league.sportsleague.domain.repository.ChronicleRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChronicleServiceTest {

    @Mock FixtureRepository fixtureRepository;
    @Mock ChronicleRepository chronicleRepository;
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

    // ── stored chronicles and translation ─────────────────────────────────────

    @Test
    @DisplayName("generateChronicle stores the new text and drops older versions")
    void generateStoresAndReplacesOlderVersions() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(chroniclePort.generate(any())).thenReturn(new ChronicleResult("Fresh report.", Language.ENGLISH));

        chronicleService.generateChronicle(fixture.getId(), Language.ENGLISH);

        verify(chronicleRepository).deleteByFixtureId(fixture.getId());
        var saved = ArgumentCaptor.forClass(MatchChronicle.class);
        verify(chronicleRepository).save(saved.capture());
        assertEquals("Fresh report.", saved.getValue().getContent());
        assertEquals(Language.ENGLISH, saved.getValue().getLanguage());
    }

    @Test
    @DisplayName("findChronicle returns the stored text in the requested language without calling the AI")
    void findReturnsStoredTextWithoutAi() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(chronicleRepository.findByFixtureIdAndLanguage(fixture.getId(), Language.CROATIAN))
                .thenReturn(Optional.of(new MatchChronicle(fixture.getId(), Language.CROATIAN, "Izvještaj.")));

        var result = chronicleService.findChronicle(fixture.getId(), Language.CROATIAN);

        assertEquals("Izvještaj.", result.orElseThrow().text());
        verifyNoInteractions(chroniclePort);
    }

    @Test
    @DisplayName("findChronicle translates the stored other-language text and stores the translation")
    void findTranslatesStoredOtherLanguage() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(chronicleRepository.findByFixtureIdAndLanguage(fixture.getId(), Language.CROATIAN))
                .thenReturn(Optional.empty());
        when(chronicleRepository.findByFixtureId(fixture.getId()))
                .thenReturn(java.util.List.of(new MatchChronicle(fixture.getId(), Language.ENGLISH, "English report.")));
        when(chroniclePort.translate(any(), eq(Language.CROATIAN)))
                .thenReturn(new ChronicleResult("Hrvatski izvještaj.", Language.CROATIAN));

        var result = chronicleService.findChronicle(fixture.getId(), Language.CROATIAN);

        assertEquals("Hrvatski izvještaj.", result.orElseThrow().text());
        var source = ArgumentCaptor.forClass(ChronicleResult.class);
        verify(chroniclePort).translate(source.capture(), eq(Language.CROATIAN));
        assertEquals("English report.", source.getValue().text());
        verify(chroniclePort, never()).generate(any());
        var saved = ArgumentCaptor.forClass(MatchChronicle.class);
        verify(chronicleRepository).save(saved.capture());
        assertEquals(Language.CROATIAN, saved.getValue().getLanguage());
    }

    @Test
    @DisplayName("findChronicle is empty when nothing has been generated yet")
    void findEmptyWhenNothingStored() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(chronicleRepository.findByFixtureIdAndLanguage(any(), any())).thenReturn(Optional.empty());
        when(chronicleRepository.findByFixtureId(fixture.getId())).thenReturn(java.util.List.of());

        assertTrue(chronicleService.findChronicle(fixture.getId(), Language.ENGLISH).isEmpty());
        verifyNoInteractions(chroniclePort);
    }

    @Test
    @DisplayName("a failed translation is reported and nothing is stored")
    void failedTranslationStoresNothing() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(chronicleRepository.findByFixtureIdAndLanguage(any(), any())).thenReturn(Optional.empty());
        when(chronicleRepository.findByFixtureId(fixture.getId()))
                .thenReturn(java.util.List.of(new MatchChronicle(fixture.getId(), Language.ENGLISH, "English report.")));
        when(chroniclePort.translate(any(), any())).thenThrow(new ChronicleUnavailableException("503"));

        assertThrows(ChronicleUnavailableException.class,
                () -> chronicleService.findChronicle(fixture.getId(), Language.CROATIAN));
        verify(chronicleRepository, never()).save(any());
    }
}
