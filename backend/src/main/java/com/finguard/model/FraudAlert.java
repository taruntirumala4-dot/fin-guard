package com.finguard.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * FraudAlert entity – created when a transaction is flagged SUSPICIOUS or FRAUD in SQL database.
 *
 * alert_type: e.g. HIGH_AMOUNT, RAPID_TRANSACTION, LOCATION_MISMATCH, etc.
 * severity:   LOW | MEDIUM | HIGH | CRITICAL  (derived from fraud score)
 * status:     OPEN → INVESTIGATING → RESOLVED | FALSE_POSITIVE
 */
@Entity
@Table(name = "fraud_alerts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 50)
    private String transactionId;

    @Column(name = "alert_type", nullable = false, length = 100)
    private String alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "fraud_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal fraudScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum AlertStatus {
        OPEN, INVESTIGATING, RESOLVED, FALSE_POSITIVE
    }
}
