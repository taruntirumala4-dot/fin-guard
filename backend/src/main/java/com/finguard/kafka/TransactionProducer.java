package com.finguard.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.concurrent.CompletableFuture;

/**
 * TransactionProducer – publishes TransactionEvent messages to the
 * 'transactions' Kafka topic.
 *
 * Uses the async KafkaTemplate.send() API. Failures are logged but
 * do not fail the original HTTP request (Kafka is asynchronous).
 */
@Component
@Slf4j
public class TransactionProducer {

    private static final String TOPIC = "transactions";

    @Autowired(required = false)
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    /**
     * Publishes a transaction event to Kafka.
     *
     * @param event the transaction event to publish
     */
    public void publishTransaction(TransactionEvent event) {
        if (kafkaTemplate == null) {
            log.info("Kafka disabled in local profile – skipping async Kafka publish for {}",
                event.getTransactionId());
            return;
        }

        log.info("Publishing transaction event to Kafka: transactionId={}",
            event.getTransactionId());

        try {
            CompletableFuture<SendResult<String, TransactionEvent>> future =
                kafkaTemplate.send(TOPIC, event.getTransactionId(), event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish transaction {} to Kafka: {}",
                        event.getTransactionId(), ex.getMessage());
                } else {
                    log.info("Transaction {} published to Kafka partition={} offset={}",
                        event.getTransactionId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.warn("Kafka unavailable – skipping Kafka publish for transaction {}: {}",
                event.getTransactionId(), ex.getMessage());
        }
    }
}
