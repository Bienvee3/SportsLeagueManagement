package com.league.sportsleague.web.view;

import com.league.sportsleague.application.service.LeagueService;
import com.league.sportsleague.application.service.SeasonService;
import com.league.sportsleague.application.service.TeamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final LeagueService leagueService;
    private final SeasonService seasonService;
    private final TeamService teamService;

    public DashboardController(LeagueService leagueService,
                               SeasonService seasonService,
                               TeamService teamService) {
        this.leagueService = leagueService;
        this.seasonService = seasonService;
        this.teamService = teamService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        var leagues = leagueService.findAll();
        var seasons = leagues.stream()
                .flatMap(l -> seasonService.findByLeague(l.id()).stream())
                .toList();
        long activeSeasons = seasons.stream().filter(s -> "ACTIVE".equals(s.status())).count();
        model.addAttribute("leagues", leagues);
        model.addAttribute("seasonCount", seasons.size());
        model.addAttribute("activeSeasonCount", activeSeasons);
        model.addAttribute("teamCount", teamService.findAll().size());
        model.addAttribute("currentPage", "dashboard");
        return "dashboard";
    }
}
