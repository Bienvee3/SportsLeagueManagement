package com.league.sportsleague.application.service;

import com.league.sportsleague.domain.ai.ChroniclePort;
import com.league.sportsleague.domain.ai.ChronicleRequest;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.exception.ChronicleUnavailableException;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * ChronicleService orchestrates short transactional steps (ChronicleStore) around the slow provider
 * call, so these tests pin the order of the steps and that the provider is only used where intended.
 */
@ExtendWith(MockitoExtension.class)
class ChronicleServiceTest {

    private static final UUID FIXTURE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock ChronicleStore store;
    @Mock ChroniclePort chroniclePort;

    @InjectMocks ChronicleService chronicleService;

    private static ChronicleRequest request(Language language) {
        return new ChronicleRequest("Dinamo Zagreb", "Hajduk Split", 2, 1, List.of(), language);
    }

    // ── generate ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generate reads the data, calls the provider, and only then persists the result")
    void generateCallsProviderBetweenTheTwoTransactions() {
        ChronicleRequest req = request(Language.CROATIAN);
        when(store.buildRequest(FIXTURE_ID, Language.CROATIAN)).thenReturn(req);
        when(chroniclePort.generate(req)).thenReturn(new ChronicleResult("Izvještaj.", Language.CROATIAN));

        ChronicleResult result = chronicleService.generateChronicle(FIXTURE_ID, Language.CROATIAN);

        assertEquals("Izvještaj.", result.text());
        InOrder order = inOrder(store, chroniclePort);
        order.verify(store).buildRequest(FIXTURE_ID, Language.CROATIAN);
        order.verify(chroniclePort).generate(req);
        order.verify(store).replace(FIXTURE_ID, Language.CROATIAN, "Izvještaj.");
    }

    @Test
    @DisplayName("generate defaults to ENGLISH when the language is null")
    void generateDefaultsToEnglish() {
        ChronicleRequest req = request(Language.ENGLISH);
        when(store.buildRequest(FIXTURE_ID, Language.ENGLISH)).thenReturn(req);
        when(chroniclePort.generate(req)).thenReturn(new ChronicleResult("Report.", Language.ENGLISH));

        chronicleService.generateChronicle(FIXTURE_ID, null);

        verify(store).replace(FIXTURE_ID, Language.ENGLISH, "Report.");
    }

    @Test
    @DisplayName("generate deletes and stores nothing when the provider fails")
    void generateKeepsStoredChroniclesWhenProviderFails() {
        ChronicleRequest req = request(Language.ENGLISH);
        when(store.buildRequest(FIXTURE_ID, Language.ENGLISH)).thenReturn(req);
        when(chroniclePort.generate(req)).thenThrow(new ChronicleUnavailableException("503"));

        assertThrows(ChronicleUnavailableException.class,
                () -> chronicleService.generateChronicle(FIXTURE_ID, Language.ENGLISH));

        verify(store).buildRequest(FIXTURE_ID, Language.ENGLISH);
        verifyNoMoreInteractions(store);
    }

    @Test
    @DisplayName("generate does not touch the provider when the fixture does not exist")
    void generateUnknownFixtureNeverCallsProvider() {
        when(store.buildRequest(any(), any())).thenThrow(new EntityNotFoundException("Fixture", FIXTURE_ID));

        assertThrows(EntityNotFoundException.class,
                () -> chronicleService.generateChronicle(FIXTURE_ID, Language.ENGLISH));

        verifyNoInteractions(chroniclePort);
    }

    // ── find (read only, never calls the provider) ────────────────────────────

    @Test
    @DisplayName("find returns the stored chronicle without calling the provider")
    void findReturnsStoredWithoutProvider() {
        when(store.findStored(FIXTURE_ID, Language.CROATIAN))
                .thenReturn(Optional.of(new ChronicleResult("Izvještaj.", Language.CROATIAN)));

        var result = chronicleService.findChronicle(FIXTURE_ID, Language.CROATIAN);

        assertEquals("Izvještaj.", result.orElseThrow().text());
        verifyNoInteractions(chroniclePort);
    }

    @Test
    @DisplayName("find is empty when the chronicle only exists in the other language — no translation happens")
    void findDoesNotTranslate() {
        when(store.findStored(FIXTURE_ID, Language.CROATIAN)).thenReturn(Optional.empty());

        assertTrue(chronicleService.findChronicle(FIXTURE_ID, Language.CROATIAN).isEmpty());

        verifyNoInteractions(chroniclePort);
        verify(store, never()).findAnyStored(any());
    }

    // ── translate (explicit, provider call outside any transaction) ───────────

    @Test
    @DisplayName("translate returns an existing chronicle of the requested language without calling the provider")
    void translateReturnsExisting() {
        when(store.findStored(FIXTURE_ID, Language.CROATIAN))
                .thenReturn(Optional.of(new ChronicleResult("Izvještaj.", Language.CROATIAN)));

        var result = chronicleService.translateChronicle(FIXTURE_ID, Language.CROATIAN);

        assertEquals("Izvještaj.", result.orElseThrow().text());
        verifyNoInteractions(chroniclePort);
    }

    @Test
    @DisplayName("translate translates the stored other-language text (never regenerates) and stores it afterwards")
    void translateTranslatesAndStores() {
        ChronicleResult source = new ChronicleResult("English report.", Language.ENGLISH);
        ChronicleResult translated = new ChronicleResult("Hrvatski izvještaj.", Language.CROATIAN);
        when(store.findStored(FIXTURE_ID, Language.CROATIAN)).thenReturn(Optional.empty());
        when(store.findAnyStored(FIXTURE_ID)).thenReturn(Optional.of(source));
        when(chroniclePort.translate(source, Language.CROATIAN)).thenReturn(translated);
        when(store.saveTranslation(FIXTURE_ID, source, translated)).thenReturn(true);

        var result = chronicleService.translateChronicle(FIXTURE_ID, Language.CROATIAN);

        assertEquals("Hrvatski izvještaj.", result.orElseThrow().text());
        verify(chroniclePort, never()).generate(any());
        InOrder order = inOrder(chroniclePort, store);
        order.verify(chroniclePort).translate(source, Language.CROATIAN);
        order.verify(store).saveTranslation(FIXTURE_ID, source, translated);
    }

    @Test
    @DisplayName("translate is empty and calls nothing when no chronicle exists yet")
    void translateEmptyWhenNothingStored() {
        when(store.findStored(FIXTURE_ID, Language.ENGLISH)).thenReturn(Optional.empty());
        when(store.findAnyStored(FIXTURE_ID)).thenReturn(Optional.empty());

        assertTrue(chronicleService.translateChronicle(FIXTURE_ID, Language.ENGLISH).isEmpty());

        verifyNoInteractions(chroniclePort);
    }

    @Test
    @DisplayName("a failed translation is reported and nothing is stored")
    void failedTranslationStoresNothing() {
        ChronicleResult source = new ChronicleResult("English report.", Language.ENGLISH);
        when(store.findStored(FIXTURE_ID, Language.CROATIAN)).thenReturn(Optional.empty());
        when(store.findAnyStored(FIXTURE_ID)).thenReturn(Optional.of(source));
        when(chroniclePort.translate(any(), eq(Language.CROATIAN)))
                .thenThrow(new ChronicleUnavailableException("503"));

        assertThrows(ChronicleUnavailableException.class,
                () -> chronicleService.translateChronicle(FIXTURE_ID, Language.CROATIAN));

        verify(store, never()).saveTranslation(any(), any(), any());
    }

    @Test
    @DisplayName("a translation of a chronicle that was regenerated meanwhile is discarded")
    void staleTranslationIsDiscarded() {
        ChronicleResult source = new ChronicleResult("Old story.", Language.ENGLISH);
        ChronicleResult translated = new ChronicleResult("Stara priča.", Language.CROATIAN);
        when(store.findStored(FIXTURE_ID, Language.CROATIAN)).thenReturn(Optional.empty());
        when(store.findAnyStored(FIXTURE_ID)).thenReturn(Optional.of(source));
        when(chroniclePort.translate(source, Language.CROATIAN)).thenReturn(translated);
        when(store.saveTranslation(FIXTURE_ID, source, translated)).thenReturn(false);

        assertTrue(chronicleService.translateChronicle(FIXTURE_ID, Language.CROATIAN).isEmpty());
    }
}
