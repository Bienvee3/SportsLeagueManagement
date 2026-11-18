package com.league.sportsleague.domain.exception;

/** Thrown when the hourly cap on paid AI provider calls is used up. */
public class AiQuotaExceededException extends RuntimeException {

    public AiQuotaExceededException(int maxCalls, java.time.Duration window) {
        super("The limit of %d AI calls per %d minutes has been reached, try again later"
                .formatted(maxCalls, window.toMinutes()));
    }
}
