package com.league.sportsleague.domain;

import java.util.Locale;

/**
 * Match-sheet rules of each supported sport: how many players start, how many may sit on the bench,
 * how substitutions work and how long a regulation match lasts.
 */
public enum LineupRules {

    FOOTBALL(11, 12, 5, false, 90),
    BASKETBALL(5, 7, Integer.MAX_VALUE, true, 40),
    HANDBALL(7, 9, Integer.MAX_VALUE, true, 60),
    /** Volleyball has no game clock, so minutes played are not tracked ({@code regulationMinutes == 0}). */
    VOLLEYBALL(6, 8, Integer.MAX_VALUE, true, 0);

    private final int starters;
    private final int maxBench;
    private final int maxSubstitutions;
    private final boolean reentryAllowed;
    private final int regulationMinutes;

    LineupRules(int starters, int maxBench, int maxSubstitutions, boolean reentryAllowed, int regulationMinutes) {
        this.starters = starters;
        this.maxBench = maxBench;
        this.maxSubstitutions = maxSubstitutions;
        this.reentryAllowed = reentryAllowed;
        this.regulationMinutes = regulationMinutes;
    }

    /** Exact number of players in the starting lineup. */
    public int starters() { return starters; }

    /** Maximum number of substitutes named on the match sheet. */
    public int maxBench() { return maxBench; }

    /** Maximum number of substitutions per team and match. */
    public int maxSubstitutions() { return maxSubstitutions; }

    /** Whether a player who left the pitch may come back on. */
    public boolean reentryAllowed() { return reentryAllowed; }

    /** Length of a regulation match in minutes; 0 when the sport has no game clock. */
    public int regulationMinutes() { return regulationMinutes; }

    /**
     * Rules for a sport type as stored on leagues and teams. Sport types are free strings, so a
     * missing or unknown value falls back to FOOTBALL, the default sport of the application.
     */
    public static LineupRules forSport(String sportType) {
        if (sportType == null || sportType.isBlank()) {
            return FOOTBALL;
        }
        try {
            return valueOf(sportType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknownSport) {
            return FOOTBALL;
        }
    }
}
