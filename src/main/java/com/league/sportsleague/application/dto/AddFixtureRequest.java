package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AddFixtureRequest(
        @NotNull(message = "{validation.fixture.home.required}")
        UUID homeTeamId,

        @NotNull(message = "{validation.fixture.away.required}")
        UUID awayTeamId,

        @NotNull(message = "{validation.fixture.kickoff.required}")
        Instant scheduledTime
) {}
