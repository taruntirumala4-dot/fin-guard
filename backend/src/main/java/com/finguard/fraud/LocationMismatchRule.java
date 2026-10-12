package com.finguard.fraud;

import com.finguard.model.Transaction;
import com.finguard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * LocationMismatchRule – detects "impossible travel":
 * a user appears in two geographically distant locations within an
 * unrealistically short time period.
 *
 * Heuristic: if the previous transaction was in a DIFFERENT city and
 * occurred within 30 minutes, flag it.
 *
 * In a production system, this would use actual geo-distance calculations.
 * For this project, a city-name mismatch within a time window is used.
 *
 * Risk score: 30 points
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocationMismatchRule implements FraudRule {

    private final TransactionRepository transactionRepository;

    // If two transactions are in different cities within this window → suspicious
    private static final int LOCATION_WINDOW_MINUTES = 30;

    /** Per-thread reason storage to avoid concurrent bleed between requests. */
    private final ThreadLocal<String> threadReason = new ThreadLocal<>();

    @Override
    public boolean evaluate(Transaction transaction) {
        if (transaction.getUserId() == null || transaction.getLocation() == null) return false;

        Instant currentTime = transaction.getTimestamp() != null
            ? transaction.getTimestamp() : Instant.now();

        Optional<Transaction> previousOpt = transactionRepository
            .findTop1ByUserIdAndTimestampBeforeOrderByTimestampDesc(transaction.getUserId(), currentTime);

        if (previousOpt.isEmpty()) return false;

        Transaction previous = previousOpt.get();
        if (previous.getLocation() == null) return false;

        // Normalize locations for comparison (trim, lowercase)
        String prevCity = previous.getLocation().trim().toLowerCase();
        String currCity = transaction.getLocation().trim().toLowerCase();

        if (prevCity.equals(currCity)) return false;

        Duration timeDiff = Duration.between(previous.getTimestamp(), currentTime);
        long minutesDiff = Math.abs(timeDiff.toMinutes());

        boolean triggered = minutesDiff <= LOCATION_WINDOW_MINUTES;

        if (triggered) {
            threadReason.set(String.format(
                "Impossible travel detected: '%s' at %s → '%s' at %s (%d minutes apart)",
                previous.getLocation(), previous.getTimestamp(),
                transaction.getLocation(), currentTime,
                minutesDiff));
            log.info("LOCATION_MISMATCH rule triggered for user {}: {}",
                transaction.getUserId(), threadReason.get());
        } else {
            threadReason.remove();
        }

        return triggered;
    }

    @Override
    public int getRiskScore() { return 30; }

    @Override
    public String getReason() {
        String r = threadReason.get();
        return r != null ? r : "Transaction location does not match recent transaction location";
    }

    @Override
    public String getRuleName() { return "LOCATION_MISMATCH"; }
}
