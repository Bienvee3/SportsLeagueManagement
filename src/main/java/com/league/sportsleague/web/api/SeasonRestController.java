package com.league.sportsleague.web.api;

import com.league.sportsleague.application.dto.AddFixtureRequest;
import com.league.sportsleague.application.dto.CreateSeasonRequest;
import com.league.sportsleague.application.dto.PlayerStatsResponse;
import com.league.sportsleague.application.dto.RegisterTeamRequest;
import com.league.sportsleague.application.dto.RoundResponse;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
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

    @GetMapping("/{id}/player-stats")
    @Operation(summary = "Get player statistics (goals, assists, cards, appearances, minutes) for a season")
    public PlayerStatsResponse getPlayerStats(@PathVariable UUID leagueId, @PathVariable UUID id) {
        return seasonService.getPlayerStats(id);
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

    @GetMapping("/{id}/rounds")
    @Operation(summary = "List the rounds of a season, ordered by number")
    public List<RoundResponse> listRounds(@PathVariable UUID leagueId, @PathVariable UUID id) {
        return seasonService.findRounds(id);
    }

    @PostMapping("/{id}/rounds")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create the next round; the number is assigned automatically (ADMIN)")
    public ResponseEntity<Void> createRound(@PathVariable UUID leagueId,
                                            @PathVariable UUID id,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        UUID roundId = seasonService.createRound(id, startDate);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{roundId}").buildAndExpand(roundId).toUri();
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}/rounds/{roundId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a round without finished matches; later rounds are renumbered (ADMIN)")
    public ResponseEntity<Void> deleteRound(@PathVariable UUID leagueId,
                                            @PathVariable UUID id,
                                            @PathVariable UUID roundId) {
        seasonService.deleteRound(id, roundId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rounds/{roundId}/fixtures")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add one match to a round (ADMIN)")
    public ResponseEntity<Void> addFixture(@PathVariable UUID leagueId,
                                           @PathVariable UUID id,
                                           @PathVariable UUID roundId,
                                           @Valid @RequestBody AddFixtureRequest request) {
        seasonService.addFixture(id, roundId, request.homeTeamId(), request.awayTeamId(), request.scheduledTime());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/rounds/{roundId}/fixtures/{fixtureId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a match that has not been played (ADMIN)")
    public ResponseEntity<Void> deleteFixture(@PathVariable UUID leagueId,
                                              @PathVariable UUID id,
                                              @PathVariable UUID roundId,
                                              @PathVariable UUID fixtureId) {
        seasonService.deleteFixture(id, roundId, fixtureId);
        return ResponseEntity.noContent().build();
    }
}
