package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateSeasonRequest(
        @NotBlank(message = "{validation.season.name.required}")
        String name,

        @Min(value = 1900, message = "{validation.season.yearStart.min}")
        int yearStart,

        @Min(value = 1900, message = "{validation.season.yearEnd.min}")
        int yearEnd
) {}
