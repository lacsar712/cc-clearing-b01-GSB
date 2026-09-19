package com.clearing.netting.domain.port.out;

import com.clearing.netting.domain.model.NettingRun;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NettingRunRepositoryPort {
    NettingRun save(NettingRun run);

    Optional<NettingRun> findById(String runId);

    List<NettingRun> findAllOrderByCreatedAtDesc();

    /**
     * Returns the successfully completed run (if any) for the same settleDate/currency.
     * Used as the idempotency guard against re-netting the same batch of OPEN obligations.
     */
    Optional<NettingRun> findCompletedBySettleDateAndCurrency(LocalDate settleDate, String currency);
}
