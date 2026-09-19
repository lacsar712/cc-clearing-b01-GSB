package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.MemberStatus;
import com.clearing.netting.domain.model.NettingRunStatus;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.MemberRepositoryPort;
import com.clearing.netting.domain.port.out.NetPositionRepositoryPort;
import com.clearing.netting.domain.port.out.NettingRunRepositoryPort;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the full Spring context against H2: proves PlatformTransactionManager injection,
 * the derived-query name, and end-to-end idempotency on the real persistence layer.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:netting-it;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class NettingConcurrencyIntegrationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 18);

    @Autowired
    private NettingApplicationService service;
    @Autowired
    private ObligationRepositoryPort obligations;
    @Autowired
    private MemberRepositoryPort members;
    @Autowired
    private NetPositionRepositoryPort positions;
    @Autowired
    private NettingRunRepositoryPort runs;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void secondExecuteForSameDayAndCurrencyConflictsAndDoesNotDoubleNet() {
        members.save(new Member("M1", "Bank 1", MemberStatus.ACTIVE));
        members.save(new Member("M2", "Bank 2", MemberStatus.ACTIVE));
        obligations.save(TradeObligation.open("M1", "M2", "USD", new BigDecimal("100"), DATE.minusDays(1), DATE));
        obligations.save(TradeObligation.open("M2", "M1", "USD", new BigDecimal("40"), DATE.minusDays(1), DATE));

        // First execution succeeds and consumes the OPEN batch.
        NettingApplicationService.NettingRunResult first = service.execute(DATE, "USD");
        assertEquals(NettingRunStatus.COMPLETED, first.run().getStatus());

        // Second execution (double-click / re-post) is rejected as a conflict.
        DomainException ex = assertThrows(DomainException.class, () -> service.execute(DATE, "USD"));
        assertEquals("NETTING_RUN_CONFLICT", ex.getCode());

        // The obligations were netted exactly once, still linked only to the first run.
        List<TradeObligation> stored = transactionTemplate.execute(s -> obligations.findAll());
        assertEquals(2, stored.size());
        assertTrue(stored.stream().allMatch(o -> o.getStatus() == ObligationStatus.NETTED));
        assertTrue(stored.stream().allMatch(o -> o.getNettingRunId().equals(first.run().getRunId())));

        // Exactly one COMPLETED run and one set of positions: no duplicate netting.
        long completedRuns = runs.findAllOrderByCreatedAtDesc().stream()
                .filter(r -> r.getStatus() == NettingRunStatus.COMPLETED)
                .count();
        assertEquals(1, completedRuns);
        assertEquals(2, positions.findByRunId(first.run().getRunId()).size());
    }
}
