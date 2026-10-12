package com.finguard.service;

import com.finguard.dto.FraudAlertResponse;
import com.finguard.exception.ResourceNotFoundException;
import com.finguard.fraud.FraudRuleEngine;
import com.finguard.model.FraudAlert;
import com.finguard.model.Transaction;
import com.finguard.redis.RedisKeyGenerator;
import com.finguard.redis.RedisService;
import com.finguard.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AlertService – creates and manages fraud alerts.
 *
 * Severity derivation:
 *   0–30   → (shouldn't reach here if SAFE)
 *   31–60  → MEDIUM (SUSPICIOUS)
 *   61–80  → HIGH
 *   81–100 → CRITICAL
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final FraudAlertRepository fraudAlertRepository;
    private final RedisService redisService;

    @Value("${finguard.redis.ttl.fraud-alerts-seconds:300}")
    private long fraudAlertsTtlSeconds;

    // ─── Create ───────────────────────────────────────────────

    public FraudAlert createAlert(Transaction transaction,
                                   FraudRuleEngine.RuleEvaluationResult ruleResult,
                                   double finalScore) {
        FraudAlert.Severity severity = deriveSeverity(finalScore);
        String reason = String.join(". ", ruleResult.reasons());
        if (reason.isEmpty()) reason = "Fraud score threshold exceeded";

        String alertType = ruleResult.hasTriggeredRules()
            ? ruleResult.combinedAlertType()
            : "SCORE_THRESHOLD";

        FraudAlert alert = FraudAlert.builder()
            .transactionId(transaction.getTransactionId())
            .alertType(alertType)
            .severity(severity)
            .reason(reason)
            .fraudScore(BigDecimal.valueOf(finalScore))
            .status(FraudAlert.AlertStatus.OPEN)
            .build();

        FraudAlert saved = fraudAlertRepository.save(alert);
        log.info("Fraud alert created: alertId={} transactionId={} severity={}",
            saved.getId(), transaction.getTransactionId(), severity);

        // Invalidate recent alerts cache
        redisService.evict(RedisKeyGenerator.recentFraudAlertsKey());

        return saved;
    }

    // ─── Read ─────────────────────────────────────────────────

    public Page<FraudAlertResponse> getAlerts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return fraudAlertRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(FraudAlertResponse::from);
    }

    @SuppressWarnings("null")
    public FraudAlertResponse getAlertById(String id) {
        return fraudAlertRepository.findById(Long.valueOf(id))
            .map(FraudAlertResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("FraudAlert", "id", id));
    }

    public Map<String, Object> getStatistics() {
        long totalAlerts  = fraudAlertRepository.count();
        long openAlerts   = fraudAlertRepository.countByStatus(FraudAlert.AlertStatus.OPEN);
        long criticalAlerts = fraudAlertRepository.countBySeverity(FraudAlert.Severity.CRITICAL);
        long highAlerts   = fraudAlertRepository.countBySeverity(FraudAlert.Severity.HIGH);

        List<Object[]> bySeverity = fraudAlertRepository.countBySeverityGrouped();

        return Map.of(
            "total",    totalAlerts,
            "open",     openAlerts,
            "critical", criticalAlerts,
            "high",     highAlerts,
            "bySeverity", bySeverity
        );
    }

    @SuppressWarnings("unchecked")
    public List<FraudAlertResponse> getRecentAlerts() {
        // Try cache first
        Optional<List<FraudAlertResponse>> cached =
            (Optional<List<FraudAlertResponse>>) (Optional<?>) redisService.get(
                RedisKeyGenerator.recentFraudAlertsKey(), List.class);
        if (cached.isPresent()) return cached.get();

        List<FraudAlertResponse> alerts = fraudAlertRepository
            .findTop5ByStatusOrderByCreatedAtDesc(FraudAlert.AlertStatus.OPEN)
            .stream()
            .map(FraudAlertResponse::from)
            .toList();

        redisService.set(RedisKeyGenerator.recentFraudAlertsKey(),
            alerts, Duration.ofSeconds(fraudAlertsTtlSeconds));

        return alerts;
    }

    // ─── Helpers ──────────────────────────────────────────────

    private FraudAlert.Severity deriveSeverity(double score) {
        if (score > 80) return FraudAlert.Severity.CRITICAL;
        if (score > 60) return FraudAlert.Severity.HIGH;
        if (score > 30) return FraudAlert.Severity.MEDIUM;
        return FraudAlert.Severity.LOW;
    }

}
