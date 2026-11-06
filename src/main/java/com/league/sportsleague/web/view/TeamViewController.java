package com.league.sportsleague.web.view;

import com.league.sportsleague.application.dto.CreateTeamRequest;
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
    public String list(Model model) {
        model.addAttribute("teams", teamService.findAll());
        model.addAttribute("currentPage", "teams");
        return "teams/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("team", teamService.findById(id));
        model.addAttribute("currentPage", "teams");
        return "teams/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("form", new CreateTeamRequest("", "", "", "", null));
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
                    form.stadiumName(),
                    form.city(),
                    form.foundedYear()));
            ra.addFlashAttribute("flashMsg", "Team created.");
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
        ra.addFlashAttribute("flashMsg", "Team updated.");
        return "redirect:/teams/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            teamService.deleteTeam(id);
            ra.addFlashAttribute("flashMsg", "Team deleted.");
            return "redirect:/teams";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg",
                    "Cannot delete team — it may be registered in one or more seasons. Withdraw it from all seasons first.");
            return "redirect:/teams/" + id;
        }
    }
}
