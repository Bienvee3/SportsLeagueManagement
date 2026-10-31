package com.league.sportsleague.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainExceptionsTest {

    @Test
    @DisplayName("InvalidStateTransitionException should include entity, state, and action in message")
    void invalidStateTransitionShouldIncludeContext() {
        InvalidStateTransitionException ex =
            new InvalidStateTransitionException("Season", "UPCOMING", "complete");
        String msg = ex.getMessage();
        assertTrue(msg.contains("Season"), "Message must contain entity name");
        assertTrue(msg.contains("UPCOMING"), "Message must contain current state");
        assertTrue(msg.contains("complete"), "Message must contain attempted action");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("DuplicateTeamException should include team code and context in message")
    void duplicateTeamShouldIncludeContext() {
        DuplicateTeamException ex = new DuplicateTeamException("DIN", "enrolled in this season");
        String msg = ex.getMessage();
        assertTrue(msg.contains("DIN"), "Message must contain team code");
        assertTrue(msg.contains("enrolled"), "Message must contain context");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("EntityNotFoundException should include entity type and identifier in message")
    void entityNotFoundShouldIncludeContext() {
        EntityNotFoundException ex = new EntityNotFoundException("League", "supersport-hnl");
        String msg = ex.getMessage();
        assertTrue(msg.contains("League"), "Message must contain entity type");
        assertTrue(msg.contains("supersport-hnl"), "Message must contain identifier");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("All domain exceptions should be unchecked (RuntimeException)")
    void allExceptionsShouldBeUnchecked() {
        assertInstanceOf(RuntimeException.class,
            new InvalidStateTransitionException("X", "Y", "Z"));
        assertInstanceOf(RuntimeException.class,
            new DuplicateTeamException("X", "Y"));
        assertInstanceOf(RuntimeException.class,
            new EntityNotFoundException("X", "Y"));
    }
}
