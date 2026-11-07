package com.league.sportsleague.infrastructure.ai;

import com.league.sportsleague.domain.ai.ChroniclePort;
import com.league.sportsleague.domain.ai.ChronicleRequest;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.exception.ChronicleUnavailableException;

/**
 * Fallback adapter used when no AI provider is configured
 * (e.g. tests, {@code spring.ai.model.chat=none}). It never invents a chronicle: both operations
 * report the provider as unavailable so nothing gets stored.
 * Registered as a bean only when no other {@link ChroniclePort} bean is present
 * — see {@link ChronicleAdapterConfig}.
 */
public class StubChronicleAdapter implements ChroniclePort {

    private static final String MESSAGE = "AI provider not configured";

    @Override
    public ChronicleResult generate(ChronicleRequest request) {
        throw new ChronicleUnavailableException(MESSAGE);
    }

    @Override
    public ChronicleResult translate(ChronicleResult source, Language target) {
        throw new ChronicleUnavailableException(MESSAGE);
    }
}
