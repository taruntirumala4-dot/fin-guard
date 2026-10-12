package com.finguard.kafka;

import com.finguard.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * TransactionConsumer – subscribes to the 'transactions' Kafka topic
 * and drives the fraud detection pipeline.
 *
 * Error handling strategy:
 *   - Each message is processed independently.
 *   - A single bad message does NOT crash the consumer.
 *   - Exceptions are caught, logged, and the message is acknowledged
 *     (dead-letter-queue is a future enhancement).
 */
@SuppressWarnings("unused")
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {

    private final FraudDetectionService fraudDetectionService;

    @KafkaListener(
        topics = "transactions",
        groupId = "finguard-transaction-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, TransactionEvent> record) {
        log.info("Received transaction event from Kafka: key={} partition={} offset={}",
            record.key(), record.partition(), record.offset());

        TransactionEvent event = record.value();

        if (event == null) {
            log.warn("Received null event from Kafka – skipping");
            return;
        }

        try {
            log.info("Processing transaction: {}", event.getTransactionId());
            fraudDetectionService.processTransaction(event);
            log.info("Fraud detection completed for: {}", event.getTransactionId());
        } catch (Exception ex) {
            // Log the error but don't re-throw – prevents consumer from crashing
            log.error("Error processing transaction {}: {}",
                event.getTransactionId(), ex.getMessage(), ex);
        }
    }
}
