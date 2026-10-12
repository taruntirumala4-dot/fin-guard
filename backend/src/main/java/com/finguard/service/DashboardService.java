package com.finguard.service;

import com.finguard.dto.TransactionResponse;
import com.finguard.model.Transaction;
import com.finguard.redis.RedisKeyGenerator;
import com.finguard.redis.RedisService;
import com.finguard.repository.FraudAlertRepository;
import com.finguard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardService – aggregates statistics for the dashboard.
 *
 * Uses Redis to cache expensive aggregate queries.
 * Cache TTL: 5 minutes (configurable).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final RedisService redisService;

    @Value("${finguard.redis.ttl.dashboard-stats-seconds:300}")
    private long dashboardStatsTtlSeconds;

    // ─── Summary ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSummary() {
        String cacheKey = RedisKeyGenerator.dashboardStatsKey();

        // Try cache
        java.util.Optional<Object> cached = redisService.get(cacheKey, Object.class);
        if (cached.isPresent()) {
            log.debug("Dashboard summary served from Redis cache");
            return (Map<String, Object>) cached.get();
        }

        // Build fresh
        long totalTransactions   = transactionRepository.count();
        BigDecimal totalValue    = transactionRepository.sumAllAmounts();
        long safeTransactions    = transactionRepository.countByFraudStatus(Transaction.FraudStatus.SAFE);
        long suspiciousCount     = transactionRepository.countByFraudStatus(Transaction.FraudStatus.SUSPICIOUS);
        long fraudCount          = transactionRepository.countByFraudStatus(Transaction.FraudStatus.FRAUD);
        long totalAlerts         = fraudAlertRepository.count();

        double fraudRate = totalTransactions > 0
            ? ((double)(suspiciousCount + fraudCount) / totalTransactions) * 100
            : 0.0;

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTransactions",  totalTransactions);
        summary.put("totalValue",         totalValue != null ? totalValue : BigDecimal.ZERO);
        summary.put("safeTransactions",   safeTransactions);
        summary.put("suspiciousCount",    suspiciousCount);
        summary.put("fraudCount",         fraudCount);
        summary.put("totalAlerts",        totalAlerts);
        summary.put("fraudRate",          Math.round(fraudRate * 100.0) / 100.0);

        redisService.set(cacheKey, summary, Duration.ofSeconds(dashboardStatsTtlSeconds));

        return summary;
    }

    // ─── Transaction stats ────────────────────────────────────

    public Map<String, Object> getTransactionStats() {
        List<TransactionResponse> recent = transactionRepository.findTop10ByOrderByTimestampDesc()
            .stream()
            .map(TransactionResponse::from)
            .toList();

        Map<String, Object> stats = new HashMap<>();
        stats.put("recentTransactions", recent);
        stats.put("byStatus", Map.of(
            "SUCCESS", transactionRepository.countByStatus(Transaction.TransactionStatus.SUCCESS),
            "PENDING", transactionRepository.countByStatus(Transaction.TransactionStatus.PENDING),
            "BLOCKED", transactionRepository.countByStatus(Transaction.TransactionStatus.BLOCKED),
            "FAILED",  transactionRepository.countByStatus(Transaction.TransactionStatus.FAILED)
        ));
        stats.put("byFraudStatus", Map.of(
            "SAFE",       transactionRepository.countByFraudStatus(Transaction.FraudStatus.SAFE),
            "SUSPICIOUS", transactionRepository.countByFraudStatus(Transaction.FraudStatus.SUSPICIOUS),
            "FRAUD",      transactionRepository.countByFraudStatus(Transaction.FraudStatus.FRAUD)
        ));

        return stats;
    }

    // ─── Fraud stats ──────────────────────────────────────────

    public Map<String, Object> getFraudStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAlerts",     fraudAlertRepository.count());
        stats.put("openAlerts",      fraudAlertRepository.countByStatus(com.finguard.model.FraudAlert.AlertStatus.OPEN));
        stats.put("criticalAlerts",  fraudAlertRepository.countBySeverity(com.finguard.model.FraudAlert.Severity.CRITICAL));
        stats.put("fraudTransactions", transactionRepository.countByFraudStatus(Transaction.FraudStatus.FRAUD));
        stats.put("suspiciousTransactions", transactionRepository.countByFraudStatus(Transaction.FraudStatus.SUSPICIOUS));

        return stats;
    }
}
