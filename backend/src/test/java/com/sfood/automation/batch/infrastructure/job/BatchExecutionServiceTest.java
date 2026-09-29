package com.sfood.automation.batch.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sfood.automation.batch.domain.enums.BatchExecutionStatus;
import com.sfood.automation.batch.domain.enums.BatchTriggeredBy;
import com.sfood.automation.batch.domain.model.BatchExecutionLog;
import com.sfood.automation.batch.domain.port.BatchExecutionLogRepository;
import com.sfood.automation.common.slack.SlackNotifier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class BatchExecutionServiceTest {

    @Test
    void should_execute_job_when_not_running() {
        ProductSyncBatchJob productSyncBatchJob = org.mockito.Mockito.mock(ProductSyncBatchJob.class);
        MemberSyncBatchJob memberSyncBatchJob = org.mockito.Mockito.mock(MemberSyncBatchJob.class);
        OrderSyncBatchJob orderSyncBatchJob = org.mockito.Mockito.mock(OrderSyncBatchJob.class);
        BatchExecutionLogRepository logRepository = org.mockito.Mockito.mock(BatchExecutionLogRepository.class);
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        when(productSyncBatchJob.run()).thenReturn("품절 1건, 정상 전환 0건");

        BatchExecutionService service = new BatchExecutionService(productSyncBatchJob, memberSyncBatchJob, orderSyncBatchJob, logRepository, slackNotifier);

        BatchExecutionStatus status = service.execute("product-sync", BatchTriggeredBy.MANUAL);

        assertThat(status).isEqualTo(BatchExecutionStatus.SUCCESS);
        verify(productSyncBatchJob).run();
        var captor = forClass(BatchExecutionLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(BatchExecutionStatus.SUCCESS);
        assertThat(captor.getValue().durationMs()).isNotNull();
    }

    @Test
    void should_skip_when_job_is_already_running() throws Exception {
        ProductSyncBatchJob productSyncBatchJob = org.mockito.Mockito.mock(ProductSyncBatchJob.class);
        MemberSyncBatchJob memberSyncBatchJob = org.mockito.Mockito.mock(MemberSyncBatchJob.class);
        OrderSyncBatchJob orderSyncBatchJob = org.mockito.Mockito.mock(OrderSyncBatchJob.class);
        BatchExecutionLogRepository logRepository = org.mockito.Mockito.mock(BatchExecutionLogRepository.class);
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(productSyncBatchJob.run()).thenAnswer(invocation -> {
            started.countDown();
            release.await();
            return "done";
        });

        BatchExecutionService service = new BatchExecutionService(productSyncBatchJob, memberSyncBatchJob, orderSyncBatchJob, logRepository, slackNotifier);
        var executor = Executors.newSingleThreadExecutor();
        try {
            Future<BatchExecutionStatus> first = executor.submit(() -> service.execute("product-sync", BatchTriggeredBy.SCHEDULER));
            started.await();

            BatchExecutionStatus second = service.execute("product-sync", BatchTriggeredBy.MANUAL);

            assertThat(second).isEqualTo(BatchExecutionStatus.SKIPPED);
            release.countDown();
            assertThat(first.get()).isEqualTo(BatchExecutionStatus.SUCCESS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void should_log_failure_and_notify_slack_when_job_throws() {
        ProductSyncBatchJob productSyncBatchJob = org.mockito.Mockito.mock(ProductSyncBatchJob.class);
        MemberSyncBatchJob memberSyncBatchJob = org.mockito.Mockito.mock(MemberSyncBatchJob.class);
        OrderSyncBatchJob orderSyncBatchJob = org.mockito.Mockito.mock(OrderSyncBatchJob.class);
        BatchExecutionLogRepository logRepository = org.mockito.Mockito.mock(BatchExecutionLogRepository.class);
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        when(productSyncBatchJob.run()).thenThrow(new IllegalStateException("WMS login failed"));

        BatchExecutionService service = new BatchExecutionService(productSyncBatchJob, memberSyncBatchJob, orderSyncBatchJob, logRepository, slackNotifier);

        BatchExecutionStatus status = service.execute("product-sync", BatchTriggeredBy.SCHEDULER);

        assertThat(status).isEqualTo(BatchExecutionStatus.FAIL);
        var captor = forClass(BatchExecutionLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(BatchExecutionStatus.FAIL);
        verify(slackNotifier).send("BatchExecutionService.execute", "[product-sync] WMS login failed");
    }

    @Test
    void should_reset_running_state_after_completion() {
        ProductSyncBatchJob productSyncBatchJob = org.mockito.Mockito.mock(ProductSyncBatchJob.class);
        MemberSyncBatchJob memberSyncBatchJob = org.mockito.Mockito.mock(MemberSyncBatchJob.class);
        OrderSyncBatchJob orderSyncBatchJob = org.mockito.Mockito.mock(OrderSyncBatchJob.class);
        BatchExecutionLogRepository logRepository = org.mockito.Mockito.mock(BatchExecutionLogRepository.class);
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        when(productSyncBatchJob.run()).thenReturn("done");

        BatchExecutionService service = new BatchExecutionService(productSyncBatchJob, memberSyncBatchJob, orderSyncBatchJob, logRepository, slackNotifier);

        service.execute("product-sync", BatchTriggeredBy.SCHEDULER);

        assertThat(service.isRunning("product-sync")).isFalse();
        service.execute("product-sync", BatchTriggeredBy.MANUAL);
        verify(slackNotifier, never()).send(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
