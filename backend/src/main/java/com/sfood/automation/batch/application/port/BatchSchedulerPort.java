package com.sfood.automation.batch.application.port;

import com.sfood.automation.batch.domain.model.BatchScheduleConfig;

public interface BatchSchedulerPort {
    void initializeSchedules();
    void register(BatchScheduleConfig config);
    void reschedule(BatchScheduleConfig config);
    void cancel(String jobName);
    boolean isRunning(String jobName);
}
