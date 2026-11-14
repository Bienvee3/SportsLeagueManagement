package com.league.sportsleague.infrastructure.scheduling;

import com.league.sportsleague.application.dto.OverdueFixtureResponse;
import com.league.sportsleague.application.service.OverdueFixtureService;
import com.league.sportsleague.application.service.RoundLifecycleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDetail;
import org.quartz.Trigger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledJobsTest {

    @Nested
    @DisplayName("RoundLifecycleJob")
    class RoundLifecycle {

        @Test
        @DisplayName("runs the lifecycle service once per firing")
        void delegatesToService() {
            RoundLifecycleService service = mock(RoundLifecycleService.class);
            when(service.advanceRounds()).thenReturn(2);

            new RoundLifecycleJob(service).executeInternal(null);

            verify(service).advanceRounds();
        }

        @Test
        @DisplayName("is not allowed to overlap with itself")
        void isNonConcurrent() {
            assertTrue(RoundLifecycleJob.class.isAnnotationPresent(DisallowConcurrentExecution.class));
        }
    }

    @Nested
    @DisplayName("OverdueFixtureJob")
    class OverdueFixtures {

        @Test
        @DisplayName("asks the service for matches overdue by the configured grace period")
        void usesConfiguredGrace() {
            OverdueFixtureService service = mock(OverdueFixtureService.class);
            Duration grace = Duration.ofHours(5);
            when(service.findOverdue(any(Instant.class), eq(grace))).thenReturn(List.of(
                    new OverdueFixtureResponse(UUID.randomUUID(), "Dinamo", "Hajduk", 3, Instant.parse("2026-11-14T08:00:00Z"))));

            new OverdueFixtureJob(service, grace).executeInternal(null);

            verify(service).findOverdue(any(Instant.class), eq(grace));
        }

        @Test
        @DisplayName("handles the empty report")
        void emptyReport() {
            OverdueFixtureService service = mock(OverdueFixtureService.class);
            when(service.findOverdue(any(), any())).thenReturn(List.of());

            new OverdueFixtureJob(service, Duration.ofHours(3)).executeInternal(null);

            verify(service).findOverdue(any(), eq(Duration.ofHours(3)));
        }
    }

    @Nested
    @DisplayName("QuartzJobsConfig")
    class Config {

        private final QuartzJobsConfig config = new QuartzJobsConfig();

        @Test
        @DisplayName("the lifecycle job is durable, named and fires on the configured cron")
        void roundLifecycleDefinition() {
            JobDetail job = config.roundLifecycleJobDetail();
            Trigger trigger = config.roundLifecycleTrigger(job, "0 0/5 * * * ?");

            assertEquals(RoundLifecycleJob.class, job.getJobClass());
            assertEquals("roundLifecycle", job.getKey().getName());
            assertTrue(job.isDurable(), "a durable job stays registered even without a trigger");
            assertEquals(job.getKey(), trigger.getJobKey());
            assertEquals("0 0/5 * * * ?", ((CronTrigger) trigger).getCronExpression());
        }

        @Test
        @DisplayName("the overdue job is registered with its own cron")
        void overdueDefinition() {
            JobDetail job = config.overdueFixturesJobDetail();
            Trigger trigger = config.overdueFixturesTrigger(job, "0 0 * * * ?");

            assertEquals(OverdueFixtureJob.class, job.getJobClass());
            assertEquals("overdueFixtures", job.getKey().getName());
            assertEquals("0 0 * * * ?", ((CronTrigger) trigger).getCronExpression());
            assertNotNull(trigger.getKey());
        }

        @Test
        @DisplayName("an invalid cron expression fails fast at startup")
        void invalidCronFailsFast() {
            JobDetail job = config.roundLifecycleJobDetail();

            assertThrows(RuntimeException.class, () -> config.roundLifecycleTrigger(job, "every five minutes"));
        }
    }
}
