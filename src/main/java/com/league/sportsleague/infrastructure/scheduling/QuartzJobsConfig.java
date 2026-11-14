package com.league.sportsleague.infrastructure.scheduling;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * The two scheduled jobs and their cron triggers. Spring Boot registers every {@link JobDetail} and
 * {@link Trigger} bean in the Quartz scheduler, whose JDBC job store (tables from migration V11) keeps them
 * in the database and, in cluster mode, lets only one instance fire each trigger.
 *
 * <p>Cron expressions come from {@code app.jobs.*} so a deployment can change them without code. A missed
 * firing (application down at the time) is skipped, not replayed: both jobs are idempotent and the next
 * run catches up.
 */
@Configuration
public class QuartzJobsConfig {

    static final String ROUND_LIFECYCLE = "roundLifecycle";
    static final String OVERDUE_FIXTURES = "overdueFixtures";

    @Bean
    JobDetail roundLifecycleJobDetail() {
        return JobBuilder.newJob(RoundLifecycleJob.class)
                .withIdentity(ROUND_LIFECYCLE)
                .withDescription("Starts and completes rounds from the state of their matches")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger roundLifecycleTrigger(JobDetail roundLifecycleJobDetail,
                                  @Value("${app.jobs.round-lifecycle.cron:0 0/5 * * * ?}") String cron) {
        return cronTrigger(roundLifecycleJobDetail, ROUND_LIFECYCLE + "Trigger", cron);
    }

    @Bean
    JobDetail overdueFixturesJobDetail() {
        return JobBuilder.newJob(OverdueFixtureJob.class)
                .withIdentity(OVERDUE_FIXTURES)
                .withDescription("Reports SCHEDULED matches that should already have started")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger overdueFixturesTrigger(JobDetail overdueFixturesJobDetail,
                                   @Value("${app.jobs.overdue-fixtures.cron:0 0 * * * ?}") String cron) {
        return cronTrigger(overdueFixturesJobDetail, OVERDUE_FIXTURES + "Trigger", cron);
    }

    /**
     * Jobs get their collaborators through the constructor, like every other class of the project.
     * Quartz needs a no-argument constructor by default (setter/field population), so this factory
     * creates each job through the bean factory, which resolves constructor parameters.
     */
    @Bean
    SchedulerFactoryBeanCustomizer constructorInjectionJobFactory(ApplicationContext applicationContext) {
        AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
        return schedulerFactoryBean -> schedulerFactoryBean.setJobFactory(new SpringBeanJobFactory() {
            @Override
            protected Object createJobInstance(TriggerFiredBundle bundle) {
                return beanFactory.createBean(bundle.getJobDetail().getJobClass());
            }
        });
    }

    private static Trigger cronTrigger(JobDetail job, String name, String cron) {
        return TriggerBuilder.newTrigger()
                .forJob(job)
                .withIdentity(name)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron).withMisfireHandlingInstructionDoNothing())
                .build();
    }
}
