package com.sfood.automation.batch.presentation;

import com.sfood.automation.batch.application.dto.BatchRunResponse;
import com.sfood.automation.batch.application.dto.BatchScheduleResponse;
import com.sfood.automation.batch.application.dto.UpdateBatchScheduleRequest;
import com.sfood.automation.batch.application.usecase.GetBatchScheduleUseCase;
import com.sfood.automation.batch.application.usecase.RunBatchNowUseCase;
import com.sfood.automation.batch.application.usecase.UpdateBatchScheduleUseCase;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/batch-schedules")
public class BatchScheduleAdminController {

    private final GetBatchScheduleUseCase getBatchScheduleUseCase;
    private final UpdateBatchScheduleUseCase updateBatchScheduleUseCase;
    private final RunBatchNowUseCase runBatchNowUseCase;

    public BatchScheduleAdminController(
        GetBatchScheduleUseCase getBatchScheduleUseCase,
        UpdateBatchScheduleUseCase updateBatchScheduleUseCase,
        RunBatchNowUseCase runBatchNowUseCase
    ) {
        this.getBatchScheduleUseCase = getBatchScheduleUseCase;
        this.updateBatchScheduleUseCase = updateBatchScheduleUseCase;
        this.runBatchNowUseCase = runBatchNowUseCase;
    }

    @GetMapping("/{jobName}")
    ResponseEntity<BatchScheduleResponse> getSchedule(@PathVariable String jobName) {
        return ResponseEntity.ok(getBatchScheduleUseCase.execute(jobName));
    }

    @PutMapping("/{jobName}")
    ResponseEntity<Void> updateSchedule(@PathVariable String jobName, @Valid @RequestBody UpdateBatchScheduleRequest request) {
        updateBatchScheduleUseCase.execute(jobName, request.enabled(), request.fixedDelaySeconds(), request.modifiedBy());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{jobName}/run")
    ResponseEntity<BatchRunResponse> runNow(
        @PathVariable String jobName,
        @RequestParam(required = false) String targetDate
    ) {
        if (targetDate != null) {
            return ResponseEntity.ok(runBatchNowUseCase.execute(jobName, LocalDate.parse(targetDate)));
        }
        return ResponseEntity.ok(runBatchNowUseCase.execute(jobName));
    }
}
