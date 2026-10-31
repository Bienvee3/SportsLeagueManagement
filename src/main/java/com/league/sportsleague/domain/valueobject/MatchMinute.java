package com.league.sportsleague.domain.valueobject;

public record MatchMinute(int value) {

    public MatchMinute {
        if (value < 1 || value > 130) {
            throw new IllegalArgumentException(
                "Match minute must be between 1 and 130, got: " + value);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
