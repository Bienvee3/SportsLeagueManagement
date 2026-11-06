package com.league.sportsleague.web.view;

import com.league.sportsleague.application.dto.CreateLeagueRequest;
import com.league.sportsleague.application.dto.UpdateLeagueRequest;
import com.league.sportsleague.application.service.LeagueService;
import com.league.sportsleague.application.service.SeasonService;
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
@RequestMapping("/leagues")
public class LeagueViewController {

    private final LeagueService leagueService;
    private final SeasonService seasonService;

    public LeagueViewController(LeagueService leagueService, SeasonService seasonService) {
        this.leagueService = leagueService;
        this.seasonService = seasonService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("leagues", leagueService.findAll());
        model.addAttribute("currentPage", "leagues");
        return "leagues/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        var league = leagueService.findById(id);
        model.addAttribute("league", league);
        model.addAttribute("seasons", seasonService.findByLeague(id));
        model.addAttribute("currentPage", "leagues");
        return "leagues/detail";
    }

    // ── ADMIN: create ─────────────────────────────────────────────────────────

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("form", new CreateLeagueRequest("", "", "FOOTBALL", ""));
        model.addAttribute("editMode", false);
        model.addAttribute("currentPage", "leagues");
        return "leagues/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String create(@org.springframework.web.bind.annotation.ModelAttribute("form") @Valid CreateLeagueRequest form,
                         BindingResult br,
                         Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("form", form);
            model.addAttribute("editMode", false);
            model.addAttribute("currentPage", "leagues");
            return "leagues/form";
        }
        try {
            UUID id = leagueService.createLeague(form);
            ra.addFlashAttribute("flashMsg", "League created.");
            return "redirect:/leagues/" + id;
        } catch (Exception e) {
            model.addAttribute("form", form);
            model.addAttribute("editMode", false);
            model.addAttribute("currentPage", "leagues");
            model.addAttribute("errorMsg", e.getMessage());
            return "leagues/form";
        }
    }

    // ── ADMIN: edit ───────────────────────────────────────────────────────────

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editForm(@PathVariable UUID id, Model model) {
        var l = leagueService.findById(id);
        model.addAttribute("league", l);
        model.addAttribute("form", new UpdateLeagueRequest(l.name(), l.country(), l.sportType()));
        model.addAttribute("editMode", true);
        model.addAttribute("currentPage", "leagues");
        return "leagues/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String update(@PathVariable UUID id,
                         @org.springframework.web.bind.annotation.ModelAttribute("form") @Valid UpdateLeagueRequest form,
                         BindingResult br,
                         Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("league", leagueService.findById(id));
            model.addAttribute("form", form);
            model.addAttribute("editMode", true);
            model.addAttribute("currentPage", "leagues");
            return "leagues/form";
        }
        try {
            leagueService.rename(id, form);
            ra.addFlashAttribute("flashMsg", "League updated.");
            return "redirect:/leagues/" + id;
        } catch (Exception e) {
            model.addAttribute("league", leagueService.findById(id));
            model.addAttribute("form", form);
            model.addAttribute("editMode", true);
            model.addAttribute("currentPage", "leagues");
            model.addAttribute("errorMsg", e.getMessage());
            return "leagues/form";
        }
    }

    // ── ADMIN: delete ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable UUID id, RedirectAttributes ra) {
        leagueService.delete(id);
        ra.addFlashAttribute("flashMsg", "League deleted.");
        return "redirect:/leagues";
    }
}
