package com.clearing.netting.domain.port.out;

import com.clearing.netting.domain.model.NetPosition;

import java.util.List;

public interface NetPositionRepositoryPort {
    List<NetPosition> saveAll(List<NetPosition> positions);

    List<NetPosition> findByRunId(String runId);
}
