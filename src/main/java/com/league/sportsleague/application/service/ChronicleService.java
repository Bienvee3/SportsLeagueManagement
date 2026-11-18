package com.league.sportsleague.application.service;

import com.league.sportsleague.domain.ai.ChroniclePort;
import com.league.sportsleague.domain.ai.ChronicleRequest;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.ChronicleValidator;
import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.exception.ChronicleUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI match chronicles. Deliberately not {@code @Transactional}: the provider call is slow and paid, so
 * it runs between two short transactions owned by {@link ChronicleStore} instead of holding a database
 * connection, and nothing is deleted or stored unless the provider answered.
 *
 * <p>The model is not trusted with facts. Every answer is checked by {@link ChronicleValidator} against the
 * match data (final score, invented scorelines, leaked drafting notes, length); a rejected answer is
 * retried once with the reasons and, if it is rejected again, nothing is stored and the request fails. So
 * what a user reads has already been verified; there is no manual approval step.
 */
@Service
public class ChronicleService {

    private static final Logger log = LoggerFactory.getLogger(ChronicleService.class);

    /** The first answer plus one corrected retry; every attempt is a paid call and uses quota. */
    static final int MAX_ATTEMPTS = 2;

    private final ChronicleStore store;
    private final ChroniclePort chroniclePort;
    private final AiCallLimiter limiter;

    public ChronicleService(ChronicleStore store, ChroniclePort chroniclePort, AiCallLimiter limiter) {
        this.store = store;
        this.chroniclePort = chroniclePort;
        this.limiter = limiter;
    }

    /**
     * Writes a new chronicle for the fixture, replacing any stored version (including translations
     * of the previous text, which would otherwise tell a different story).
     *
     * @param language output language (defaults to ENGLISH if null)
     * @throws ChronicleUnavailableException when the provider fails or no answer passed verification
     */
    public ChronicleResult generateChronicle(UUID fixtureId, Language language) {
        Language lang = orDefault(language);
        ChronicleRequest base = store.buildRequest(fixtureId, lang);          // transaction 1 (read)
        ChronicleRequest attempt = base;
        List<String> problems = List.of();
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            limiter.acquire();                                                // cost guard, before any paid call
            ChronicleResult result = chroniclePort.generate(attempt);         // no transaction
            problems = ChronicleValidator.problems(base, result.text());
            if (problems.isEmpty()) {
                store.replace(fixtureId, lang, result.text());                // transaction 2 (write)
                return result;
            }
            log.warn("AI chronicle for fixture {} rejected (attempt {}/{}): {}", fixtureId, i, MAX_ATTEMPTS, problems);
            attempt = base.withCorrections(problems);
        }
        throw new ChronicleUnavailableException("The AI answer failed verification: " + problems);
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
     * language — a verified translation of that text (stored for next time). Empty when no chronicle
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
        List<String> problems = List.of();
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            limiter.acquire();                                                          // cost guard
            ChronicleResult translated = chroniclePort.translate(source.get(), lang);   // no transaction
            problems = ChronicleValidator.translationProblems(source.get().text(), translated.text());
            if (problems.isEmpty()) {
                boolean stored = store.saveTranslation(fixtureId, source.get(), translated);
                return stored ? Optional.of(translated) : Optional.empty();
            }
            log.warn("AI translation for fixture {} rejected (attempt {}/{}): {}", fixtureId, i, MAX_ATTEMPTS, problems);
        }
        throw new ChronicleUnavailableException("The AI translation failed verification: " + problems);
    }

    private static Language orDefault(Language language) {
        return language != null ? language : Language.ENGLISH;
    }
}
