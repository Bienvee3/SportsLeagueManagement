package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.enums.MatchEventType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RecordMatchEventRequest(
        @NotNull(message = "Team ID must not be null")
        UUID teamId,

        @NotNull(message = "Event type must not be null")
        MatchEventType eventType,

        @Min(value = 1, message = "Match minute must be >= 1")
        @Max(value = 130, message = "Match minute must be <= 130")
        int matchMinute,

        @NotBlank(message = "Player name must not be blank")
        String playerName,

        String assistingPlayer,
        String notes
) {}
