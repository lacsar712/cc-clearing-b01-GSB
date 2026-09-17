package com.clearing.netting.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "net_positions")
public class NetPositionJpaEntity {

    @Id
    @Column(length = 64)
    private String positionId;

    @Column(nullable = false, length = 64)
    private String runId;

    @Column(nullable = false, length = 64)
    private String memberId;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(nullable = false, precision = 28, scale = 8)
    private BigDecimal netAmount;

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }
}
