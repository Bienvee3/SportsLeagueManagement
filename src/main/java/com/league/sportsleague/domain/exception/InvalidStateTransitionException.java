package com.league.sportsleague.domain.exception;

/**
 * Thrown when an entity state-transition method is called from an illegal current state.
 * Example: calling Season.complete() when the season is still UPCOMING.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String entityType, String currentState, String attemptedAction) {
        super("Cannot %s %s in state %s".formatted(attemptedAction, entityType, currentState));
    }
}
