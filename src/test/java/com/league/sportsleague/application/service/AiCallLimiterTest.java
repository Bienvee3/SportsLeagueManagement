package com.league.sportsleague.application.service;

import com.league.sportsleague.domain.exception.AiQuotaExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiCallLimiterTest {

    /** A clock the test moves by hand. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-18T10:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();

    @Test
    @DisplayName("allows calls up to the cap and rejects the next one")
    void rejectsOverTheCap() {
        AiCallLimiter limiter = new AiCallLimiter(3, Duration.ofHours(1), clock);

        assertDoesNotThrow(limiter::acquire);
        assertDoesNotThrow(limiter::acquire);
        assertDoesNotThrow(limiter::acquire);
        assertThrows(AiQuotaExceededException.class, limiter::acquire);
    }

    @Test
    @DisplayName("a rejected call does not use up quota")
    void rejectionIsNotCounted() {
        AiCallLimiter limiter = new AiCallLimiter(1, Duration.ofHours(1), clock);
        limiter.acquire();

        assertThrows(AiQuotaExceededException.class, limiter::acquire);
        clock.advance(Duration.ofMinutes(61));

        assertDoesNotThrow(limiter::acquire);
    }

    @Test
    @DisplayName("quota comes back as old calls leave the window")
    void windowSlides() {
        AiCallLimiter limiter = new AiCallLimiter(2, Duration.ofHours(1), clock);
        limiter.acquire();
        clock.advance(Duration.ofMinutes(40));
        limiter.acquire();

        assertThrows(AiQuotaExceededException.class, limiter::acquire);
        clock.advance(Duration.ofMinutes(21)); // the first call is now 61 minutes old

        assertDoesNotThrow(limiter::acquire);
        assertThrows(AiQuotaExceededException.class, limiter::acquire);
    }

    @Test
    @DisplayName("a cap of 0 or less disables the limit")
    void zeroMeansUnlimited() {
        AiCallLimiter limiter = new AiCallLimiter(0, Duration.ofHours(1), clock);

        for (int i = 0; i < 100; i++) {
            assertDoesNotThrow(limiter::acquire);
        }
    }
}
