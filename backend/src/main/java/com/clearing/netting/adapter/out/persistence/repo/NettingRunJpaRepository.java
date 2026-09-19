package com.clearing.netting.adapter.out.persistence.repo;

import com.clearing.netting.adapter.out.persistence.entity.NettingRunJpaEntity;
import com.clearing.netting.domain.model.NettingRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NettingRunJpaRepository extends JpaRepository<NettingRunJpaEntity, String> {
    List<NettingRunJpaEntity> findAllByOrderByCreatedAtDesc();

    Optional<NettingRunJpaEntity> findFirstBySettleDateAndCurrencyIgnoreCaseAndStatus(
            LocalDate settleDate, String currency, NettingRunStatus status);
}
