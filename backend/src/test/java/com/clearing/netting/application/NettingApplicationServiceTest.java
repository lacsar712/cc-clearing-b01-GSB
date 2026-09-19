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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NettingApplicationServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 18);

    private NettingRunRepositoryPort runRepo;
    private ObligationRepositoryPort obRepo;
    private MemberRepositoryPort memberRepo;
    private NetPositionRepositoryPort posRepo;
    private PlatformTransactionManager txManager;
    private NettingApplicationService service;

    private Member a;
    private Member b;

    @BeforeEach
    void setUp() {
        runRepo = mock(NettingRunRepositoryPort.class);
        obRepo = mock(ObligationRepositoryPort.class);
        memberRepo = mock(MemberRepositoryPort.class);
        posRepo = mock(NetPositionRepositoryPort.class);
        txManager = mock(PlatformTransactionManager.class);

        // Synchronous, no-op transaction so the lock-outside-tx logic runs unchanged in unit tests.
        lenient().when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        lenient().when(runRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new NettingApplicationService(
                runRepo, obRepo, memberRepo, posRepo,
                new NettingRunStatusService(runRepo), txManager);

        a = new Member("A", "Bank A", MemberStatus.ACTIVE);
        b = new Member("B", "Bank B", MemberStatus.ACTIVE);
    }

    @Test
    void rejectsConcurrentInProgressRunAndNetsObligationsOnlyOnce() throws InterruptedException {
        when(memberRepo.findByIds(any())).thenReturn(List.of(a, b));

        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(obRepo.findOpenBySettleDateAndCurrency(any(), any())).thenAnswer(inv -> {
            entered.countDown();
            release.await(5, TimeUnit.SECONDS);
            return List.of(open("A", "B", "100"), open("B", "A", "40"));
        });

        AtomicReference<Exception> firstError = new AtomicReference<>();
        Thread first = new Thread(() -> {
            try {
                service.execute(DATE, "USD");
            } catch (Exception e) {
                firstError.set(e);
            }
        });
        first.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS), "first run should enter the critical section");

        // Late-arriving second request while the first is still running: conflict, no queuing.
        DomainException ex = assertThrows(DomainException.class, () -> service.execute(DATE, "USD"));
        assertEquals("NETTING_RUN_CONFLICT", ex.getCode());

        release.countDown();
        first.join(5_000);
        assertNull(firstError.get(), "winning run should complete");

        // The OPEN batch was consumed exactly once; the losing request never touched obligations.
        verify(obRepo, times(1)).saveAll(anyList());
        verify(posRepo, times(1)).saveAll(anyList());
    }

    @Test
    void rejectsWhenSuccessfulRunAlreadyExistsForSameDayAndCurrency() {
        NettingRun done = new NettingRun(
                "run-done", DATE, "USD", NettingRunStatus.COMPLETED, Instant.now(), null);
        when(runRepo.findCompletedBySettleDateAndCurrency(DATE, "USD")).thenReturn(Optional.of(done));

        DomainException ex = assertThrows(DomainException.class, () -> service.execute(DATE, "USD"));
        assertEquals("NETTING_RUN_CONFLICT", ex.getCode());
        assertTrue(ex.getMessage().contains("run-done"));

        // No new run row, no obligation read or mutation: the already-netted batch is untouched.
        verify(runRepo, never()).save(any());
        verify(obRepo, never()).findOpenBySettleDateAndCurrency(any(), any());
        verify(obRepo, never()).saveAll(anyList());
    }

    @Test
    void successfulRunNetsObligationsExactlyOnce() {
        when(memberRepo.findByIds(any())).thenReturn(List.of(a, b));
        when(obRepo.findOpenBySettleDateAndCurrency(DATE, "USD"))
                .thenReturn(List.of(open("A", "B", "100"), open("B", "A", "40")));

        NettingApplicationService.NettingRunResult result = service.execute(DATE, "USD");

        assertEquals(NettingRunStatus.COMPLETED, result.run().getStatus());
        BigDecimal sum = result.positions().stream()
                .map(NetPosition::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(BigDecimal.ZERO));
        for (TradeObligation o : result.obligations()) {
            assertEquals(ObligationStatus.NETTED, o.getStatus());
            assertEquals(result.run().getRunId(), o.getNettingRunId());
        }
        verify(obRepo, times(1)).saveAll(anyList());
        verify(posRepo, times(1)).saveAll(anyList());
    }

    @Test
    void failedRunReleasesLockAndAllowsRetryThatNetsOnce() {
        when(memberRepo.findByIds(any())).thenReturn(List.of(a, b));
        // First attempt finds no OPEN obligations -> NO_OBLIGATIONS (FAILED); retry finds the batch.
        when(obRepo.findOpenBySettleDateAndCurrency(any(), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(open("A", "B", "100"), open("B", "A", "40")));

        DomainException first = assertThrows(DomainException.class, () -> service.execute(DATE, "USD"));
        assertEquals("NO_OBLIGATIONS", first.getCode());

        // Lock released and FAILED does not occupy the key -> retry succeeds.
        NettingApplicationService.NettingRunResult retry = service.execute(DATE, "USD");
        assertEquals(NettingRunStatus.COMPLETED, retry.run().getStatus());

        // Netting happened only on the successful retry.
        verify(obRepo, times(1)).saveAll(anyList());
    }

    private TradeObligation open(String payer, String payee, String amount) {
        return TradeObligation.open(
                payer, payee, "USD", new BigDecimal(amount), DATE.minusDays(1), DATE);
    }
}
