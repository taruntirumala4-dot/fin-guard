package com.finguard.model;

import lombok.*;
import java.util.List;

/**
 * FraudResult – value object (not a database entity) that carries the
 * combined result of rule-based + ML fraud analysis.
 *
 * Algorithmic note:
 *   finalScore = (ruleScore × ruleWeight) + (mlScore × mlWeight)
 *   where ruleWeight + mlWeight = 1.0
 *
 * Time complexity of rule evaluation: O(R) where R = number of rules.
 * Space complexity: O(T) where T = recent transactions for velocity checks.
 */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudResult {

    /** Sum of risk points from all triggered rules (0–100) */
    private double ruleScore;

    /** ML model fraud probability × 100 (0–100) */
    private double mlScore;

    /** Combined weighted score: ruleWeight*ruleScore + mlWeight*mlScore */
    private double finalScore;

    /** SAFE | SUSPICIOUS | FRAUD */
    private Transaction.FraudStatus fraudStatus;

    /** List of human-readable reasons from triggered rules */
    private List<String> reasons;

    /** Combined alert type label (comma-separated rule names) */
    private String alertType;

    /** Whether the ML service was reachable */
    private boolean mlServiceAvailable;
}
