package com.clearing.netting.application;

import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.port.out.NettingRunRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NettingRunStatusService {

    private final NettingRunRepositoryPort runRepository;

    public NettingRunStatusService(NettingRunRepositoryPort runRepository) {
        this.runRepository = runRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NettingRun saveInNewTx(NettingRun run) {
        return runRepository.save(run);
    }
}
