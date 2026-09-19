package com.clearing.netting.application;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-process mutual-exclusion guard keyed by (settleDate, currency).
 * Serializes concurrent netting executions for the same key so that a
 * double-submit fails fast with a conflict instead of double-netting the
 * same OPEN obligations. Complements the durable occupancy check on
 * persisted runs (RUNNING/COMPLETED) inside NettingApplicationService.
 */
@Component
public class NettingExecutionGuard {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public boolean tryAcquire(LocalDate settleDate, String currency) {
        return locks.computeIfAbsent(key(settleDate, currency), k -> new ReentrantLock()).tryLock();
    }

    public void release(LocalDate settleDate, String currency) {
        ReentrantLock lock = locks.get(key(settleDate, currency));
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private String key(LocalDate settleDate, String currency) {
        return settleDate + "|" + currency.trim().toUpperCase();
    }
}
