package com.league.sportsleague.domain.exception;

/** The AI provider could not produce or translate a chronicle (down, overloaded, not configured). */
public class ChronicleUnavailableException extends RuntimeException {

    public ChronicleUnavailableException(String message) {
        super(message);
    }

    public ChronicleUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
