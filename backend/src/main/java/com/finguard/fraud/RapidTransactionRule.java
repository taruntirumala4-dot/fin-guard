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
 * RapidTransactionRule – detects multiple transactions within a short time window.
 *
 * Algorithm:
 *   Count user's transactions in the past N minutes.
 *   If count > threshold → fraud signal.
 *
 * Time complexity: O(n) with index scan on (user_id, timestamp)
 * Space complexity: O(1) – only count is returned, not all transactions
 *
 * Default: more than 5 transactions within 2 minutes → risk score 25
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RapidTransactionRule implements FraudRule {

    private final TransactionRepository transactionRepository;

    @Value("${finguard.fraud.rapid-transaction-window-minutes:2}")
    private int windowMinutes;

    @Value("${finguard.fraud.rapid-transaction-count:5}")
    private int rapidTransactionCount;

    @Value("${finguard.fraud.rapid-transaction-score:25}")
    private int riskScore;

    /** Per-thread reason storage to avoid concurrent bleed between requests. */
    private final ThreadLocal<String> threadReason = new ThreadLocal<>();

    @Override
    public boolean evaluate(Transaction transaction) {
        if (transaction.getUserId() == null) return false;

        Instant windowStart = (transaction.getTimestamp() != null
            ? transaction.getTimestamp()
            : Instant.now()).minus(windowMinutes, ChronoUnit.MINUTES);

        long count = transactionRepository.countByUserIdAndTimestampAfter(
            transaction.getUserId(), windowStart);

        // Include the current transaction in the count
        long effectiveCount = count + 1;

        boolean triggered = effectiveCount > rapidTransactionCount;

        if (triggered) {
            threadReason.set(String.format(
                "User made %d transactions within %d minute(s) (threshold: %d)",
                effectiveCount, windowMinutes, rapidTransactionCount));
            log.info("RAPID_TRANSACTION rule triggered for user {}: {}",
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
        return r != null ? r : "Multiple rapid transactions detected in short time window";
    }

    @Override
    public String getRuleName() { return "RAPID_TRANSACTION"; }
}
