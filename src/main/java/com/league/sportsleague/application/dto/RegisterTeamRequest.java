package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterTeamRequest(
        @NotNull(message = "Team ID must not be null")
        UUID teamId,

        @Min(value = 0, message = "Initial seed must be >= 0")
        int initialSeed
) {}
