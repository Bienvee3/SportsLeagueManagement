package com.league.sportsleague.domain;

import java.util.UUID;

/**
 * Immutable snapshot of a team's position in the standings table.
 * Not a JPA entity — computed on demand from finished fixtures.
 */
public record StandingsEntry(
        UUID teamId,
        String teamName,
        int played,
        int wins,
        int draws,
        int losses,
        int goalsFor,
        int goalsAgainst,
        int goalDifference,
        int points
) {
    /** Accumulates a home win for this team. */
    StandingsEntry withHomeWin(int scored, int conceded) {
        return new StandingsEntry(teamId, teamName,
                played + 1, wins + 1, draws, losses,
                goalsFor + scored, goalsAgainst + conceded,
                goalDifference + scored - conceded, points + 3);
    }

    StandingsEntry withHomeDraw(int scored, int conceded) {
        return new StandingsEntry(teamId, teamName,
                played + 1, wins, draws + 1, losses,
                goalsFor + scored, goalsAgainst + conceded,
                goalDifference + scored - conceded, points + 1);
    }

    StandingsEntry withHomeLoss(int scored, int conceded) {
        return new StandingsEntry(teamId, teamName,
                played + 1, wins, draws, losses + 1,
                goalsFor + scored, goalsAgainst + conceded,
                goalDifference + scored - conceded, points);
    }

    StandingsEntry withAwayWin(int scored, int conceded) {
        return new StandingsEntry(teamId, teamName,
                played + 1, wins + 1, draws, losses,
                goalsFor + scored, goalsAgainst + conceded,
                goalDifference + scored - conceded, points + 3);
    }

    StandingsEntry withAwayDraw(int scored, int conceded) {
        return new StandingsEntry(teamId, teamName,
                played + 1, wins, draws + 1, losses,
                goalsFor + scored, goalsAgainst + conceded,
                goalDifference + scored - conceded, points + 1);
    }

    StandingsEntry withAwayLoss(int scored, int conceded) {
        return new StandingsEntry(teamId, teamName,
                played + 1, wins, draws, losses + 1,
                goalsFor + scored, goalsAgainst + conceded,
                goalDifference + scored - conceded, points);
    }

    /** Creates a zero-entry for a newly encountered team. */
    static StandingsEntry blank(UUID teamId, String teamName) {
        return new StandingsEntry(teamId, teamName, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
