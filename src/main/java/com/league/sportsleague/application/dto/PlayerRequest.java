package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlayerRequest(
        @NotBlank(message = "{validation.player.name.required}")
        @Size(max = 100, message = "{validation.player.name.size}")
        String name,

        @NotNull(message = "{validation.player.shirt.required}")
        @Min(value = 1, message = "{validation.player.shirt.min}")
        @Max(value = 99, message = "{validation.player.shirt.max}")
        Integer shirtNumber,

        @Size(max = 30, message = "{validation.player.position.size}")
        String position
) {}
