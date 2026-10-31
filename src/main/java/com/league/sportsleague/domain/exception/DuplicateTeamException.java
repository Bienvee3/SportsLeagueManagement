package com.league.sportsleague.domain.exception;

/**
 * Thrown when a team is added to a context where it already exists,
 * e.g. registering a team already enrolled in a season, or assigning
 * the same team as both home and away in a fixture.
 */
public class DuplicateTeamException extends RuntimeException {

    public DuplicateTeamException(String teamCode, String context) {
        super("Team %s is already %s".formatted(teamCode, context));
    }
}
