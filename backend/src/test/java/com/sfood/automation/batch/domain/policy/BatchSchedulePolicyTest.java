package com.sfood.automation.batch.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.ScheduleValidationException;
import org.junit.jupiter.api.Test;

class BatchSchedulePolicyTest {

    private final BatchSchedulePolicy batchSchedulePolicy = new BatchSchedulePolicy();

    @Test
    void should_pass_when_fixed_delay_is_60_seconds() {
        assertThatCode(() -> batchSchedulePolicy.validate(60)).doesNotThrowAnyException();
    }

    @Test
    void should_fail_when_fixed_delay_is_59_seconds() {
        assertThatThrownBy(() -> batchSchedulePolicy.validate(59))
            .isInstanceOf(ScheduleValidationException.class);
    }

    @Test
    void should_fail_when_fixed_delay_is_zero() {
        assertThatThrownBy(() -> batchSchedulePolicy.validate(0))
            .isInstanceOf(ScheduleValidationException.class);
    }
}
