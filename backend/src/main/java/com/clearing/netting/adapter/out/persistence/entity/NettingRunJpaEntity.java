package com.clearing.netting.adapter.out.persistence.entity;

import com.clearing.netting.domain.model.NettingRunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "netting_runs")
public class NettingRunJpaEntity {

    @Id
    @Column(length = 64)
    private String runId;

    @Column(nullable = false)
    private LocalDate settleDate;

    @Column(nullable = false, length = 8)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NettingRunStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 512)
    private String failureReason;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public LocalDate getSettleDate() {
        return settleDate;
    }

    public void setSettleDate(LocalDate settleDate) {
        this.settleDate = settleDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public NettingRunStatus getStatus() {
        return status;
    }

    public void setStatus(NettingRunStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
