package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.repo.NettingRunJpaRepository;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.NettingRunStatus;
import com.clearing.netting.domain.port.out.NettingRunRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class NettingRunRepositoryAdapter implements NettingRunRepositoryPort {

    private final NettingRunJpaRepository repository;

    public NettingRunRepositoryAdapter(NettingRunJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public NettingRun save(NettingRun run) {
        return PersistenceMapper.toDomain(repository.save(PersistenceMapper.toEntity(run)));
    }

    @Override
    public Optional<NettingRun> findById(String runId) {
        return repository.findById(runId).map(PersistenceMapper::toDomain);
    }

    @Override
    public List<NettingRun> findAllOrderByCreatedAtDesc() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(PersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<NettingRun> findLatestBySettleDateAndCurrencyAndStatusIn(
            LocalDate settleDate, String currency, Collection<NettingRunStatus> statuses) {
        return repository
                .findFirstBySettleDateAndCurrencyAndStatusInOrderByCreatedAtDesc(settleDate, currency, statuses)
                .map(PersistenceMapper::toDomain);
    }
}
