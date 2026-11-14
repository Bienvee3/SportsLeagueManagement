package com.league.sportsleague.infrastructure.scheduling;

import com.league.sportsleague.application.service.RoundLifecycleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Starts the real Quartz scheduler with the JDBC job store in cluster mode against the Flyway-created
 * tables (migration V11, H2 here, PostgreSQL in production). Proves the schema works, the jobs and
 * triggers are persisted, and that a fired job is built through the constructor and runs.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:quartz_testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.quartz.auto-startup=true",
    "spring.quartz.job-store-type=jdbc",
    "spring.quartz.jdbc.initialize-schema=never",
    "spring.quartz.scheduler-name=quartz-test-scheduler",
    "spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO",
    "spring.quartz.properties.org.quartz.jobStore.isClustered=true",
    "spring.quartz.properties.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate"
})
class QuartzJdbcSchedulerTest {

    @Autowired Scheduler scheduler;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean RoundLifecycleService roundLifecycleService;

    @Test
    @DisplayName("both jobs and their cron triggers are stored in the database")
    void jobsAreStoredInTheJdbcJobStore() throws Exception {
        assertNotNull(scheduler.getJobDetail(JobKey.jobKey("roundLifecycle")));
        assertNotNull(scheduler.getJobDetail(JobKey.jobKey("overdueFixtures")));
        CronTrigger lifecycle = (CronTrigger) scheduler.getTrigger(TriggerKey.triggerKey("roundLifecycleTrigger"));
        assertEquals("0 0/5 * * * ?", lifecycle.getCronExpression());

        Integer jobRows = jdbc.queryForObject(
                "select count(*) from qrtz_job_details where sched_name = 'quartz-test-scheduler'", Integer.class);
        Integer cronRows = jdbc.queryForObject(
                "select count(*) from qrtz_cron_triggers where sched_name = 'quartz-test-scheduler'", Integer.class);
        assertEquals(2, jobRows);
        assertEquals(2, cronRows);
    }

    @Test
    @DisplayName("this instance is registered in the cluster state table")
    void instanceIsRegisteredForClustering() {
        Integer instances = jdbc.queryForObject(
                "select count(*) from qrtz_scheduler_state where sched_name = 'quartz-test-scheduler'", Integer.class);
        assertTrue(instances >= 1);
    }

    @Test
    @DisplayName("firing the lifecycle job builds it with its constructor dependency and runs the service")
    void firedJobRunsTheService() throws Exception {
        scheduler.triggerJob(JobKey.jobKey("roundLifecycle"));

        verify(roundLifecycleService, timeout(15_000)).advanceRounds();
    }
}
