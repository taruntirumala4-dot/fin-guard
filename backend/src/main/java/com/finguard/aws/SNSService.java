package com.finguard.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finguard.model.FraudAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * SNSService – Publishes fraud alerts and system notifications via Amazon SNS.
 *
 * Topics:
 *  - Fraud Alerts: HIGH and CRITICAL severity alerts → ops / security teams
 *  - Email subscriptions for compliance reporting
 *  - SMS for critical alerts (configurable)
 *
 * Gracefully degrades when AWS SNS is unavailable (logs warning, does not throw).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = false)
public class SNSService {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sns.fraud-alerts-topic-arn}")
    private String fraudAlertsTopicArn;

    @Value("${aws.sns.system-alerts-topic-arn}")
    private String systemAlertsTopicArn;

    @Value("${aws.sns.reports-topic-arn:#{null}}")
    private String reportsTopicArn;

    // ─── Fraud Alert Notifications ─────────────────────────────────

    /**
     * Publishes a fraud alert notification to the SNS fraud-alerts topic.
     * Only HIGH and CRITICAL severity alerts trigger SNS notifications.
     *
     * @param alert       The FraudAlert entity
     * @param transactionId Associated transaction ID
     * @param amount      Transaction amount
     */
    public void publishFraudAlert(FraudAlert alert, String transactionId, BigDecimal amount) {
        if (alert.getSeverity() != FraudAlert.Severity.HIGH
                && alert.getSeverity() != FraudAlert.Severity.CRITICAL) {
            log.debug("Skipping SNS notification for severity={} (below threshold)", alert.getSeverity());
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                "alertId",       alert.getId(),
                "transactionId", transactionId,
                "alertType",     alert.getAlertType(),
                "severity",      alert.getSeverity().name(),
                "fraudScore",    alert.getFraudScore(),
                "reason",        alert.getReason(),
                "amount",        amount,
                "status",        alert.getStatus().name(),
                "timestamp",     alert.getCreatedAt() != null ? alert.getCreatedAt().toString() : "unknown"
            );

            String messageBody = objectMapper.writeValueAsString(payload);

            Map<String, MessageAttributeValue> attributes = new HashMap<>();
            attributes.put("severity", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(alert.getSeverity().name())
                .build());
            attributes.put("alertType", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(alert.getAlertType())
                .build());

            PublishRequest request = PublishRequest.builder()
                .topicArn(fraudAlertsTopicArn)
                .subject(buildSubject(alert))
                .message(messageBody)
                .messageAttributes(attributes)
                .build();

            PublishResponse response = snsClient.publish(request);
            log.info("SNS fraud alert published: messageId={} severity={} transactionId={}",
                response.messageId(), alert.getSeverity(), transactionId);

        } catch (Throwable ex) {
            // SNS failure must never crash the transaction processing pipeline
            log.error("Failed to publish SNS fraud alert for transactionId={}: {}",
                transactionId, ex.getMessage(), ex);
        }
    }

    /**
     * Publishes a system health/operational alert to the system alerts topic.
     *
     * @param title   Alert title (e.g., "High Fraud Rate Detected")
     * @param message Detailed message body
     * @param level   Alert level: INFO, WARNING, CRITICAL
     */
    public void publishSystemAlert(String title, String message, String level) {
        try {
            Map<String, Object> payload = Map.of(
                "title",   title,
                "message", message,
                "level",   level,
                "service", "FinGuard-Backend",
                "timestamp", java.time.Instant.now().toString()
            );

            PublishRequest request = PublishRequest.builder()
                .topicArn(systemAlertsTopicArn)
                .subject("[FinGuard] " + level + ": " + title)
                .message(objectMapper.writeValueAsString(payload))
                .messageAttributes(Map.of(
                    "level", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(level)
                        .build()
                ))
                .build();

            PublishResponse response = snsClient.publish(request);
            log.info("SNS system alert published: messageId={} level={} title={}", response.messageId(), level, title);

        } catch (Throwable ex) {
            log.error("Failed to publish SNS system alert: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Publishes a report-ready notification when a report has been uploaded to S3.
     *
     * @param reportKey  S3 key of the report
     * @param reportName Human-readable report name
     * @param presignedUrl Pre-signed download URL
     */
    public void publishReportReady(String reportKey, String reportName, String presignedUrl) {
        if (reportsTopicArn == null) {
            log.debug("Reports SNS topic not configured, skipping notification");
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                "reportName",   reportName,
                "s3Key",        reportKey,
                "downloadUrl",  presignedUrl,
                "generatedAt",  java.time.Instant.now().toString()
            );

            PublishRequest request = PublishRequest.builder()
                .topicArn(reportsTopicArn)
                .subject("[FinGuard] Report Ready: " + reportName)
                .message(objectMapper.writeValueAsString(payload))
                .build();

            snsClient.publish(request);
            log.info("SNS report-ready notification sent for report={}", reportName);

        } catch (Throwable ex) {
            log.error("Failed to send SNS report-ready notification: {}", ex.getMessage(), ex);
        }
    }

    // ─── Topic Management ──────────────────────────────────────────

    /**
     * Subscribes an email address to the fraud alerts topic.
     * Subscription requires email confirmation by the recipient.
     */
    public String subscribeEmail(String email) {
        try {
            SubscribeRequest request = SubscribeRequest.builder()
                .topicArn(fraudAlertsTopicArn)
                .protocol("email")
                .endpoint(email)
                .build();

            SubscribeResponse response = snsClient.subscribe(request);
            log.info("Email subscription initiated: email={} subscriptionArn={}", email, response.subscriptionArn());
            return response.subscriptionArn();

        } catch (Throwable ex) {
            log.error("Failed to subscribe email={} to SNS: {}", email, ex.getMessage(), ex);
            throw new RuntimeException("SNS subscription failed", ex);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private String buildSubject(FraudAlert alert) {
        return String.format("[FinGuard] %s FRAUD ALERT – Score: %.1f",
            alert.getSeverity().name(),
            alert.getFraudScore().doubleValue());
    }
}
