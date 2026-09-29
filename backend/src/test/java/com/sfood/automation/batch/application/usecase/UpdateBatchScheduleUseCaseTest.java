package com.sfood.automation.batch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sfood.automation.batch.application.port.BatchSchedulerPort;
import com.sfood.automation.batch.domain.enums.BatchScheduleType;
import com.sfood.automation.batch.domain.model.BatchScheduleChangeLog;
import com.sfood.automation.batch.domain.model.BatchScheduleConfig;
import com.sfood.automation.batch.domain.policy.BatchSchedulePolicy;
import com.sfood.automation.batch.domain.port.BatchScheduleChangeLogRepository;
import com.sfood.automation.batch.domain.port.BatchScheduleConfigRepository;
import com.sfood.automation.common.exception.ScheduleValidationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UpdateBatchScheduleUseCaseTest {

    @Test
    void should_save_change_log_and_reschedule_when_request_is_valid() {
        BatchScheduleConfigRepository configRepository = org.mockito.Mockito.mock(BatchScheduleConfigRepository.class);
        BatchScheduleChangeLogRepository changeLogRepository = org.mockito.Mockito.mock(BatchScheduleChangeLogRepository.class);
        BatchSchedulerPort batchSchedulerPort = org.mockito.Mockito.mock(BatchSchedulerPort.class);
        when(configRepository.findByJobName("product-sync")).thenReturn(java.util.Optional.of(existing()));

        UpdateBatchScheduleUseCase useCase = new UpdateBatchScheduleUseCase(
            configRepository,
            changeLogRepository,
            batchSchedulerPort,
            new BatchSchedulePolicy()
        );

        useCase.execute("product-sync", true, 300, "user@s-food.com");

        verify(configRepository).save(org.mockito.ArgumentMatchers.any(BatchScheduleConfig.class));
        var captor = forClass(BatchScheduleChangeLog.class);
        verify(changeLogRepository).save(captor.capture());
        verify(batchSchedulerPort).reschedule(org.mockito.ArgumentMatchers.any(BatchScheduleConfig.class));
        assertThat(captor.getValue().beforeFixedDelaySec()).isEqualTo(600);
        assertThat(captor.getValue().afterFixedDelaySec()).isEqualTo(300);
    }

    @Test
    void should_fail_when_fixed_delay_is_less_than_60() {
        BatchScheduleConfigRepository configRepository = org.mockito.Mockito.mock(BatchScheduleConfigRepository.class);
        BatchScheduleChangeLogRepository changeLogRepository = org.mockito.Mockito.mock(BatchScheduleChangeLogRepository.class);
        BatchSchedulerPort batchSchedulerPort = org.mockito.Mockito.mock(BatchSchedulerPort.class);
        when(configRepository.findByJobName("product-sync")).thenReturn(java.util.Optional.of(existing()));

        UpdateBatchScheduleUseCase useCase = new UpdateBatchScheduleUseCase(
            configRepository,
            changeLogRepository,
            batchSchedulerPort,
            new BatchSchedulePolicy()
        );

        assertThatThrownBy(() -> useCase.execute("product-sync", true, 30, "user@s-food.com"))
            .isInstanceOf(ScheduleValidationException.class);

        verify(configRepository, never()).save(org.mockito.ArgumentMatchers.any(BatchScheduleConfig.class));
        verify(changeLogRepository, never()).save(org.mockito.ArgumentMatchers.any(BatchScheduleChangeLog.class));
        verify(batchSchedulerPort, never()).reschedule(org.mockito.ArgumentMatchers.any(BatchScheduleConfig.class));
    }

    private BatchScheduleConfig existing() {
        return new BatchScheduleConfig(
            1,
            "product-sync",
            true,
            BatchScheduleType.FIXED_DELAY,
            600,
            0,
            "sync job",
            null,
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z")
        );
    }
}
