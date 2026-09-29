package com.sfood.automation.batch.infrastructure.scheduler;

import com.sfood.automation.batch.application.port.BatchJobExecutor;
import com.sfood.automation.batch.application.port.BatchSchedulerPort;
import com.sfood.automation.batch.domain.enums.BatchTriggeredBy;
import com.sfood.automation.batch.domain.model.BatchScheduleConfig;
import com.sfood.automation.batch.domain.port.BatchScheduleConfigRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class DynamicSchedulerManager implements BatchSchedulerPort, ApplicationRunner {

    private final TaskScheduler taskScheduler;
    private final BatchScheduleConfigRepository batchScheduleConfigRepository;
    private final BatchJobExecutor batchJobExecutor;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final boolean autoStart;

    public DynamicSchedulerManager(
        TaskScheduler taskScheduler,
        BatchScheduleConfigRepository batchScheduleConfigRepository,
        BatchJobExecutor batchJobExecutor,
        @Value("${app.batch.scheduler.auto-start:true}") boolean autoStart
    ) {
        this.taskScheduler = taskScheduler;
        this.batchScheduleConfigRepository = batchScheduleConfigRepository;
        this.batchJobExecutor = batchJobExecutor;
        this.autoStart = autoStart;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (autoStart && !isGradleTestExecution()) {
            initializeSchedules();
        }
    }

    private boolean isGradleTestExecution() {
        return System.getProperty("org.gradle.test.worker") != null;
    }

    @Override
    public void initializeSchedules() {
        batchScheduleConfigRepository.findAllByEnabledTrue().forEach(this::register);
    }

    @Override
    public void register(BatchScheduleConfig config) {
        if (!config.enabled()) {
            return;
        }
        ScheduledFuture<?> scheduledFuture = taskScheduler.scheduleWithFixedDelay(
            () -> batchJobExecutor.execute(config.jobName(), BatchTriggeredBy.SCHEDULER),
            Instant.now().plusSeconds(config.initialDelaySeconds()),
            Duration.ofSeconds(config.fixedDelaySeconds())
        );
        scheduledTasks.put(config.jobName(), scheduledFuture);
    }

    @Override
    public void reschedule(BatchScheduleConfig config) {
        cancel(config.jobName());
        if (config.enabled()) {
            register(config);
        }
    }

    @Override
    public void cancel(String jobName) {
        ScheduledFuture<?> scheduledFuture = scheduledTasks.remove(jobName);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    @Override
    public boolean isRunning(String jobName) {
        return batchJobExecutor.isRunning(jobName);
    }
}
