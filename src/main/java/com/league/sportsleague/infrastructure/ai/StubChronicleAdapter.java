package com.league.sportsleague.infrastructure.ai;

import com.league.sportsleague.domain.ai.ChroniclePort;
import com.league.sportsleague.domain.ai.ChronicleRequest;
import com.league.sportsleague.domain.ai.ChronicleResult;

/**
 * Fallback adapter used when the Gemini adapter is not configured
 * (e.g. tests, {@code spring.ai.model.chat=none}).
 * Registered as a bean only when no other {@link ChroniclePort} bean is present
 * — see {@link ChronicleAdapterConfig}.
 */
public class StubChronicleAdapter implements ChroniclePort {

    @Override
    public ChronicleResult generate(ChronicleRequest request) {
        String text = "Chronicle unavailable: AI provider not configured. " +
                "Match: %s %d-%d %s.".formatted(
                        request.homeTeam(), request.homeScore(),
                        request.awayScore(), request.awayTeam());
        return new ChronicleResult(text, request.language());
    }
}
