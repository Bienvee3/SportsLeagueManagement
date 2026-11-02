package com.league.sportsleague.web.api;

import com.league.sportsleague.application.dto.CreateSeasonRequest;
import com.league.sportsleague.application.dto.RegisterTeamRequest;
import com.league.sportsleague.application.dto.SeasonResponse;
import com.league.sportsleague.application.dto.StandingsResponse;
import com.league.sportsleague.application.service.SeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leagues/{leagueId}/seasons")
@Tag(name = "Seasons", description = "Manage seasons within a league")
public class SeasonRestController {

    private final SeasonService seasonService;

    public SeasonRestController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping
    @Operation(summary = "List seasons for a league")
    public List<SeasonResponse> listByLeague(@PathVariable UUID leagueId) {
        return seasonService.findByLeague(leagueId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get season by ID")
    public SeasonResponse getById(@PathVariable UUID leagueId, @PathVariable UUID id) {
        return seasonService.findById(id);
    }

    @GetMapping("/{id}/standings")
    @Operation(summary = "Get standings for a season")
    public StandingsResponse getStandings(@PathVariable UUID leagueId, @PathVariable UUID id) {
        return seasonService.getStandings(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a season (ADMIN)")
    public ResponseEntity<Void> create(@PathVariable UUID leagueId,
                                       @Valid @RequestBody CreateSeasonRequest request) {
        UUID id = seasonService.createSeason(leagueId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).build();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a season (ADMIN)")
    public ResponseEntity<Void> activate(@PathVariable UUID leagueId, @PathVariable UUID id) {
        seasonService.activateSeason(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete a season (ADMIN)")
    public ResponseEntity<Void> complete(@PathVariable UUID leagueId, @PathVariable UUID id) {
        seasonService.completeSeason(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/register-team")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a team to the season (ADMIN)")
    public ResponseEntity<Void> registerTeam(@PathVariable UUID leagueId,
                                             @PathVariable UUID id,
                                             @Valid @RequestBody RegisterTeamRequest request) {
        seasonService.registerTeam(id, request.teamId(), request.initialSeed());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/withdraw-team/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Withdraw a team from the season (ADMIN)")
    public ResponseEntity<Void> withdrawTeam(@PathVariable UUID leagueId,
                                             @PathVariable UUID id,
                                             @PathVariable UUID teamId) {
        seasonService.withdrawTeam(id, teamId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate round-robin schedule (ADMIN)")
    public ResponseEntity<Void> generateSchedule(@PathVariable UUID leagueId,
                                                  @PathVariable UUID id,
                                                  @RequestParam Instant firstMatchTime) {
        seasonService.generateSchedule(id, firstMatchTime);
        return ResponseEntity.noContent().build();
    }
}
