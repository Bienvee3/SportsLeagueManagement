package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.StandingsEntry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StandingsResponse(
        UUID seasonId,
        String seasonName,
        Instant computedAt,
        List<StandingsEntry> entries
) {}
