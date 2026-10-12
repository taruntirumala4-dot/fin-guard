package com.finguard.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transaction entity – core financial transaction record in SQL database.
 *
 * fraud_score: 0–100 composite score (rule-based + ML)
 * fraud_status: SAFE | SUSPICIOUS | FRAUD
 */
@Entity
@Table(name = "transactions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 50)
    private String transactionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "user_name")
    private String userName;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 10, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(nullable = false)
    private String merchant;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "fraud_score", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal fraudScore = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "fraud_status", nullable = false)
    @Builder.Default
    private FraudStatus fraudStatus = FraudStatus.SAFE;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    // ─── Enums ───────────────────────────────────────────────

    public enum TransactionType {
        PAYMENT, TRANSFER, WITHDRAWAL, DEPOSIT
    }

    public enum TransactionStatus {
        SUCCESS, FAILED, PENDING, BLOCKED
    }

    public enum FraudStatus {
        SAFE, SUSPICIOUS, FRAUD
    }
}
