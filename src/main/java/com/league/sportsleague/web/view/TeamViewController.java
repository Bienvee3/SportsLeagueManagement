package com.league.sportsleague.web.view;

import com.league.sportsleague.application.dto.CreateTeamRequest;
import com.league.sportsleague.application.dto.PlayerRequest;
import com.league.sportsleague.application.dto.TeamResponse;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.application.dto.UpdateTeamRequest;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/teams")
public class TeamViewController {

    private final TeamService teamService;

    public TeamViewController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String sport, Model model) {
        var all = teamService.findAll();
        String selected = (sport == null || sport.isBlank()) ? null : sport.toUpperCase();
        var teams = selected == null ? all
                : all.stream().filter(t -> t.sportType().equalsIgnoreCase(selected)).toList();
        model.addAttribute("teams", teams);
        model.addAttribute("sports", all.stream().map(TeamResponse::sportType).distinct().sorted().toList());
        model.addAttribute("selectedSport", selected);
        model.addAttribute("currentPage", "teams");
        return "teams/list";
    }

    /**
     * @param fixture set when the page was opened from a match sheet ("Manage squad"): the back arrow
     *                then returns to that fixture instead of the team list
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id,
                         @RequestParam(required = false) UUID fixture,
                         Model model) {
        model.addAttribute("team", teamService.findById(id));
        model.addAttribute("players", teamService.findPlayers(id));
        model.addAttribute("returnFixture", fixture);
        model.addAttribute("currentPage", "teams");
        return "teams/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("form", new CreateTeamRequest("", "", Team.DEFAULT_SPORT, "", "", null));
        model.addAttribute("editMode", false);
        model.addAttribute("currentPage", "teams");
        return "teams/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String create(@ModelAttribute("form") @Valid CreateTeamRequest form,
                         BindingResult br,
                         Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("form", form);
            model.addAttribute("editMode", false);
            model.addAttribute("currentPage", "teams");
            return "teams/form";
        }
        try {
            UUID id = teamService.createTeam(new CreateTeamRequest(
                    form.name(),
                    form.shortCode() != null ? form.shortCode().toUpperCase() : "",
                    form.sportType(),
                    form.stadiumName(),
                    form.city(),
                    form.foundedYear()));
            ra.addFlashAttribute("flashKey", "teams.flash.created");
            return "redirect:/teams/" + id;
        } catch (Exception e) {
            model.addAttribute("form", form);
            model.addAttribute("editMode", false);
            model.addAttribute("currentPage", "teams");
            model.addAttribute("errorMsg", e.getMessage());
            return "teams/form";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editForm(@PathVariable UUID id, Model model) {
        var t = teamService.findById(id);
        model.addAttribute("team", t);
        model.addAttribute("form", new UpdateTeamRequest(t.name(), t.stadiumName(), t.city(), t.foundedYear()));
        model.addAttribute("editMode", true);
        model.addAttribute("currentPage", "teams");
        return "teams/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String update(@PathVariable UUID id,
                         @ModelAttribute("form") @Valid UpdateTeamRequest form,
                         BindingResult br,
                         Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("team", teamService.findById(id));
            model.addAttribute("form", form);
            model.addAttribute("editMode", true);
            model.addAttribute("currentPage", "teams");
            return "teams/form";
        }
        teamService.updateProfile(id, form.name(), form.stadiumName(), form.city(), form.foundedYear());
        ra.addFlashAttribute("flashKey", "teams.flash.updated");
        return "redirect:/teams/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            teamService.deleteTeam(id);
            ra.addFlashAttribute("flashKey", "teams.flash.deleted");
            return "redirect:/teams";
        } catch (Exception e) {
            ra.addFlashAttribute("errorKey", "teams.error.deleteInUse");
            return "redirect:/teams/" + id;
        }
    }

    // ── ADMIN: squad ──────────────────────────────────────────────────────────

    @PostMapping("/{id}/players")
    @PreAuthorize("hasRole('ADMIN')")
    public String addPlayer(@PathVariable UUID id,
                            @Valid PlayerRequest form,
                            BindingResult br,
                            @RequestParam(required = false) UUID fixture,
                            RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMsg", "Invalid input: " + br.getAllErrors().get(0).getDefaultMessage());
            return teamPage(id, fixture);
        }
        try {
            teamService.addPlayer(id, form);
            ra.addFlashAttribute("flashKey", "teams.flash.playerAdded");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return teamPage(id, fixture);
    }

    /** Adds a whole squad at once from pasted text, one player per line: {@code <number> <name>[, <position>]}. */
    @PostMapping("/{id}/players/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public String addPlayers(@PathVariable UUID id,
                             @RequestParam(defaultValue = "") String lines,
                             @RequestParam(required = false) UUID fixture,
                             RedirectAttributes ra) {
        try {
            teamService.addPlayersFromText(id, lines);
            ra.addFlashAttribute("flashKey", "teams.flash.playersAdded");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            ra.addFlashAttribute("bulkLines", lines); // keep the pasted text so it can be corrected
        }
        return teamPage(id, fixture);
    }

    @GetMapping("/{id}/players/{playerId}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editPlayerForm(@PathVariable UUID id, @PathVariable UUID playerId, Model model) {
        model.addAttribute("team", teamService.findById(id));
        model.addAttribute("player", teamService.findPlayer(id, playerId));
        model.addAttribute("currentPage", "teams");
        return "teams/player-form";
    }

    @PostMapping("/{id}/players/{playerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updatePlayer(@PathVariable UUID id,
                               @PathVariable UUID playerId,
                               @Valid PlayerRequest form,
                               BindingResult br,
                               RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMsg", "Invalid input: " + br.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/teams/" + id + "/players/" + playerId + "/edit";
        }
        try {
            teamService.updatePlayer(id, playerId, form);
            ra.addFlashAttribute("flashKey", "teams.flash.playerUpdated");
            return "redirect:/teams/" + id;
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/teams/" + id + "/players/" + playerId + "/edit";
        }
    }

    @PostMapping("/{id}/players/{playerId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String removePlayer(@PathVariable UUID id,
                               @PathVariable UUID playerId,
                               @RequestParam(required = false) UUID fixture,
                               RedirectAttributes ra) {
        teamService.removePlayer(id, playerId);
        ra.addFlashAttribute("flashKey", "teams.flash.playerRemoved");
        return teamPage(id, fixture);
    }

    private static String teamPage(UUID teamId, UUID fixture) {
        return "redirect:/teams/" + teamId + (fixture == null ? "" : "?fixture=" + fixture);
    }
}
