package com.finguard.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finguard.kafka.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * LambdaService – Invokes AWS Lambda functions for event-driven processing.
 *
 * Lambda Functions Integrated:
 *  1. finguard-fraud-enrichment     – Enriches fraud scores with external data sources
 *                                     (geo-IP, device reputation, merchant risk profiles)
 *  2. finguard-report-generator     – Async report generation (CSV/PDF) triggered by SQS
 *  3. finguard-compliance-checker   – Validates transactions against regulatory rules
 *  4. finguard-alert-dispatcher     – Routes alerts to appropriate notification channels
 *
 * Invocation Modes:
 *  - RequestResponse (synchronous): fraud enrichment, compliance checks
 *  - Event (asynchronous/fire-and-forget): report generation, alert dispatch
 *
 * Gracefully degrades when Lambda is unavailable (returns empty/default response).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = false)
public class LambdaService {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.lambda.fraud-enrichment-function:finguard-fraud-enrichment}")
    private String fraudEnrichmentFunction;

    @Value("${aws.lambda.report-generator-function:finguard-report-generator}")
    private String reportGeneratorFunction;

    @Value("${aws.lambda.compliance-checker-function:finguard-compliance-checker}")
    private String complianceCheckerFunction;

    @Value("${aws.lambda.alert-dispatcher-function:finguard-alert-dispatcher}")
    private String alertDispatcherFunction;

    // ─── Fraud Enrichment ──────────────────────────────────────────

    /**
     * Invokes the fraud enrichment Lambda to augment the base fraud score
     * with external intelligence (geo-IP, device reputation, merchant category risk).
     *
     * @param event       Original transaction event
     * @param baseScore   Internally computed fraud score (rule engine + ML)
     * @return Enriched fraud score, or baseScore on Lambda unavailability
     */
    public double invokeFraudEnrichment(TransactionEvent event, double baseScore) {
        try {
            Map<String, Object> payload = Map.of(
                "transactionId", event.getTransactionId(),
                "userId",        event.getUserId(),
                "amount",        event.getAmount(),
                "currency",      event.getCurrency(),
                "merchant",      event.getMerchant(),
                "location",      event.getLocation(),
                "deviceId",      event.getDeviceId() != null ? event.getDeviceId() : "",
                "baseScore",     baseScore,
                "timestamp",     event.getTimestamp() != null ? event.getTimestamp().toString() : ""
            );

            String responseJson = invokeSynchronous(fraudEnrichmentFunction, payload);
            if (responseJson == null) return baseScore;

            Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
            Object enrichedScore = responseMap.get("enrichedScore");
            if (enrichedScore instanceof Number number) {
                double score = number.doubleValue();
                log.info("Lambda fraud enrichment: transactionId={} base={} enriched={}",
                    event.getTransactionId(), baseScore, score);
                return Math.min(100.0, Math.max(0.0, score));
            }

            return baseScore;

        } catch (Throwable ex) {
            // Lambda enrichment failure must never block fraud detection
            log.warn("Lambda fraud enrichment unavailable for transactionId={}: {}",
                event.getTransactionId(), ex.getMessage());
            return baseScore;
        }
    }

    // ─── Report Generation ─────────────────────────────────────────

    /**
     * Asynchronously triggers the report generation Lambda.
     * The Lambda will generate the report and upload it to S3.
     *
     * @param reportType  "TRANSACTIONS", "FRAUD_SUMMARY", "COMPLIANCE_AUDIT"
     * @param params      Report parameters (dateRange, userId, filters)
     * @param requestedBy Email of the requesting user
     */
    public void triggerReportGeneration(String reportType, Map<String, Object> params, String requestedBy) {
        try {
            Map<String, Object> payload = Map.of(
                "reportType",  reportType,
                "params",      params,
                "requestedBy", requestedBy,
                "requestedAt", java.time.Instant.now().toString()
            );

            invokeAsynchronous(reportGeneratorFunction, payload);
            log.info("Lambda report generation triggered: reportType={} requestedBy={}", reportType, requestedBy);

        } catch (Throwable ex) {
            log.error("Failed to trigger Lambda report generation: {}", ex.getMessage(), ex);
            throw new RuntimeException("Report generation Lambda invocation failed", ex);
        }
    }

    // ─── Compliance Check ──────────────────────────────────────────

    /**
     * Runs a compliance check Lambda for a transaction (AML, KYC, regulatory thresholds).
     *
     * @param transactionId Transaction to validate
     * @param amount        Transaction amount
     * @param userId        User initiating the transaction
     * @return ComplianceResult with isCompliant flag and any violation codes
     */
    public ComplianceResult invokeComplianceCheck(String transactionId, java.math.BigDecimal amount, String userId) {
        try {
            Map<String, Object> payload = Map.of(
                "transactionId", transactionId,
                "amount",        amount.doubleValue(),
                "userId",        userId,
                "currency",      "INR",
                "timestamp",     java.time.Instant.now().toString()
            );

            String responseJson = invokeSynchronous(complianceCheckerFunction, payload);
            if (responseJson == null) {
                return ComplianceResult.defaultPass();
            }

            Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
            boolean isCompliant = Boolean.TRUE.equals(responseMap.get("isCompliant"));
            String violationCode = (String) responseMap.get("violationCode");
            Object reasonObj = responseMap.get("reason");
            String reason = reasonObj != null ? (String) reasonObj : "";

            log.info("Compliance check: transactionId={} isCompliant={} violationCode={}",
                transactionId, isCompliant, violationCode);

            return new ComplianceResult(isCompliant, violationCode, reason);

        } catch (Throwable ex) {
            log.warn("Lambda compliance check unavailable for transactionId={}: {}",
                transactionId, ex.getMessage());
            return ComplianceResult.defaultPass(); // Fail-open for availability
        }
    }

    // ─── Alert Dispatch ────────────────────────────────────────────

    /**
     * Asynchronously dispatches an alert via the alert-dispatcher Lambda.
     * The Lambda routes the alert to the appropriate channel based on severity
     * (SNS, PagerDuty, Slack, email, etc.)
     *
     * @param alertPayload Alert data to dispatch
     */
    public void dispatchAlert(Map<String, Object> alertPayload) {
        try {
            invokeAsynchronous(alertDispatcherFunction, alertPayload);
            log.info("Lambda alert dispatch triggered: severity={}", alertPayload.get("severity"));
        } catch (Throwable ex) {
            log.error("Failed to invoke Lambda alert dispatcher: {}", ex.getMessage(), ex);
        }
    }

    // ─── Lambda Management ─────────────────────────────────────────

    /**
     * Lists all FinGuard Lambda functions (for health check / admin endpoints).
     */
    public java.util.List<String> listFunctions() {
        try {
            ListFunctionsResponse response = lambdaClient.listFunctions(
                ListFunctionsRequest.builder()
                    .functionVersion(FunctionVersion.ALL)
                    .build()
            );
            return response.functions().stream()
                .map(FunctionConfiguration::functionName)
                .filter(name -> name.startsWith("finguard-"))
                .toList();
        } catch (Throwable ex) {
            log.error("Failed to list Lambda functions: {}", ex.getMessage());
            return java.util.List.of();
        }
    }

    // ─── Invoke Helpers ────────────────────────────────────────────

    /**
     * Invokes a Lambda function synchronously and returns the response payload.
     * Throws if the Lambda returns a function error.
     */
    private String invokeSynchronous(String functionName, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .invocationType(InvocationType.REQUEST_RESPONSE)
                .payload(SdkBytes.fromString(payloadJson, StandardCharsets.UTF_8))
                .build();

            InvokeResponse response = lambdaClient.invoke(request);

            if (response.functionError() != null) {
                log.error("Lambda function error: function={} error={}", functionName, response.functionError());
                return null;
            }

            String responseBody = response.payload().asString(StandardCharsets.UTF_8);
            log.debug("Lambda sync response: function={} statusCode={}", functionName, response.statusCode());
            return responseBody;

        } catch (Throwable ex) {
            log.error("Lambda sync invocation failed for {}: {}", functionName, ex.getMessage());
            return null;
        }
    }

    /**
     * Invokes a Lambda function asynchronously (fire-and-forget).
     * Returns immediately without waiting for function execution.
     */
    private void invokeAsynchronous(String functionName, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .invocationType(InvocationType.EVENT) // Async / fire-and-forget
                .payload(SdkBytes.fromString(payloadJson, StandardCharsets.UTF_8))
                .build();

            lambdaClient.invoke(request);
            log.debug("Lambda async invocation sent: function={}", functionName);

        } catch (Throwable ex) {
            log.error("Lambda async invocation failed for {}: {}", functionName, ex.getMessage());
            throw new RuntimeException("Lambda async invocation failed: " + functionName, ex);
        }
    }

    // ─── Inner Types ───────────────────────────────────────────────

    /**
     * Result of a Lambda compliance check invocation.
     */
    public record ComplianceResult(boolean isCompliant, String violationCode, String reason) {
        public static ComplianceResult defaultPass() {
            return new ComplianceResult(true, null, "Lambda unavailable – defaulting to pass");
        }
    }
}
