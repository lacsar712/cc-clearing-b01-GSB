package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.MemberStatus;
import com.clearing.netting.domain.model.NetPosition;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.NettingRunStatus;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.MemberRepositoryPort;
import com.clearing.netting.domain.port.out.NetPositionRepositoryPort;
import com.clearing.netting.domain.port.out.NettingRunRepositoryPort;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NettingApplicationServiceTest {

    private static final LocalDate SETTLE_DATE = LocalDate.of(2026, 9, 18);

    private StubRunRepository runs;
    private StubObligationRepository obligations;
    private StubMemberRepository members;
    private StubPositionRepository positions;
    private NettingExecutionGuard guard;
    private NettingApplicationService service;

    @BeforeEach
    void setUp() {
        runs = new StubRunRepository();
        obligations = new StubObligationRepository();
        members = new StubMemberRepository();
        positions = new StubPositionRepository();
        guard = new NettingExecutionGuard();
        service = new NettingApplicationService(
                runs, obligations, members, positions, new NettingRunStatusService(runs), guard);

        members.add(new Member("A", "Bank A", MemberStatus.ACTIVE));
        members.add(new Member("B", "Bank B", MemberStatus.ACTIVE));
    }

    @Test
    void secondExecuteAfterCompletedConflictsAndObligationsNotRenetted() {
        seedUsdObligations();

        NettingApplicationService.NettingRunResult first = service.execute(SETTLE_DATE, "USD");
        assertEquals(NettingRunStatus.COMPLETED, first.run().getStatus());
        String firstRunId = first.run().getRunId();
        assertEquals(2, positions.saved.size());

        DomainException ex = assertThrows(DomainException.class, () -> service.execute(SETTLE_DATE, "USD"));
        assertEquals("NETTING_CONFLICT", ex.getCode());

        // obligations were netted exactly once and remain bound to the first run
        for (TradeObligation o : obligations.findAll()) {
            assertEquals(ObligationStatus.NETTED, o.getStatus());
            assertEquals(firstRunId, o.getNettingRunId());
        }
        // no duplicate net positions were produced
        assertEquals(2, positions.saved.size());
        assertEquals(1, runs.findAllOrderByCreatedAtDesc().stream()
                .filter(r -> r.getStatus() == NettingRunStatus.COMPLETED)
                .count());
    }

    @Test
    void executeConflictsWhileAnotherRunInProgress() {
        runs.save(new NettingRun("run-running", SETTLE_DATE, "USD",
                NettingRunStatus.RUNNING, Instant.now(), null));
        seedUsdObligations();

        DomainException ex = assertThrows(DomainException.class, () -> service.execute(SETTLE_DATE, "USD"));
        assertEquals("NETTING_CONFLICT", ex.getCode());

        // obligations untouched: still OPEN, no positions produced
        assertTrue(obligations.findAll().stream().allMatch(o -> o.getStatus() == ObligationStatus.OPEN));
        assertTrue(positions.saved.isEmpty());
    }

    @Test
    void executeAllowedAfterFailedRun() {
        runs.save(new NettingRun("run-failed", SETTLE_DATE, "USD",
                NettingRunStatus.FAILED, Instant.now(), "boom"));
        seedUsdObligations();

        NettingApplicationService.NettingRunResult result = service.execute(SETTLE_DATE, "USD");
        assertEquals(NettingRunStatus.COMPLETED, result.run().getStatus());
        assertEquals(2, positions.saved.size());
    }

    @Test
    void concurrentExecutionFailsFastWithConflict() throws Exception {
        seedUsdObligations();
        // simulate an in-flight execution holding the (settleDate, currency) key
        assertTrue(guard.tryAcquire(SETTLE_DATE, "USD"));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<DomainException> future = executor.submit(() -> {
                try {
                    service.execute(SETTLE_DATE, "USD");
                    return null;
                } catch (DomainException ex) {
                    return ex;
                }
            });
            DomainException ex = future.get(5, TimeUnit.SECONDS);
            assertNotNull(ex, "concurrent execute must be rejected");
            assertEquals("NETTING_CONFLICT", ex.getCode());
        } finally {
            executor.shutdownNow();
            guard.release(SETTLE_DATE, "USD");
        }

        // the rejected attempt must not have netted anything
        assertTrue(obligations.findAll().stream().allMatch(o -> o.getStatus() == ObligationStatus.OPEN));
        assertTrue(positions.saved.isEmpty());
    }

    @Test
    void differentCurrencyDoesNotConflict() {
        seedUsdObligations();
        obligations.add(TradeObligation.open("A", "B", "EUR",
                new BigDecimal("70"), SETTLE_DATE, SETTLE_DATE));
        obligations.add(TradeObligation.open("B", "A", "EUR",
                new BigDecimal("30"), SETTLE_DATE, SETTLE_DATE));

        assertEquals(NettingRunStatus.COMPLETED, service.execute(SETTLE_DATE, "USD").run().getStatus());
        assertEquals(NettingRunStatus.COMPLETED, service.execute(SETTLE_DATE, "EUR").run().getStatus());
    }

    private void seedUsdObligations() {
        obligations.add(TradeObligation.open("A", "B", "USD",
                new BigDecimal("100"), SETTLE_DATE, SETTLE_DATE));
        obligations.add(TradeObligation.open("B", "A", "USD",
                new BigDecimal("40"), SETTLE_DATE, SETTLE_DATE));
    }

    static class StubRunRepository implements NettingRunRepositoryPort {
        private final Map<String, NettingRun> store = new LinkedHashMap<>();

        @Override
        public synchronized NettingRun save(NettingRun run) {
            store.put(run.getRunId(), copy(run));
            return copy(run);
        }

        @Override
        public synchronized Optional<NettingRun> findById(String runId) {
            return Optional.ofNullable(store.get(runId)).map(StubRunRepository::copy);
        }

        @Override
        public synchronized List<NettingRun> findAllOrderByCreatedAtDesc() {
            return store.values().stream()
                    .sorted(Comparator.comparing(NettingRun::getCreatedAt).reversed())
                    .map(StubRunRepository::copy)
                    .collect(Collectors.toList());
        }

        @Override
        public synchronized Optional<NettingRun> findLatestBySettleDateAndCurrencyAndStatusIn(
                LocalDate settleDate, String currency, Collection<NettingRunStatus> statuses) {
            return store.values().stream()
                    .filter(r -> r.getSettleDate().equals(settleDate))
                    .filter(r -> r.getCurrency().equalsIgnoreCase(currency))
                    .filter(r -> statuses.contains(r.getStatus()))
                    .max(Comparator.comparing(NettingRun::getCreatedAt))
                    .map(StubRunRepository::copy);
        }

        private static NettingRun copy(NettingRun r) {
            return new NettingRun(r.getRunId(), r.getSettleDate(), r.getCurrency(),
                    r.getStatus(), r.getCreatedAt(), r.getFailureReason());
        }
    }

    static class StubObligationRepository implements ObligationRepositoryPort {
        private final Map<String, TradeObligation> store = new LinkedHashMap<>();

        void add(TradeObligation o) {
            store.put(o.getObligationId(), o);
        }

        @Override
        public TradeObligation save(TradeObligation obligation) {
            store.put(obligation.getObligationId(), obligation);
            return obligation;
        }

        @Override
        public List<TradeObligation> saveAll(List<TradeObligation> obligations) {
            obligations.forEach(o -> store.put(o.getObligationId(), o));
            return obligations;
        }

        @Override
        public Optional<TradeObligation> findById(String obligationId) {
            return Optional.ofNullable(store.get(obligationId));
        }

        @Override
        public List<TradeObligation> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<TradeObligation> findByFilters(String currency, LocalDate settleDate, ObligationStatus status) {
            return store.values().stream()
                    .filter(o -> currency == null || o.getCurrency().equalsIgnoreCase(currency))
                    .filter(o -> settleDate == null || o.getSettleDate().equals(settleDate))
                    .filter(o -> status == null || o.getStatus() == status)
                    .collect(Collectors.toList());
        }

        @Override
        public List<TradeObligation> findOpenBySettleDateAndCurrency(LocalDate settleDate, String currency) {
            return store.values().stream()
                    .filter(o -> o.getStatus() == ObligationStatus.OPEN)
                    .filter(o -> o.getSettleDate().equals(settleDate))
                    .filter(o -> o.getCurrency().equalsIgnoreCase(currency))
                    .collect(Collectors.toList());
        }

        @Override
        public List<TradeObligation> findByNettingRunId(String runId) {
            return store.values().stream()
                    .filter(o -> runId.equals(o.getNettingRunId()))
                    .collect(Collectors.toList());
        }
    }

    static class StubMemberRepository implements MemberRepositoryPort {
        private final Map<String, Member> store = new HashMap<>();

        void add(Member m) {
            store.put(m.getMemberId(), m);
        }

        @Override
        public Member save(Member member) {
            store.put(member.getMemberId(), member);
            return member;
        }

        @Override
        public Optional<Member> findById(String memberId) {
            return Optional.ofNullable(store.get(memberId));
        }

        @Override
        public List<Member> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<Member> findByIds(Iterable<String> memberIds) {
            List<Member> out = new ArrayList<>();
            for (String id : memberIds) {
                Member m = store.get(id);
                if (m != null) {
                    out.add(m);
                }
            }
            return out;
        }
    }

    static class StubPositionRepository implements NetPositionRepositoryPort {
        final List<NetPosition> saved = new ArrayList<>();

        @Override
        public List<NetPosition> saveAll(List<NetPosition> positions) {
            saved.addAll(positions);
            return positions;
        }

        @Override
        public List<NetPosition> findByRunId(String runId) {
            return saved.stream()
                    .filter(p -> p.getRunId().equals(runId))
                    .collect(Collectors.toList());
        }
    }
}
