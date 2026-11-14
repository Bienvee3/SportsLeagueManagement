package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.enums.MatchEventType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * A match event. The player is given either as a squad player ({@code playerId}, mandatory when the
 * team filed a lineup) or, for teams without a lineup, as a free-text {@code playerName}.
 * {@code relatedPlayerId} is the assist provider of a goal or the player coming on in a substitution.
 */
public record RecordMatchEventRequest(
        @NotNull(message = "{validation.team.required}")
        UUID teamId,

        @NotNull(message = "{validation.event.type.required}")
        MatchEventType eventType,

        @Min(value = 1, message = "{validation.event.minute.min}")
        @Max(value = 130, message = "{validation.event.minute.max}")
        int matchMinute,

        String playerName,
        String assistingPlayer,
        String notes,
        UUID playerId,
        UUID relatedPlayerId
) {}
