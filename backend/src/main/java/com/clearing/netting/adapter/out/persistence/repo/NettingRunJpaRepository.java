package com.clearing.netting.adapter.out.persistence.repo;

import com.clearing.netting.adapter.out.persistence.entity.NettingRunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NettingRunJpaRepository extends JpaRepository<NettingRunJpaEntity, String> {
    List<NettingRunJpaEntity> findAllByOrderByCreatedAtDesc();
}
