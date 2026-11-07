package com.league.sportsleague.domain.ai;

import com.league.sportsleague.domain.exception.ChronicleUnavailableException;

/**
 * Domain port (outbound) for AI match chronicle generation and translation.
 * The domain stays agnostic of the LLM provider; adapters live in infrastructure.
 * Implementations throw {@link ChronicleUnavailableException} when the provider cannot answer.
 */
public interface ChroniclePort {

    /** Writes a new chronicle from the match data. */
    ChronicleResult generate(ChronicleRequest request);

    /** Translates an existing chronicle faithfully into {@code target} (same story, same facts). */
    ChronicleResult translate(ChronicleResult source, Language target);
}
