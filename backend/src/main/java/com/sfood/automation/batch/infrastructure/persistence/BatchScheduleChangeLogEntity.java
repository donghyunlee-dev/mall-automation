package com.sfood.automation.batch.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "batch_schedule_change_log")
public class BatchScheduleChangeLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "before_fixed_delay_sec")
    private Integer beforeFixedDelaySec;

    @Column(name = "after_fixed_delay_sec")
    private Integer afterFixedDelaySec;

    @Column(name = "before_enabled")
    private Integer beforeEnabled;

    @Column(name = "after_enabled")
    private Integer afterEnabled;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private String changedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Integer getBeforeFixedDelaySec() {
        return beforeFixedDelaySec;
    }

    public void setBeforeFixedDelaySec(Integer beforeFixedDelaySec) {
        this.beforeFixedDelaySec = beforeFixedDelaySec;
    }

    public Integer getAfterFixedDelaySec() {
        return afterFixedDelaySec;
    }

    public void setAfterFixedDelaySec(Integer afterFixedDelaySec) {
        this.afterFixedDelaySec = afterFixedDelaySec;
    }

    public Integer getBeforeEnabled() {
        return beforeEnabled;
    }

    public void setBeforeEnabled(Integer beforeEnabled) {
        this.beforeEnabled = beforeEnabled;
    }

    public Integer getAfterEnabled() {
        return afterEnabled;
    }

    public void setAfterEnabled(Integer afterEnabled) {
        this.afterEnabled = afterEnabled;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(String changedAt) {
        this.changedAt = changedAt;
    }
}
