package com.clearing.netting.domain.port.out;

import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.NettingRunStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NettingRunRepositoryPort {
    NettingRun save(NettingRun run);

    Optional<NettingRun> findById(String runId);

    List<NettingRun> findAllOrderByCreatedAtDesc();

    Optional<NettingRun> findLatestBySettleDateAndCurrencyAndStatusIn(
            LocalDate settleDate, String currency, Collection<NettingRunStatus> statuses);
}
