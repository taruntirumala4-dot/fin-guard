package com.finguard.fraud;

import com.finguard.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * HighAmountRule – flags transactions that exceed a configurable threshold.
 *
 * Default threshold: ₹100,000 INR
 * Risk score: 30 points
 *
 * Configurable via:
 *   finguard.fraud.high-amount-threshold (application.yml)
 */
@Component
@Slf4j
public class HighAmountRule implements FraudRule {

    @Value("${finguard.fraud.high-amount-threshold:100000}")
    private double highAmountThreshold;

    @Value("${finguard.fraud.high-amount-score:30}")
    private int riskScore;

    /**
     * ThreadLocal stores the reason per-thread to avoid race conditions
     * when multiple requests evaluate the same singleton rule bean concurrently.
     */
    private final ThreadLocal<String> threadReason = new ThreadLocal<>();

    @Override
    public boolean evaluate(Transaction transaction) {
        if (transaction.getAmount() == null) return false;

        boolean triggered = transaction.getAmount()
            .compareTo(BigDecimal.valueOf(highAmountThreshold)) > 0;

        if (triggered) {
            threadReason.set(String.format(
                "Transaction amount ₹%,.0f exceeds threshold ₹%,.0f",
                transaction.getAmount().doubleValue(), highAmountThreshold));
            log.info("HIGH_AMOUNT rule triggered for transaction {}: {}",
                transaction.getTransactionId(), threadReason.get());
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
        return r != null ? r : "Transaction amount exceeds configured threshold";
    }

    @Override
    public String getRuleName() { return "HIGH_AMOUNT"; }
}
