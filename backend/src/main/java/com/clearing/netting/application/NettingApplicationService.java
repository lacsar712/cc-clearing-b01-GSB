package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.NetPosition;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.NettingRunStatus;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.MemberRepositoryPort;
import com.clearing.netting.domain.port.out.NetPositionRepositoryPort;
import com.clearing.netting.domain.port.out.NettingRunRepositoryPort;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import com.clearing.netting.domain.service.MultilateralNettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class NettingApplicationService {

    static final String CODE_CONFLICT = "NETTING_RUN_CONFLICT";

    private final NettingRunRepositoryPort runRepository;
    private final ObligationRepositoryPort obligationRepository;
    private final MemberRepositoryPort memberRepository;
    private final NetPositionRepositoryPort positionRepository;
    private final NettingRunStatusService statusService;
    private final MultilateralNettingService nettingService;
    private final TransactionTemplate transactionTemplate;

    /**
     * Per (settleDate, currency) mutex. The lock is acquired <em>outside</em> the database
     * transaction and only released after it has committed, so a late-arriving request can
     * never observe the pre-netting OPEN batch again and double-spend it. Locks are retained
     * (never removed while a thread may be waiting) to avoid the premature-removal race; the
     * key space is bounded by settleDate x currency.
     */
    private final ConcurrentHashMap<RunKey, ReentrantLock> runLocks = new ConcurrentHashMap<>();

    public NettingApplicationService(
            NettingRunRepositoryPort runRepository,
            ObligationRepositoryPort obligationRepository,
            MemberRepositoryPort memberRepository,
            NetPositionRepositoryPort positionRepository,
            NettingRunStatusService statusService,
            org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.runRepository = runRepository;
        this.obligationRepository = obligationRepository;
        this.memberRepository = memberRepository;
        this.positionRepository = positionRepository;
        this.statusService = statusService;
        this.nettingService = new MultilateralNettingService();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<NettingRun> listRuns() {
        return runRepository.findAllOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public NettingRun getRun(String runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new DomainException("RUN_NOT_FOUND", "netting run not found: " + runId));
    }

    @Transactional(readOnly = true)
    public List<NetPosition> getPositions(String runId) {
        getRun(runId);
        return positionRepository.findByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<TradeObligation> getRunObligations(String runId) {
        getRun(runId);
        return obligationRepository.findByNettingRunId(runId);
    }

    /**
     * Executes netting for one settleDate/currency with an idempotency/concurrency guard.
     * A second request that arrives while one is in progress, or after one has already
     * succeeded for the same key, is rejected with {@link #CODE_CONFLICT} instead of
     * re-netting the same OPEN obligations.
     */
    public NettingRunResult execute(LocalDate settleDate, String currency) {
        if (settleDate == null) {
            throw new DomainException("INVALID_DATE", "settleDate is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new DomainException("INVALID_CURRENCY", "currency is required");
        }
        String ccy = currency.trim().toUpperCase();

        RunKey key = new RunKey(settleDate, ccy);
        ReentrantLock lock = runLocks.computeIfAbsent(key, k -> new ReentrantLock());
        // tryLock: a run for this exact batch is already in progress -> fail fast, never queue.
        if (!lock.tryLock()) {
            throw new DomainException(CODE_CONFLICT,
                    "该交割日/币种的轧差正在执行中,请勿重复提交(稍后刷新查看结果)");
        }
        try {
            // Runs inside the lock so the critical section covers commit, not just the method body.
            return transactionTemplate.execute(status -> doExecute(settleDate, ccy));
        } finally {
            lock.unlock();
        }
    }

    private NettingRunResult doExecute(LocalDate settleDate, String ccy) {
        // Idempotency: a previous successful run already consumed the OPEN batch.
        NettingRun existing = runRepository.findCompletedBySettleDateAndCurrency(settleDate, ccy)
                .orElse(null);
        if (existing != null) {
            throw new DomainException(CODE_CONFLICT,
                    "该交割日/币种已存在成功轧差批次 " + existing.getRunId()
                            + ",OPEN 义务已完成净额,请勿重复执行");
        }

        NettingRun run = NettingRun.create(settleDate, ccy);
        run.markRunning();
        run = statusService.saveInNewTx(run);

        try {
            List<TradeObligation> opens = obligationRepository.findOpenBySettleDateAndCurrency(settleDate, ccy);
            Set<String> memberIds = new HashSet<>();
            for (TradeObligation o : opens) {
                memberIds.add(o.getPayerMemberId());
                memberIds.add(o.getPayeeMemberId());
            }
            Map<String, Member> members = new HashMap<>();
            for (Member m : memberRepository.findByIds(memberIds)) {
                members.put(m.getMemberId(), m);
            }

            List<NetPosition> positions = nettingService.net(run.getRunId(), ccy, opens, members);

            for (TradeObligation o : opens) {
                o.markNetted(run.getRunId());
            }
            obligationRepository.saveAll(opens);
            positionRepository.saveAll(positions);

            run.markCompleted();
            run = runRepository.save(run);
            return new NettingRunResult(run, positions, opens);
        } catch (DomainException ex) {
            run.markFailed(ex.getMessage());
            statusService.saveInNewTx(run);
            throw ex;
        } catch (RuntimeException ex) {
            run.markFailed(ex.getMessage() == null ? "unexpected error" : ex.getMessage());
            statusService.saveInNewTx(run);
            throw new DomainException("NETTING_FAILED", ex.getMessage());
        }
    }

    @Transactional
    public NettingRun settle(String runId) {
        NettingRun run = getRun(runId);
        if (run.getStatus() != NettingRunStatus.COMPLETED) {
            throw new DomainException("INVALID_STATE", "only COMPLETED runs can be settled");
        }
        List<TradeObligation> obligations = obligationRepository.findByNettingRunId(runId);
        if (obligations.isEmpty()) {
            throw new DomainException("NO_OBLIGATIONS", "no obligations linked to run");
        }
        for (TradeObligation o : obligations) {
            if (o.getStatus() == ObligationStatus.NETTED) {
                o.markSettled();
            } else if (o.getStatus() != ObligationStatus.SETTLED) {
                throw new DomainException("INVALID_STATE", "obligation not NETTED: " + o.getObligationId());
            }
        }
        obligationRepository.saveAll(obligations);
        return run;
    }

    public record NettingRunResult(NettingRun run, List<NetPosition> positions, List<TradeObligation> obligations) {
    }

    private record RunKey(LocalDate settleDate, String currency) {
    }
}
