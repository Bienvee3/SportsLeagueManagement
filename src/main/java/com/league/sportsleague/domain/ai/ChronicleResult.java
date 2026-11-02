package com.league.sportsleague.domain.ai;

/** Output from the AI chronicle port. Contains the generated text and the language used. */
public record ChronicleResult(
        String text,
        Language language
) {}
