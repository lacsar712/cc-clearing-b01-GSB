package com.clearing.netting.adapter.out.persistence.repo;

import com.clearing.netting.adapter.out.persistence.entity.NetPositionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NetPositionJpaRepository extends JpaRepository<NetPositionJpaEntity, String> {
    List<NetPositionJpaEntity> findByRunId(String runId);
}
