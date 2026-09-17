package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.repo.NetPositionJpaRepository;
import com.clearing.netting.domain.model.NetPosition;
import com.clearing.netting.domain.port.out.NetPositionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NetPositionRepositoryAdapter implements NetPositionRepositoryPort {

    private final NetPositionJpaRepository repository;

    public NetPositionRepositoryAdapter(NetPositionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<NetPosition> saveAll(List<NetPosition> positions) {
        return repository.saveAll(positions.stream().map(PersistenceMapper::toEntity).collect(Collectors.toList()))
                .stream().map(PersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<NetPosition> findByRunId(String runId) {
        return repository.findByRunId(runId).stream()
                .map(PersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
