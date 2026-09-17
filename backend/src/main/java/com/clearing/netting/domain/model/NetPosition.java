package com.clearing.netting.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public class NetPosition {
    private final String positionId;
    private final String runId;
    private final String memberId;
    private final String currency;
    private final BigDecimal netAmount;

    public NetPosition(String positionId, String runId, String memberId, String currency, BigDecimal netAmount) {
        this.positionId = Objects.requireNonNull(positionId);
        this.runId = Objects.requireNonNull(runId);
        this.memberId = Objects.requireNonNull(memberId);
        this.currency = Objects.requireNonNull(currency).toUpperCase();
        this.netAmount = Objects.requireNonNull(netAmount).setScale(8, RoundingMode.HALF_UP);
    }

    public static NetPosition of(String runId, String memberId, String currency, BigDecimal netAmount) {
        return new NetPosition(UUID.randomUUID().toString(), runId, memberId, currency, netAmount);
    }

    public String getPositionId() {
        return positionId;
    }

    public String getRunId() {
        return runId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }
}
