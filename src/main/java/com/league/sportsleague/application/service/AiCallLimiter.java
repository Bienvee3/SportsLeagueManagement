package com.league.sportsleague.application.service;

import com.league.sportsleague.domain.exception.AiQuotaExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Caps how many paid AI provider calls the whole application may make in a sliding window, so a stuck
 * script or a curious user cannot run up the bill of the shared API key. Every attempt counts, including
 * the ones the provider then fails, because the provider may bill them. The counter lives in memory: it
 * resets on restart and each application instance has its own, which is acceptable for a cost guard.
 * A cap of 0 or less disables it.
 */
@Component
public class AiCallLimiter {

    private final int maxCalls;
    private final Duration window;
    private final Clock clock;
    private final Deque<Instant> calls = new ArrayDeque<>();

    @Autowired
    public AiCallLimiter(@Value("${app.ai.max-calls-per-hour:30}") int maxCalls) {
        this(maxCalls, Duration.ofHours(1), Clock.systemUTC());
    }

    AiCallLimiter(int maxCalls, Duration window, Clock clock) {
        this.maxCalls = maxCalls;
        this.window = window;
        this.clock = clock;
    }

    /** Reserves one call or throws {@link AiQuotaExceededException} when the window is full. */
    public synchronized void acquire() {
        if (maxCalls <= 0) {
            return;
        }
        Instant now = clock.instant();
        Instant oldest = now.minus(window);
        while (!calls.isEmpty() && !calls.peekFirst().isAfter(oldest)) {
            calls.removeFirst();
        }
        if (calls.size() >= maxCalls) {
            throw new AiQuotaExceededException(maxCalls, window);
        }
        calls.addLast(now);
    }
}
