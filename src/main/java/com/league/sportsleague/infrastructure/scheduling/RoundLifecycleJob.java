package com.league.sportsleague.infrastructure.scheduling;

import com.league.sportsleague.application.service.RoundLifecycleService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Domain job: starts and completes rounds of active seasons from the state of their matches, so nobody has
 * to press a button to close a round. Delegates to {@link RoundLifecycleService}; the job itself only adds
 * scheduling and logging. Not concurrent: two overlapping runs would read the same rounds.
 */
@DisallowConcurrentExecution
public class RoundLifecycleJob extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(RoundLifecycleJob.class);

    private final RoundLifecycleService roundLifecycleService;

    public RoundLifecycleJob(RoundLifecycleService roundLifecycleService) {
        this.roundLifecycleService = roundLifecycleService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        int changed = roundLifecycleService.advanceRounds();
        if (changed > 0) {
            log.info("Round lifecycle job: {} round(s) changed status", changed);
        } else {
            log.debug("Round lifecycle job: nothing to advance");
        }
    }
}
