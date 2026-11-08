package com.league.sportsleague.application.service;

import com.league.sportsleague.domain.ai.ChronicleRequest;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.League;
import com.league.sportsleague.domain.model.MatchChronicle;
import com.league.sportsleague.domain.model.MatchEvent;
import com.league.sportsleague.domain.model.Round;
import com.league.sportsleague.domain.model.Season;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.model.enums.MatchEventType;
import com.league.sportsleague.domain.repository.ChronicleRepository;
import com.league.sportsleague.domain.repository.FixtureRepository;
import com.league.sportsleague.domain.valueobject.Score;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChronicleStoreTest {

    @Mock FixtureRepository fixtureRepository;
    @Mock ChronicleRepository chronicleRepository;

    @InjectMocks ChronicleStore store;

    private Fixture fixture;
    private Team home;

    @BeforeEach
    void setUp() {
        League league = new League("Test League", "test-league", "FOOTBALL", "Croatia");
        Season season = new Season(league, "2026/2027", 2026, 2027, null);
        Round round = new Round(season, 1, "R1", null, null, null);
        home = new Team("Dinamo Zagreb", "DZG", "Maksimir", "Zagreb", 1945);
        Team away = new Team("Hajduk Split", "HAJ", "Poljud", "Split", 1911);
        fixture = new Fixture(round, home, away, Instant.now());
    }

    @Test
    @DisplayName("buildRequest maps team names, score, events and language")
    void buildRequestMapsFixture() {
        fixture.addMatchEvent(new MatchEvent(fixture, home, MatchEventType.GOAL, 23, "Petkovic"));
        fixture.recordResult(new Score(1, 0));
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));

        ChronicleRequest request = store.buildRequest(fixture.getId(), Language.CROATIAN);

        assertEquals("Dinamo Zagreb", request.homeTeam());
        assertEquals("Hajduk Split", request.awayTeam());
        assertEquals(1, request.homeScore());
        assertEquals(0, request.awayScore());
        assertEquals(Language.CROATIAN, request.language());
        assertEquals(1, request.events().size());
        assertEquals("Petkovic", request.events().get(0).playerName());
    }

    @Test
    @DisplayName("buildRequest throws EntityNotFoundException for an unknown fixture")
    void buildRequestUnknownFixture() {
        when(fixtureRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> store.buildRequest(UUID.randomUUID(), Language.ENGLISH));
    }

    @Test
    @DisplayName("findStored returns the stored text of the requested language")
    void findStoredReturnsText() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(chronicleRepository.findByFixtureIdAndLanguage(fixture.getId(), Language.CROATIAN))
                .thenReturn(Optional.of(new MatchChronicle(fixture.getId(), Language.CROATIAN, "Izvještaj.")));

        var result = store.findStored(fixture.getId(), Language.CROATIAN);

        assertEquals(new ChronicleResult("Izvještaj.", Language.CROATIAN), result.orElseThrow());
    }

    @Test
    @DisplayName("findStored throws EntityNotFoundException for an unknown fixture")
    void findStoredUnknownFixture() {
        when(fixtureRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> store.findStored(UUID.randomUUID(), Language.ENGLISH));
    }

    @Test
    @DisplayName("replace deletes every stored version first and then saves the new one")
    void replaceDeletesThenSaves() {
        store.replace(fixture.getId(), Language.ENGLISH, "Fresh report.");

        InOrder order = inOrder(chronicleRepository);
        order.verify(chronicleRepository).deleteByFixtureId(fixture.getId());
        var saved = ArgumentCaptor.forClass(MatchChronicle.class);
        order.verify(chronicleRepository).save(saved.capture());
        assertEquals("Fresh report.", saved.getValue().getContent());
        assertEquals(Language.ENGLISH, saved.getValue().getLanguage());
    }

    @Test
    @DisplayName("saveTranslation stores the translation while its source is still the stored chronicle")
    void saveTranslationStoresWhenSourceUnchanged() {
        ChronicleResult source = new ChronicleResult("English report.", Language.ENGLISH);
        ChronicleResult translated = new ChronicleResult("Hrvatski izvještaj.", Language.CROATIAN);
        when(chronicleRepository.findByFixtureIdAndLanguage(fixture.getId(), Language.ENGLISH))
                .thenReturn(Optional.of(new MatchChronicle(fixture.getId(), Language.ENGLISH, "English report.")));
        when(chronicleRepository.findByFixtureIdAndLanguage(fixture.getId(), Language.CROATIAN))
                .thenReturn(Optional.empty());

        assertTrue(store.saveTranslation(fixture.getId(), source, translated));

        var saved = ArgumentCaptor.forClass(MatchChronicle.class);
        verify(chronicleRepository).save(saved.capture());
        assertEquals(Language.CROATIAN, saved.getValue().getLanguage());
        assertEquals("Hrvatski izvještaj.", saved.getValue().getContent());
    }

    @Test
    @DisplayName("saveTranslation discards a translation whose source was regenerated in the meantime")
    void saveTranslationDiscardedWhenSourceChanged() {
        ChronicleResult source = new ChronicleResult("Old story.", Language.ENGLISH);
        ChronicleResult translated = new ChronicleResult("Stara priča.", Language.CROATIAN);
        when(chronicleRepository.findByFixtureIdAndLanguage(fixture.getId(), Language.ENGLISH))
                .thenReturn(Optional.of(new MatchChronicle(fixture.getId(), Language.ENGLISH, "New story.")));

        assertFalse(store.saveTranslation(fixture.getId(), source, translated));

        verify(chronicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveTranslation keeps the translation that another request stored first")
    void saveTranslationDoesNotDuplicate() {
        ChronicleResult source = new ChronicleResult("English report.", Language.ENGLISH);
        ChronicleResult translated = new ChronicleResult("Hrvatski izvještaj.", Language.CROATIAN);
        when(chronicleRepository.findByFixtureIdAndLanguage(fixture.getId(), Language.ENGLISH))
                .thenReturn(Optional.of(new MatchChronicle(fixture.getId(), Language.ENGLISH, "English report.")));
        when(chronicleRepository.findByFixtureIdAndLanguage(fixture.getId(), Language.CROATIAN))
                .thenReturn(Optional.of(new MatchChronicle(fixture.getId(), Language.CROATIAN, "Već pohranjeno.")));

        assertTrue(store.saveTranslation(fixture.getId(), source, translated));

        verify(chronicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("findAnyStored returns the first stored chronicle of any language")
    void findAnyStoredReturnsFirst() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(chronicleRepository.findByFixtureId(fixture.getId()))
                .thenReturn(List.of(new MatchChronicle(fixture.getId(), Language.ENGLISH, "English report.")));

        var result = store.findAnyStored(fixture.getId());

        assertEquals(new ChronicleResult("English report.", Language.ENGLISH), result.orElseThrow());
    }
}
