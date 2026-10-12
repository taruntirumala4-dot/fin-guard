package com.finguard.dto;

import com.finguard.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * TransactionResponse – safe DTO that never exposes internal DB IDs directly
 * and excludes sensitive user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private String id;
    private String transactionId;
    private String userId;
    private String userName;
    private BigDecimal amount;
    private String currency;
    private String merchant;
    private String location;
    private Transaction.TransactionType transactionType;
    private String deviceId;
    private Instant timestamp;
    private Transaction.TransactionStatus status;
    private BigDecimal fraudScore;
    private Transaction.FraudStatus fraudStatus;
    private Instant createdAt;

    public static TransactionResponse from(Transaction t) {
        return TransactionResponse.builder()
            .id(String.valueOf(t.getId()))
            .transactionId(t.getTransactionId())
            .userId(t.getUserId())
            .userName(t.getUserName())
            .amount(t.getAmount())
            .currency(t.getCurrency())
            .merchant(t.getMerchant())
            .location(t.getLocation())
            .transactionType(t.getTransactionType())
            .deviceId(t.getDeviceId())
            .timestamp(t.getTimestamp())
            .status(t.getStatus())
            .fraudScore(t.getFraudScore())
            .fraudStatus(t.getFraudStatus())
            .createdAt(t.getCreatedAt())
            .build();
    }
}
