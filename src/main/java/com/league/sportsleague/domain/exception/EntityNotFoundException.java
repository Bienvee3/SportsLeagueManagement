package com.league.sportsleague.domain.exception;

/**
 * Thrown by application services when a repository lookup returns empty.
 * Named EntityNotFoundException to reflect the domain concept.
 * Note: this is distinct from jakarta.persistence.EntityNotFoundException
 * (which is an infrastructure concern). Always use this class by fully
 * qualified name or explicit import from this package.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String entityType, Object identifier) {
        super("%s not found: %s".formatted(entityType, identifier));
    }
}
