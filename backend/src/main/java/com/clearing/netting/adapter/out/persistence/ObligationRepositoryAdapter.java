package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.entity.ObligationJpaEntity;
import com.clearing.netting.adapter.out.persistence.repo.ObligationJpaRepository;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ObligationRepositoryAdapter implements ObligationRepositoryPort {

    private final ObligationJpaRepository repository;

    public ObligationRepositoryAdapter(ObligationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TradeObligation save(TradeObligation obligation) {
        return PersistenceMapper.toDomain(repository.save(PersistenceMapper.toEntity(obligation)));
    }

    @Override
    public List<TradeObligation> saveAll(List<TradeObligation> obligations) {
        return repository.saveAll(obligations.stream().map(PersistenceMapper::toEntity).collect(Collectors.toList()))
                .stream().map(PersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<TradeObligation> findById(String obligationId) {
        return repository.findById(obligationId).map(PersistenceMapper::toDomain);
    }

    @Override
    public List<TradeObligation> findAll() {
        return repository.findAll().stream().map(PersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TradeObligation> findByFilters(String currency, LocalDate settleDate, ObligationStatus status) {
        String ccy = currency == null || currency.isBlank() ? null : currency.trim().toUpperCase();
        Specification<ObligationJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ccy != null) {
                predicates.add(cb.equal(root.get("currency"), ccy));
            }
            if (settleDate != null) {
                predicates.add(cb.equal(root.get("settleDate"), settleDate));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            query.orderBy(cb.desc(root.get("settleDate")), cb.asc(root.get("obligationId")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec).stream().map(PersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TradeObligation> findOpenBySettleDateAndCurrency(LocalDate settleDate, String currency) {
        return repository.findBySettleDateAndCurrencyIgnoreCaseAndStatus(settleDate, currency, ObligationStatus.OPEN)
                .stream().map(PersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TradeObligation> findByNettingRunId(String runId) {
        return repository.findByNettingRunId(runId).stream()
                .map(PersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
