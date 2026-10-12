package com.finguard.fraud;

import com.finguard.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * FraudRuleEngine – orchestrates all FraudRule implementations.
 *
 * Design patterns used:
 *   - Strategy Pattern: each FraudRule is a strategy injected by Spring
 *   - Open/Closed Principle: add new rules without modifying this class
 *
 * Algorithm:
 *   Iterate through all registered rules (O(R) where R = number of rules).
 *   For each triggered rule, accumulate its risk score.
 *   Cap total rule score at 100.
 *
 * The engine returns a RuleEvaluationResult containing:
 *   - total rule score (0–100)
 *   - list of triggered rule names
 *   - list of human-readable reasons
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FraudRuleEngine {

    private final List<FraudRule> rules; // Spring auto-injects all FraudRule beans

    /**
     * Evaluates all registered rules against a transaction.
     *
     * @param transaction the transaction to evaluate
     * @return evaluation result with score and triggered rules
     */
    public RuleEvaluationResult evaluate(Transaction transaction) {
        log.info("Evaluating {} fraud rules for transaction: {}",
            rules.size(), transaction.getTransactionId());

        List<String> triggeredRules = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        int totalScore = 0;

        for (FraudRule rule : rules) {
            try {
                if (rule.evaluate(transaction)) {
                    triggeredRules.add(rule.getRuleName());
                    reasons.add(rule.getReason());
                    totalScore += rule.getRiskScore();
                    log.debug("Rule {} triggered, score contribution: {}",
                        rule.getRuleName(), rule.getRiskScore());
                }
            } catch (Exception ex) {
                log.error("Error evaluating rule {}: {}", rule.getRuleName(), ex.getMessage());
                // Continue with other rules even if one fails
            }
        }

        // Cap rule score at 100
        int finalScore = Math.min(totalScore, 100);

        log.info("Rule evaluation complete for {}: score={}, triggered={}",
            transaction.getTransactionId(), finalScore, triggeredRules);

        return new RuleEvaluationResult(finalScore, triggeredRules, reasons);
    }

    /**
     * Value object holding rule evaluation results.
     */
    public record RuleEvaluationResult(
        int ruleScore,
        List<String> triggeredRules,
        List<String> reasons
    ) {
        public boolean hasTriggeredRules() {
            return !triggeredRules.isEmpty();
        }

        public String combinedAlertType() {
            return String.join(", ", triggeredRules);
        }
    }
}
