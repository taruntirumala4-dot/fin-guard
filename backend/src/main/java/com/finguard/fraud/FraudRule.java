package com.finguard.fraud;

import com.finguard.model.Transaction;

/**
 * FraudRule – interface for all fraud detection rules.
 *
 * Open/Closed Principle: the rule engine is open for extension (add new rules)
 * but closed for modification (no changes to FraudRuleEngine needed).
 *
 * Each rule:
 *   1. evaluate()      → whether this rule triggers for a given transaction
 *   2. getRiskScore()  → risk points contributed (0–100 scale)
 *   3. getReason()     → human-readable explanation
 *   4. getRuleName()   → identifier for logging and alerts
 */
public interface FraudRule {

    /**
     * Evaluates whether this rule triggers for the given transaction.
     *
     * @param transaction the transaction to evaluate
     * @return true if the rule is triggered (fraud signal detected)
     */
    boolean evaluate(Transaction transaction);

    /**
     * @return number of risk points contributed when rule fires (0–100 scale)
     */
    int getRiskScore();

    /**
     * @return human-readable explanation of why this rule fired
     */
    String getReason();

    /**
     * @return unique identifier for this rule (e.g. "HIGH_AMOUNT")
     */
    String getRuleName();
}
