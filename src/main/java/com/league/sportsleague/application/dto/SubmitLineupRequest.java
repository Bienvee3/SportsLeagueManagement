package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** The lineup of one team for a fixture: starters, substitutes and an optional captain (a starter). */
public record SubmitLineupRequest(
        @NotEmpty(message = "{validation.lineup.starters.required}")
        List<UUID> starterIds,

        List<UUID> benchIds,

        UUID captainId
) {
    public SubmitLineupRequest {
        starterIds = starterIds == null ? List.of() : List.copyOf(starterIds);
        benchIds = benchIds == null ? List.of() : List.copyOf(benchIds);
    }
}
