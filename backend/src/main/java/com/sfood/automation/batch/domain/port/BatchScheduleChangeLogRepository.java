package com.sfood.automation.batch.domain.port;

import com.sfood.automation.batch.domain.model.BatchScheduleChangeLog;

public interface BatchScheduleChangeLogRepository {
    BatchScheduleChangeLog save(BatchScheduleChangeLog changeLog);
}
