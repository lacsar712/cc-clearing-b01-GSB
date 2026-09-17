package com.clearing.netting.adapter.out.persistence.entity;

import com.clearing.netting.domain.model.ObligationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trade_obligations")
public class ObligationJpaEntity {

    @Id
    @Column(length = 64)
    private String obligationId;

    @Column(nullable = false, length = 64)
    private String payerMemberId;

    @Column(nullable = false, length = 64)
    private String payeeMemberId;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(nullable = false, precision = 28, scale = 8)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate tradeDate;

    @Column(nullable = false)
    private LocalDate settleDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ObligationStatus status;

    @Column(length = 64)
    private String nettingRunId;

    public String getObligationId() {
        return obligationId;
    }

    public void setObligationId(String obligationId) {
        this.obligationId = obligationId;
    }

    public String getPayerMemberId() {
        return payerMemberId;
    }

    public void setPayerMemberId(String payerMemberId) {
        this.payerMemberId = payerMemberId;
    }

    public String getPayeeMemberId() {
        return payeeMemberId;
    }

    public void setPayeeMemberId(String payeeMemberId) {
        this.payeeMemberId = payeeMemberId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public LocalDate getSettleDate() {
        return settleDate;
    }

    public void setSettleDate(LocalDate settleDate) {
        this.settleDate = settleDate;
    }

    public ObligationStatus getStatus() {
        return status;
    }

    public void setStatus(ObligationStatus status) {
        this.status = status;
    }

    public String getNettingRunId() {
        return nettingRunId;
    }

    public void setNettingRunId(String nettingRunId) {
        this.nettingRunId = nettingRunId;
    }
}
