package com.league.sportsleague.infrastructure.scheduling;

import com.league.sportsleague.application.dto.OverdueFixtureResponse;
import com.league.sportsleague.application.service.OverdueFixtureService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Operational job: reports SCHEDULED matches whose kick-off passed more than a grace period ago without
 * anyone starting, postponing or cancelling them. It only reports (one WARN line per match): the decision
 * belongs to an official.
 */
@DisallowConcurrentExecution
public class OverdueFixtureJob extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(OverdueFixtureJob.class);

    private final OverdueFixtureService overdueFixtureService;
    private final Duration grace;

    public OverdueFixtureJob(OverdueFixtureService overdueFixtureService,
                             @Value("${app.jobs.overdue-fixtures.grace:PT3H}") Duration grace) {
        this.overdueFixtureService = overdueFixtureService;
        this.grace = grace;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        List<OverdueFixtureResponse> overdue = overdueFixtureService.findOverdue(Instant.now(), grace);
        if (overdue.isEmpty()) {
            log.debug("Overdue fixture job: none");
            return;
        }
        log.warn("Overdue fixture job: {} match(es) still SCHEDULED more than {} after kick-off time", overdue.size(), grace);
        for (OverdueFixtureResponse f : overdue) {
            log.warn("  round {}: {} - {} was due at {} (fixture {})",
                    f.roundNumber(), f.homeTeam(), f.awayTeam(), f.scheduledTime(), f.fixtureId());
        }
    }
}
