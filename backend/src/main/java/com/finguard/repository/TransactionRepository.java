package com.finguard.repository;

import com.finguard.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * TransactionRepository – JPA repository for Transaction entity in SQL database.
 */
@Repository
public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, TransactionRepositoryCustom, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Page<Transaction> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    Page<Transaction> findAllByOrderByTimestampDesc(Pageable pageable);

    /** Fraud dashboard: recent flagged transactions */
    Page<Transaction> findByFraudStatusOrderByTimestampDesc(
        Transaction.FraudStatus fraudStatus, Pageable pageable);

    /** Count transactions per fraud status for dashboard stats */
    long countByFraudStatus(Transaction.FraudStatus fraudStatus);

    /** Count by transaction status */
    long countByStatus(Transaction.TransactionStatus status);

    /** Velocity / rapid-transaction detection: count user transactions in time window */
    long countByUserIdAndTimestampAfter(String userId, Instant since);

    /**
     * Location mismatch detection: get the most recent transaction
     * for a user before the current one to compare locations.
     */
    Optional<Transaction> findTop1ByUserIdAndTimestampBeforeOrderByTimestampDesc(
        String userId, Instant before);

    /** Recent transactions for dashboard (last N) */
    List<Transaction> findTop10ByOrderByTimestampDesc();

    /** Device-change detection: get distinct devices used by this user */
    @Query("SELECT DISTINCT t.deviceId FROM Transaction t WHERE t.userId = :userId")
    List<String> findDistinctDeviceIdsByUserId(@Param("userId") String userId);
}
