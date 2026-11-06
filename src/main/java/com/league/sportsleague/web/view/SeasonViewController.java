package com.league.sportsleague.web.view;

import com.league.sportsleague.application.dto.CreateSeasonRequest;
import com.league.sportsleague.application.dto.RegisterTeamRequest;
import com.league.sportsleague.application.service.FixtureService;
import com.league.sportsleague.application.service.LeagueService;
import com.league.sportsleague.application.service.SeasonService;
import com.league.sportsleague.application.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Instant;
import java.util.UUID;

@Controller
@RequestMapping("/leagues/{leagueId}/seasons")
public class SeasonViewController {

    private final SeasonService seasonService;
    private final LeagueService leagueService;
    private final TeamService teamService;
    private final FixtureService fixtureService;

    public SeasonViewController(SeasonService seasonService,
                                LeagueService leagueService,
                                TeamService teamService,
                                FixtureService fixtureService) {
        this.seasonService = seasonService;
        this.leagueService = leagueService;
        this.teamService = teamService;
        this.fixtureService = fixtureService;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID leagueId,
                         @PathVariable UUID id,
                         Model model) {
        var season = seasonService.findById(id);
        var standings = seasonService.getStandings(id);
        var registeredTeams = seasonService.findRegisteredTeams(id);
        var allTeams        = teamService.findAll();
        var registeredIds   = registeredTeams.stream().map(t -> t.id()).collect(java.util.stream.Collectors.toSet());
        var availableTeams  = allTeams.stream().filter(t -> !registeredIds.contains(t.id())).toList();
        model.addAttribute("season", season);
        model.addAttribute("league", leagueService.findById(leagueId));
        model.addAttribute("standings", standings);
        model.addAttribute("registeredTeams", registeredTeams);
        model.addAttribute("availableTeams", availableTeams);
        model.addAttribute("currentPage", "leagues");
        return "seasons/detail";
    }

    @GetMapping("/{id}/calendar")
    public String calendar(@PathVariable UUID leagueId,
                           @PathVariable UUID id,
                           Model model) {
        var season = seasonService.findById(id);
        var fixtures = fixtureService.findAllBySeason(id);
        model.addAttribute("season", season);
        model.addAttribute("league", leagueService.findById(leagueId));
        model.addAttribute("fixtures", fixtures);
        model.addAttribute("currentPage", "calendar");
        return "seasons/calendar";
    }

    // ── ADMIN: create ─────────────────────────────────────────────────────────

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newForm(@PathVariable UUID leagueId, Model model) {
        model.addAttribute("league", leagueService.findById(leagueId));
        model.addAttribute("form", new CreateSeasonRequest("", 2026, 2027));
        model.addAttribute("currentPage", "leagues");
        return "seasons/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String create(@PathVariable UUID leagueId,
                         @ModelAttribute("form") @Valid CreateSeasonRequest form,
                         BindingResult br,
                         Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("league", leagueService.findById(leagueId));
            model.addAttribute("form", form);
            model.addAttribute("currentPage", "leagues");
            return "seasons/form";
        }
        UUID id = seasonService.createSeason(leagueId, form);
        ra.addFlashAttribute("flashMsg", "Season created.");
        return "redirect:/leagues/" + leagueId + "/seasons/" + id;
    }

    // ── ADMIN: lifecycle ──────────────────────────────────────────────────────

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public String activate(@PathVariable UUID leagueId,
                           @PathVariable UUID id,
                           RedirectAttributes ra) {
        seasonService.activateSeason(id);
        ra.addFlashAttribute("flashMsg", "Season activated.");
        return "redirect:/leagues/" + leagueId + "/seasons/" + id;
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public String complete(@PathVariable UUID leagueId,
                           @PathVariable UUID id,
                           RedirectAttributes ra) {
        seasonService.completeSeason(id);
        ra.addFlashAttribute("flashMsg", "Season completed.");
        return "redirect:/leagues/" + leagueId + "/seasons/" + id;
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateSchedule(@PathVariable UUID leagueId,
                                   @PathVariable UUID id,
                                   RedirectAttributes ra) {
        seasonService.generateSchedule(id, Instant.now());
        ra.addFlashAttribute("flashMsg", "Schedule generated.");
        return "redirect:/leagues/" + leagueId + "/seasons/" + id + "/calendar";
    }

    @PostMapping("/{id}/register-team")
    @PreAuthorize("hasRole('ADMIN')")
    public String registerTeam(@PathVariable UUID leagueId,
                               @PathVariable UUID id,
                               @Valid RegisterTeamRequest form,
                               RedirectAttributes ra) {
        seasonService.registerTeam(id, form.teamId(), form.initialSeed());
        ra.addFlashAttribute("flashMsg", "Team registered.");
        return "redirect:/leagues/" + leagueId + "/seasons/" + id;
    }

    @PostMapping("/{id}/withdraw-team/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String withdrawTeam(@PathVariable UUID leagueId,
                               @PathVariable UUID id,
                               @PathVariable UUID teamId,
                               RedirectAttributes ra) {
        seasonService.withdrawTeam(id, teamId);
        ra.addFlashAttribute("flashMsg", "Team withdrawn.");
        return "redirect:/leagues/" + leagueId + "/seasons/" + id;
    }
}
