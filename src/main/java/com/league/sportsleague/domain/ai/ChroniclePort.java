package com.league.sportsleague.domain.ai;

/**
 * Domain port (outbound) for AI match chronicle generation.
 * The domain stays agnostic of the LLM provider; adapters live in infrastructure.
 */
public interface ChroniclePort {
    ChronicleResult generate(ChronicleRequest request);
}
