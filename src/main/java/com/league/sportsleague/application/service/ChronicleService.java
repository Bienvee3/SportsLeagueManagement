package com.league.sportsleague.application.service;

import com.league.sportsleague.domain.ai.ChroniclePort;
import com.league.sportsleague.domain.ai.ChronicleRequest;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.Language;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * AI match chronicles. Deliberately not {@code @Transactional}: the provider call is slow and paid, so
 * it runs between two short transactions owned by {@link ChronicleStore} instead of holding a database
 * connection, and nothing is deleted or stored unless the provider answered.
 */
@Service
public class ChronicleService {

    private final ChronicleStore store;
    private final ChroniclePort chroniclePort;

    public ChronicleService(ChronicleStore store, ChroniclePort chroniclePort) {
        this.store = store;
        this.chroniclePort = chroniclePort;
    }

    /**
     * Writes a new chronicle for the fixture, replacing any stored version (including translations
     * of the previous text, which would otherwise tell a different story).
     *
     * @param language output language (defaults to ENGLISH if null)
     */
    public ChronicleResult generateChronicle(UUID fixtureId, Language language) {
        Language lang = orDefault(language);
        ChronicleRequest request = store.buildRequest(fixtureId, lang);   // transaction 1 (read)
        ChronicleResult result = chroniclePort.generate(request);         // no transaction
        store.replace(fixtureId, lang, result.text());                    // transaction 2 (write)
        return result;
    }

    /**
     * The stored chronicle in the requested language, or empty. Read-only: it never calls the AI provider,
     * so every role may use it.
     */
    public Optional<ChronicleResult> findChronicle(UUID fixtureId, Language language) {
        return store.findStored(fixtureId, orDefault(language));
    }

    /**
     * The chronicle in the requested language: the stored one, or — when it only exists in another
     * language — a faithful translation of that text (stored for next time). Empty when no chronicle
     * has been generated yet; never invents a new story.
     */
    public Optional<ChronicleResult> translateChronicle(UUID fixtureId, Language language) {
        Language lang = orDefault(language);
        Optional<ChronicleResult> existing = store.findStored(fixtureId, lang);
        if (existing.isPresent()) {
            return existing;
        }
        Optional<ChronicleResult> source = store.findAnyStored(fixtureId);
        if (source.isEmpty()) {
            return Optional.empty();
        }
        ChronicleResult translated = chroniclePort.translate(source.get(), lang);   // no transaction
        boolean stored = store.saveTranslation(fixtureId, source.get(), translated);
        return stored ? Optional.of(translated) : Optional.empty();
    }

    private static Language orDefault(Language language) {
        return language != null ? language : Language.ENGLISH;
    }
}
