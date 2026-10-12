package com.finguard.fraud;

import com.finguard.model.Transaction;
import com.finguard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * VelocityRule – detects unusually high transaction frequency over a 1-hour window.
 *
 * This is different from RapidTransactionRule (which uses a 2-minute window).
 * VelocityRule catches sustained high-volume activity over a longer period.
 *
 * Default: more than 10 transactions per hour → risk score 20
 *
 * DSA: Uses DB index on (user_id, timestamp) for efficient range count.
 * Time complexity: O(log n) with composite B-tree index.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VelocityRule implements FraudRule {

    private final TransactionRepository transactionRepository;

    @Value("${finguard.fraud.velocity-threshold-per-hour:10}")
    private int velocityThreshold;

    @Value("${finguard.fraud.velocity-score:20}")
    private int riskScore;

    /** Per-thread reason storage to avoid concurrent bleed between requests. */
    private final ThreadLocal<String> threadReason = new ThreadLocal<>();

    @Override
    public boolean evaluate(Transaction transaction) {
        if (transaction.getUserId() == null) return false;

        Instant oneHourAgo = (transaction.getTimestamp() != null
            ? transaction.getTimestamp()
            : Instant.now()).minus(1, ChronoUnit.HOURS);

        long count = transactionRepository.countByUserIdAndTimestampAfter(
            transaction.getUserId(), oneHourAgo);

        boolean triggered = count >= velocityThreshold;

        if (triggered) {
            threadReason.set(String.format(
                "High transaction velocity: %d transactions in the past hour (threshold: %d)",
                count, velocityThreshold));
            log.info("VELOCITY rule triggered for user {}: {}",
                transaction.getUserId(), threadReason.get());
        } else {
            threadReason.remove();
        }

        return triggered;
    }

    @Override
    public int getRiskScore() { return riskScore; }

    @Override
    public String getReason() {
        String r = threadReason.get();
        return r != null ? r : "Unusually high transaction frequency detected";
    }

    @Override
    public String getRuleName() { return "VELOCITY"; }
}
