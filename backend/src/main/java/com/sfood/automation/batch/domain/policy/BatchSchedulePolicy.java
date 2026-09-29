package com.sfood.automation.batch.domain.policy;

import com.sfood.automation.common.exception.ScheduleValidationException;
import org.springframework.stereotype.Component;

@Component
public class BatchSchedulePolicy {

    public void validate(int fixedDelaySeconds) {
        if (fixedDelaySeconds < 60) {
            throw new ScheduleValidationException();
        }
    }
}
