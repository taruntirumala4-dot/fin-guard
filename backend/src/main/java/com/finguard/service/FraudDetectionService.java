package com.finguard.service;

import com.finguard.aws.CloudWatchService;
import com.finguard.aws.LambdaService;
import com.finguard.fraud.FraudRuleEngine;
import com.finguard.kafka.TransactionEvent;
import com.finguard.model.FraudResult;
import com.finguard.model.Transaction;
import com.finguard.redis.RedisKeyGenerator;
import com.finguard.redis.RedisService;
import com.finguard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FraudDetectionService – orchestrates the full fraud analysis pipeline.
 *
 * Processing order (matches Transaction Processing Flow in spec §8):
 *   1. Load transaction from DB
 *   2. Run rule engine
 *   3. Call ML service for probability score
 *   4. Combine: finalScore = (ruleScore × 0.4) + (mlScore × 0.6)
 *   5. Determine fraud status based on thresholds
 *   6. Update transaction in MongoDB
 *   7. Create fraud alert if needed (via AlertService)
 *   8. Cache result in Redis
 */
@SuppressWarnings("unused")
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final TransactionRepository transactionRepository;
    private final FraudRuleEngine fraudRuleEngine;
    private final AlertService alertService;
    private final RedisService redisService;
    private final WebClient.Builder webClientBuilder;

    /** Optional AWS services – null when aws.enabled=false (local dev) */
    @Autowired(required = false)
    @Nullable
    private LambdaService lambdaService;

    @Autowired(required = false)
    @Nullable
    private CloudWatchService cloudWatchService;

    @Value("${finguard.ml-service.url}")
    private String mlServiceUrl;

    @Value("${finguard.ml-service.timeout-seconds:5}")
    private int mlTimeoutSeconds;

    @Value("${finguard.fraud.safe-threshold:30}")
    private double safeThreshold;

    @Value("${finguard.fraud.suspicious-threshold:60}")
    private double suspiciousThreshold;

    @Value("${finguard.fraud.rule-weight:0.4}")
    private double ruleWeight;

    @Value("${finguard.fraud.ml-weight:0.6}")
    private double mlWeight;

    @Value("${finguard.redis.ttl.transaction-seconds:1800}")
    private long transactionTtlSeconds;

    // ─── Main Entry (called by Kafka consumer) ────────────────

    public void processTransaction(TransactionEvent event) {
        Transaction transaction = transactionRepository
            .findByTransactionId(event.getTransactionId())
            .orElse(null);

        if (transaction == null) {
            log.error("Transaction not found for event: {}", event.getTransactionId());
            return;
        }

        long startMs = System.currentTimeMillis();
        log.info("Starting fraud analysis for: {}", transaction.getTransactionId());

        // Step 1: Rule engine
        FraudRuleEngine.RuleEvaluationResult ruleResult = fraudRuleEngine.evaluate(transaction);
        log.info("Rule score for {}: {}", transaction.getTransactionId(), ruleResult.ruleScore());

        // Step 2: ML service
        double mlProbability = callMlService(transaction, ruleResult);
        double mlScore = mlProbability * 100;

        // Step 3: Combined score (rule engine + ML)
        double combinedScore = (ruleResult.ruleScore() * ruleWeight) + (mlScore * mlWeight);
        combinedScore = Math.min(combinedScore, 100.0);

        // Step 4: Lambda enrichment (geo-IP, device reputation, merchant risk)
        //         Optional – degrades gracefully when AWS is offline
        double finalScore = enrichWithLambda(event, combinedScore);

        // Step 5: Determine fraud status
        Transaction.FraudStatus fraudStatus = determineFraudStatus(finalScore);
        Transaction.TransactionStatus txStatus = determineTxStatus(fraudStatus);

        // Step 6: Update transaction
        transaction.setFraudScore(BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP));
        transaction.setFraudStatus(fraudStatus);
        transaction.setStatus(txStatus);
        transactionRepository.save(transaction);

        long processingMs = System.currentTimeMillis() - startMs;
        log.info("Transaction {} analyzed: score={} status={} durationMs={}",
            transaction.getTransactionId(), finalScore, fraudStatus, processingMs);

        // Step 7: Create alert for suspicious/fraud
        if (fraudStatus != Transaction.FraudStatus.SAFE) {
            alertService.createAlert(transaction, ruleResult, finalScore);
        }

        // Step 8: Publish CloudWatch metrics
        if (cloudWatchService != null) {
            cloudWatchService.recordTransactionProcessed(
                finalScore,
                fraudStatus == Transaction.FraudStatus.FRAUD,
                processingMs
            );
        }

        // Step 9: Cache invalidation
        redisService.evict(RedisKeyGenerator.transactionKey(transaction.getTransactionId()));
        redisService.evict(RedisKeyGenerator.dashboardStatsKey());
        redisService.evict(RedisKeyGenerator.recentFraudAlertsKey());
    }

    // ─── Direct analysis (REST endpoint) ─────────────────────

    public FraudResult analyzeTransaction(String transactionId) {
        Transaction transaction = transactionRepository
            .findByTransactionId(transactionId)
            .orElseThrow(() -> new com.finguard.exception.ResourceNotFoundException(
                "Transaction", "transactionId", transactionId));

        FraudRuleEngine.RuleEvaluationResult ruleResult = fraudRuleEngine.evaluate(transaction);
        double mlProbability = callMlService(transaction, ruleResult);
        double mlScore = mlProbability * 100;
        double finalScore = Math.min(
            (ruleResult.ruleScore() * ruleWeight) + (mlScore * mlWeight), 100.0);

        Transaction.FraudStatus fraudStatus = determineFraudStatus(finalScore);

        return FraudResult.builder()
            .ruleScore(ruleResult.ruleScore())
            .mlScore(mlScore)
            .finalScore(finalScore)
            .fraudStatus(fraudStatus)
            .reasons(ruleResult.reasons())
            .alertType(ruleResult.combinedAlertType())
            .mlServiceAvailable(mlProbability >= 0)
            .build();
    }

    // ─── ML Service call ──────────────────────────────────────

    private double callMlService(Transaction transaction,
                                  FraudRuleEngine.RuleEvaluationResult ruleResult) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("amount", transaction.getAmount().doubleValue());
            payload.put("transaction_frequency", ruleResult.ruleScore() > 0 ? 6 : 2);
            payload.put("location_change", ruleResult.triggeredRules()
                .contains("LOCATION_MISMATCH") ? 1 : 0);
            payload.put("device_change", ruleResult.triggeredRules()
                .contains("DEVICE_CHANGE") ? 1 : 0);
            payload.put("transaction_type", transaction.getTransactionType().name());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(mlServiceUrl + "/predict")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(mlTimeoutSeconds))
                .block();

            if (response != null && response.containsKey("fraud_probability")) {
                double prob = ((Number) response.get("fraud_probability")).doubleValue();
                log.info("ML service returned probability={} for transaction={}",
                    prob, transaction.getTransactionId());
                return prob;
            }
        } catch (Exception ex) {
            log.warn("ML service unavailable for transaction {}: {}. Using rule score only.",
                transaction.getTransactionId(), ex.getMessage());
        }

        // Fallback: return 0.5 (neutral) if ML service is unreachable
        return 0.5;
    }

    /**
     * Optionally enriches the combined fraud score via Lambda.
     * Returns combinedScore unchanged if Lambda is disabled or unavailable.
     */
    private double enrichWithLambda(TransactionEvent event, double combinedScore) {
        if (lambdaService == null) return combinedScore;
        try {
            return lambdaService.invokeFraudEnrichment(event, combinedScore);
        } catch (Throwable ex) {
            log.warn("Lambda enrichment failed, using combinedScore={}: {}", combinedScore, ex.getMessage());
            return combinedScore;
        }
    }

    // ─── Helpers ──────────────────────────────────────────────

    private Transaction.FraudStatus determineFraudStatus(double score) {
        if (score <= safeThreshold) return Transaction.FraudStatus.SAFE;
        if (score <= suspiciousThreshold) return Transaction.FraudStatus.SUSPICIOUS;
        return Transaction.FraudStatus.FRAUD;
    }

    private Transaction.TransactionStatus determineTxStatus(Transaction.FraudStatus fraudStatus) {
        return switch (fraudStatus) {
            case FRAUD -> Transaction.TransactionStatus.BLOCKED;
            case SUSPICIOUS -> Transaction.TransactionStatus.SUCCESS; // Flag but allow
            case SAFE -> Transaction.TransactionStatus.SUCCESS;
        };
    }
}
