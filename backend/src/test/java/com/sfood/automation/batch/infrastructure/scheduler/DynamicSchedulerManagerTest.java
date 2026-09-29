package com.sfood.automation.batch.infrastructure.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sfood.automation.batch.application.port.BatchJobExecutor;
import com.sfood.automation.batch.domain.enums.BatchScheduleType;
import com.sfood.automation.batch.domain.model.BatchScheduleConfig;
import com.sfood.automation.batch.domain.port.BatchScheduleConfigRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

class DynamicSchedulerManagerTest {

    @Test
    void should_register_enabled_schedules_on_initialize() {
        FakeTaskScheduler taskScheduler = new FakeTaskScheduler();
        BatchScheduleConfigRepository repository = org.mockito.Mockito.mock(BatchScheduleConfigRepository.class);
        BatchJobExecutor batchJobExecutor = org.mockito.Mockito.mock(BatchJobExecutor.class);
        when(repository.findAllByEnabledTrue()).thenReturn(List.of(config(true, 600)));

        DynamicSchedulerManager manager = new DynamicSchedulerManager(taskScheduler, repository, batchJobExecutor, false);

        manager.initializeSchedules();

        org.assertj.core.api.Assertions.assertThat(taskScheduler.scheduleCount).isEqualTo(1);
    }

    @Test
    void should_not_register_disabled_schedule() {
        FakeTaskScheduler taskScheduler = new FakeTaskScheduler();
        BatchScheduleConfigRepository repository = org.mockito.Mockito.mock(BatchScheduleConfigRepository.class);
        BatchJobExecutor batchJobExecutor = org.mockito.Mockito.mock(BatchJobExecutor.class);

        DynamicSchedulerManager manager = new DynamicSchedulerManager(taskScheduler, repository, batchJobExecutor, false);

        manager.register(config(false, 600));

        org.assertj.core.api.Assertions.assertThat(taskScheduler.scheduleCount).isZero();
    }

    @Test
    void should_cancel_then_register_when_reschedule_called() {
        FakeTaskScheduler taskScheduler = new FakeTaskScheduler();
        BatchScheduleConfigRepository repository = org.mockito.Mockito.mock(BatchScheduleConfigRepository.class);
        BatchJobExecutor batchJobExecutor = org.mockito.Mockito.mock(BatchJobExecutor.class);

        DynamicSchedulerManager manager = new DynamicSchedulerManager(taskScheduler, repository, batchJobExecutor, false);
        manager.register(config(true, 600));

        FakeScheduledFuture first = taskScheduler.lastScheduledFuture;
        manager.reschedule(config(true, 300));

        org.assertj.core.api.Assertions.assertThat(taskScheduler.scheduleCount).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(first.cancelled).isTrue();
    }

    @Test
    void should_ignore_missing_job_on_cancel() {
        FakeTaskScheduler taskScheduler = new FakeTaskScheduler();
        BatchScheduleConfigRepository repository = org.mockito.Mockito.mock(BatchScheduleConfigRepository.class);
        BatchJobExecutor batchJobExecutor = org.mockito.Mockito.mock(BatchJobExecutor.class);

        DynamicSchedulerManager manager = new DynamicSchedulerManager(taskScheduler, repository, batchJobExecutor, false);

        org.assertj.core.api.Assertions.assertThatCode(() -> manager.cancel("missing-job")).doesNotThrowAnyException();
    }

    @Test
    void should_delegate_running_state_lookup() {
        FakeTaskScheduler taskScheduler = new FakeTaskScheduler();
        BatchScheduleConfigRepository repository = org.mockito.Mockito.mock(BatchScheduleConfigRepository.class);
        BatchJobExecutor batchJobExecutor = org.mockito.Mockito.mock(BatchJobExecutor.class);
        when(batchJobExecutor.isRunning("product-sync")).thenReturn(true);

        DynamicSchedulerManager manager = new DynamicSchedulerManager(taskScheduler, repository, batchJobExecutor, false);

        org.assertj.core.api.Assertions.assertThat(manager.isRunning("product-sync")).isTrue();
        verify(batchJobExecutor).isRunning("product-sync");
    }

    private BatchScheduleConfig config(boolean enabled, int fixedDelaySeconds) {
        return new BatchScheduleConfig(
            1,
            "product-sync",
            enabled,
            BatchScheduleType.FIXED_DELAY,
            fixedDelaySeconds,
            0,
            "sync job",
            null,
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z")
        );
    }

    private static final class FakeTaskScheduler implements TaskScheduler {
        private int scheduleCount;
        private FakeScheduledFuture lastScheduledFuture;

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
            return new FakeScheduledFuture();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            return new FakeScheduledFuture();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
            return new FakeScheduledFuture();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
            return new FakeScheduledFuture();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
            scheduleCount++;
            lastScheduledFuture = new FakeScheduledFuture();
            return lastScheduledFuture;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
            scheduleCount++;
            lastScheduledFuture = new FakeScheduledFuture();
            return lastScheduledFuture;
        }
    }

    private static final class FakeScheduledFuture implements ScheduledFuture<Object> {
        private boolean cancelled;

        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(Delayed other) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            this.cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    }
}
