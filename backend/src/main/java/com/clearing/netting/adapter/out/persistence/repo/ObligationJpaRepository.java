package com.clearing.netting.adapter.out.persistence.repo;

import com.clearing.netting.adapter.out.persistence.entity.ObligationJpaEntity;
import com.clearing.netting.domain.model.ObligationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface ObligationJpaRepository
        extends JpaRepository<ObligationJpaEntity, String>, JpaSpecificationExecutor<ObligationJpaEntity> {

    List<ObligationJpaEntity> findBySettleDateAndCurrencyIgnoreCaseAndStatus(
            LocalDate settleDate, String currency, ObligationStatus status);

    List<ObligationJpaEntity> findByNettingRunId(String nettingRunId);
}
