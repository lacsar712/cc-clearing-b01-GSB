package com.clearing.netting.domain.port.out;

import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ObligationRepositoryPort {
    TradeObligation save(TradeObligation obligation);

    List<TradeObligation> saveAll(List<TradeObligation> obligations);

    Optional<TradeObligation> findById(String obligationId);

    List<TradeObligation> findAll();

    List<TradeObligation> findByFilters(String currency, LocalDate settleDate, ObligationStatus status);

    List<TradeObligation> findOpenBySettleDateAndCurrency(LocalDate settleDate, String currency);

    List<TradeObligation> findByNettingRunId(String runId);
}
