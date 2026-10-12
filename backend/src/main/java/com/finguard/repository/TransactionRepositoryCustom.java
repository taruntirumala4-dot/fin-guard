package com.finguard.repository;

import com.finguard.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Custom query methods for TransactionRepository that cannot be expressed
 * as simple derived queries (dynamic optional filters + aggregations).
 */
@SuppressWarnings("unused")
public interface TransactionRepositoryCustom {

    /**
     * Search transactions with multiple optional filters.
     * Null filters are ignored (no restriction applied).
     */
    Page<Transaction> searchTransactions(
        String userId,
        Transaction.FraudStatus fraudStatus,
        Transaction.TransactionStatus status,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String location,
        String merchant,
        Instant since,
        Instant until,
        Pageable pageable);

    /** Dashboard: total transaction value (sum of all amounts). */
    BigDecimal sumAllAmounts();
}
