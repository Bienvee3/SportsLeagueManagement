package com.league.sportsleague.web.view;

import com.league.sportsleague.application.service.LeagueService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final LeagueService leagueService;

    public DashboardController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("leagues", leagueService.findAll());
        model.addAttribute("currentPage", "dashboard");
        return "dashboard";
    }
}
