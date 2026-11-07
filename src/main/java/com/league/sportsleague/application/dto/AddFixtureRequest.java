package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AddFixtureRequest(
        @NotNull(message = "Home team is required")
        UUID homeTeamId,

        @NotNull(message = "Away team is required")
        UUID awayTeamId,

        @NotNull(message = "Kick-off time is required")
        Instant scheduledTime
) {}
