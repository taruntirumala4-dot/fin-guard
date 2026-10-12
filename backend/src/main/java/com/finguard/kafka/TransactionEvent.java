package com.finguard.kafka;

import com.finguard.model.Transaction;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * TransactionEvent – the Kafka message payload.
 *
 * This is a plain POJO (not a JPA entity) designed for JSON serialization
 * over Kafka. Keep it simple and serializable.
 */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class TransactionEvent {

    private String transactionId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String merchant;
    private String location;
    private Transaction.TransactionType transactionType;
    private String deviceId;
    private Instant timestamp;
}
