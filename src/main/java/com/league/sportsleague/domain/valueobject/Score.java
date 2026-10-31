package com.league.sportsleague.domain.valueobject;

public record Score(int home, int away) {

    public Score {
        if (home < 0 || away < 0) {
            throw new IllegalArgumentException(
                "Scores must be non-negative, got: " + home + "-" + away);
        }
    }

    @Override
    public String toString() {
        return home + "-" + away;
    }
}
