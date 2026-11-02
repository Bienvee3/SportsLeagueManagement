package com.league.sportsleague.web.api;

import com.league.sportsleague.application.dto.CreateTeamRequest;
import com.league.sportsleague.application.dto.TeamResponse;
import com.league.sportsleague.application.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@Tag(name = "Teams", description = "Manage teams")
public class TeamRestController {

    private final TeamService teamService;

    public TeamRestController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    @Operation(summary = "List all teams")
    public List<TeamResponse> listAll() {
        return teamService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team by ID")
    public TeamResponse getById(@PathVariable UUID id) {
        return teamService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a team (ADMIN)")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateTeamRequest request) {
        UUID id = teamService.createTeam(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a team (ADMIN)")
    public ResponseEntity<Void> update(@PathVariable UUID id,
                                       @Valid @RequestBody CreateTeamRequest request) {
        teamService.updateProfile(id, request.name(), request.stadiumName(),
                request.city(), request.foundedYear());
        return ResponseEntity.noContent().build();
    }
}
