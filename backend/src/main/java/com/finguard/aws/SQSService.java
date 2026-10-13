package com.finguard.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finguard.kafka.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQSService – Manages Amazon SQS queues for external and cross-service event delivery.
 *
 * Role alongside Kafka:
 *  - Kafka:  internal high-throughput streaming (intra-service)
 *  - SQS:    external integration, fan-out, dead-letter, and cross-region events
 *
 * Queues:
 *  - External Transaction Events   → downstream partner integrations
 *  - Fraud Alert Fan-out Queue     → multi-consumer alert distribution
 *  - Dead-Letter Queue (DLQ)       → captures failed processing messages from Kafka
 *  - Report Generation Queue       → async report generation jobs
 *
 * Gracefully degrades when SQS is unavailable (logs warning, does not throw).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = false)
public class SQSService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.external-transactions-queue-url}")
    private String externalTransactionsQueueUrl;

    @Value("${aws.sqs.fraud-alerts-queue-url}")
    private String fraudAlertsQueueUrl;

    @Value("${aws.sqs.dlq-url}")
    private String dlqUrl;

    @Value("${aws.sqs.report-jobs-queue-url}")
    private String reportJobsQueueUrl;

    @Value("${aws.sqs.visibility-timeout-seconds:30}")
    private int visibilityTimeoutSeconds;

    @Value("${aws.sqs.max-messages-per-poll:10}")
    private int maxMessagesPerPoll;

    // ─── Publish ───────────────────────────────────────────────────

    /**
     * Publishes a transaction event to the external SQS queue for downstream consumers
     * (partner APIs, compliance systems, analytics pipelines, etc.)
     *
     * @param event TransactionEvent to publish
     */
    public void publishExternalTransactionEvent(TransactionEvent event) {
        try {
            String body = objectMapper.writeValueAsString(event);

            SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(externalTransactionsQueueUrl)
                .messageBody(body)
                .messageGroupId("transactions")           // FIFO queue group
                .messageDeduplicationId(event.getTransactionId()) // Idempotency
                .messageAttributes(Map.of(
                    "eventType", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("TRANSACTION_CREATED")
                        .build(),
                    "userId", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(event.getUserId())
                        .build()
                ))
                .build();

            SendMessageResponse response = sqsClient.sendMessage(request);
            log.info("SQS external transaction published: messageId={} transactionId={}",
                response.messageId(), event.getTransactionId());

        } catch (Throwable ex) {
            // SQS failure must never block transaction processing
            log.error("Failed to publish to SQS external transactions queue: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Publishes a fraud alert event to the SQS fraud alerts fan-out queue.
     * Multiple consumers can independently poll this queue (compliance, dashboards, etc.)
     *
     * @param alertPayload Map of alert fields to serialize
     */
    public void publishFraudAlertEvent(Map<String, Object> alertPayload) {
        try {
            String body = objectMapper.writeValueAsString(alertPayload);
            String transactionId = (String) alertPayload.getOrDefault("transactionId", "unknown");

            SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(fraudAlertsQueueUrl)
                .messageBody(body)
                .messageGroupId("fraud-alerts")
                .messageDeduplicationId("ALERT-" + transactionId + "-" + System.currentTimeMillis())
                .messageAttributes(Map.of(
                    "eventType", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("FRAUD_ALERT")
                        .build(),
                    "severity", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(String.valueOf(alertPayload.get("severity")))
                        .build()
                ))
                .build();

            SendMessageResponse response = sqsClient.sendMessage(request);
            log.info("SQS fraud alert published: messageId={} severity={}",
                response.messageId(), alertPayload.get("severity"));

        } catch (Throwable ex) {
            log.error("Failed to publish fraud alert to SQS: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Sends a failed message to the Dead-Letter Queue for manual review or retry.
     * Called when Kafka consumer exhausts retries.
     *
     * @param originalMessage  The original failed message body
     * @param errorDetails     Error description
     * @param originalQueue    Source queue/topic name for tracking
     */
    public void sendToDeadLetterQueue(String originalMessage, String errorDetails, String originalQueue) {
        try {
            Map<String, Object> dlqPayload = Map.of(
                "originalMessage",  originalMessage,
                "errorDetails",     errorDetails,
                "sourceQueue",      originalQueue,
                "failedAt",         java.time.Instant.now().toString()
            );

            SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(dlqUrl)
                .messageBody(objectMapper.writeValueAsString(dlqPayload))
                .messageAttributes(Map.of(
                    "source", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(originalQueue)
                        .build()
                ))
                .build();

            sqsClient.sendMessage(request);
            log.warn("Message sent to DLQ from source={}: {}", originalQueue, errorDetails);

        } catch (Throwable ex) {
            log.error("Critical: Failed to send message to DLQ: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Enqueues a report generation job to the async report jobs queue.
     *
     * @param reportType  Type of report: "TRANSACTIONS", "FRAUD_SUMMARY", "COMPLIANCE"
     * @param requestedBy User email who requested the report
     * @param params      Additional report parameters (date range, filters, etc.)
     */
    public void enqueueReportJob(String reportType, String requestedBy, Map<String, Object> params) {
        try {
            Map<String, Object> jobPayload = Map.of(
                "reportType",    reportType,
                "requestedBy",   requestedBy,
                "params",        params,
                "requestedAt",   java.time.Instant.now().toString(),
                "jobId",         java.util.UUID.randomUUID().toString()
            );

            SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(reportJobsQueueUrl)
                .messageBody(objectMapper.writeValueAsString(jobPayload))
                .messageAttributes(Map.of(
                    "reportType", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(reportType)
                        .build()
                ))
                .build();

            SendMessageResponse response = sqsClient.sendMessage(request);
            log.info("Report job enqueued: messageId={} reportType={} requestedBy={}",
                response.messageId(), reportType, requestedBy);

        } catch (Throwable ex) {
            log.error("Failed to enqueue report job: {}", ex.getMessage(), ex);
            throw new RuntimeException("Report job queue failed", ex);
        }
    }

    // ─── Consume ───────────────────────────────────────────────────

    /**
     * Polls the report jobs queue for pending report generation requests.
     * Used by the scheduled report processor.
     *
     * @return List of SQS messages ready for processing
     */
    public List<Message> pollReportJobs() {
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(reportJobsQueueUrl)
                .maxNumberOfMessages(maxMessagesPerPoll)
                .visibilityTimeout(visibilityTimeoutSeconds)
                .waitTimeSeconds(5) // Long polling
                .messageAttributeNames("All")
                .build();

            return sqsClient.receiveMessage(request).messages();

        } catch (Throwable ex) {
            log.error("Failed to poll SQS report jobs queue: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    /**
     * Deletes a processed message from SQS to acknowledge successful processing.
     *
     * @param queueUrl      The queue URL
     * @param receiptHandle The receipt handle from the received message
     */
    public void acknowledgeMessage(String queueUrl, String receiptHandle) {
        try {
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build());
            log.debug("SQS message acknowledged and deleted from queue={}", queueUrl);
        } catch (Throwable ex) {
            log.error("Failed to delete SQS message: {}", ex.getMessage(), ex);
        }
    }

    // ─── Queue Info ────────────────────────────────────────────────

    /**
     * Returns the approximate number of messages visible in a queue.
     * Used for monitoring / CloudWatch custom metrics.
     */
    public int getApproximateMessageCount(String queueUrl) {
        try {
            GetQueueAttributesResponse attrs = sqsClient.getQueueAttributes(
                GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                    .build()
            );
            String count = attrs.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
            return count != null ? Integer.parseInt(count) : 0;
        } catch (Throwable ex) {
            log.error("Failed to get SQS message count for {}: {}", queueUrl, ex.getMessage());
            return -1;
        }
    }

    public String getExternalTransactionsQueueUrl() { return externalTransactionsQueueUrl; }
    public String getFraudAlertsQueueUrl() { return fraudAlertsQueueUrl; }
    public String getDlqUrl() { return dlqUrl; }
    public String getReportJobsQueueUrl() { return reportJobsQueueUrl; }
}
