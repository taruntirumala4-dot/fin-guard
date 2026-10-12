package com.finguard.fraud;

import com.finguard.model.Transaction;
import com.finguard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DeviceChangeRule – detects transactions from a new or unrecognized device.
 *
 * Algorithm:
 *   Retrieve all distinct device IDs previously used by this user (a Set-like lookup).
 *   If the current device is NOT in that set → flag it.
 *
 * DSA:
 *   - Distinct device lookup: O(n) where n = user's historical transactions
 *   - HashSet membership check: O(1) average
 *
 * Risk score: 15 points (lower weight since device changes can be legitimate)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceChangeRule implements FraudRule {

    private final TransactionRepository transactionRepository;

    @Value("${finguard.fraud.device-change-score:15}")
    private int riskScore;

    /** Per-thread reason storage to avoid concurrent bleed between requests. */
    private final ThreadLocal<String> threadReason = new ThreadLocal<>();

    @Override
    public boolean evaluate(Transaction transaction) {
        if (transaction.getUserId() == null || transaction.getDeviceId() == null) return false;

        // Get all devices this user has previously used
        List<String> knownDevices = transactionRepository
            .findDistinctDeviceIdsByUserId(transaction.getUserId());

        if (knownDevices.isEmpty()) {
            // First transaction – no history to compare
            return false;
        }

        // Use a HashSet for O(1) average lookup
        java.util.Set<String> knownDeviceSet = new java.util.HashSet<>(knownDevices);

        boolean isNewDevice = !knownDeviceSet.contains(transaction.getDeviceId());

        if (isNewDevice) {
            threadReason.set(String.format(
                "Transaction from unrecognized device '%s'. User's known devices: %s",
                transaction.getDeviceId(), knownDeviceSet));
            log.info("DEVICE_CHANGE rule triggered for user {}: deviceId={}",
                transaction.getUserId(), transaction.getDeviceId());
        } else {
            threadReason.remove();
        }

        return isNewDevice;
    }

    @Override
    public int getRiskScore() { return riskScore; }

    @Override
    public String getReason() {
        String r = threadReason.get();
        return r != null ? r : "Transaction from previously unrecognized device";
    }

    @Override
    public String getRuleName() { return "DEVICE_CHANGE"; }
}
