package com.finguard.aws;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CloudWatchService – Publishes custom metrics and structured logs to Amazon CloudWatch.
 *
 * Metrics Published:
 *  - Transactions processed per minute
 *  - Fraud detection rate (%)
 *  - Average fraud score
 *  - Alert severity distribution
 *  - API response time percentiles
 *  - SQS queue depths
 *  - Redis cache hit/miss ratio
 *
 * Alarms (configured in Terraform/CDK separately):
 *  - High fraud rate alarm (>10% in 5min window)
 *  - P99 latency alarm (>2000ms)
 *  - DLQ depth alarm (>100 messages)
 *
 * Gracefully degrades when CloudWatch is unavailable.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = false)
public class CloudWatchService {

    private final CloudWatchClient cloudWatchClient;
    private final MeterRegistry meterRegistry;

    @Value("${aws.cloudwatch.namespace:FinGuard/Application}")
    private String namespace;

    @Value("${aws.region:ap-south-1}")
    private String awsRegion;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    // ─── Transaction Metrics ───────────────────────────────────────

    /**
     * Records a transaction processing event with its fraud outcome.
     *
     * @param fraudScore   Final computed fraud score (0–100)
     * @param isFraudulent Whether the transaction was flagged as fraudulent
     * @param processingMs Time taken to evaluate the transaction in ms
     */
    public void recordTransactionProcessed(double fraudScore, boolean isFraudulent, long processingMs) {
        try {
            List<MetricDatum> metrics = new ArrayList<>();

            metrics.add(MetricDatum.builder()
                .metricName("TransactionsProcessed")
                .value(1.0)
                .unit(StandardUnit.COUNT)
                .timestamp(Instant.now())
                .dimensions(environmentDimension())
                .build());

            metrics.add(MetricDatum.builder()
                .metricName("FraudScore")
                .value(fraudScore)
                .unit(StandardUnit.NONE)
                .timestamp(Instant.now())
                .dimensions(environmentDimension())
                .build());

            if (isFraudulent) {
                metrics.add(MetricDatum.builder()
                    .metricName("FraudulentTransactions")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(Instant.now())
                    .dimensions(environmentDimension())
                    .build());
            }

            metrics.add(MetricDatum.builder()
                .metricName("TransactionProcessingTime")
                .value((double) processingMs)
                .unit(StandardUnit.MILLISECONDS)
                .timestamp(Instant.now())
                .dimensions(environmentDimension())
                .build());

            publishMetrics(metrics);

        } catch (Throwable ex) {
            log.error("Failed to publish transaction metrics to CloudWatch: {}", ex.getMessage());
        }
    }

    /**
     * Records a fraud alert creation with its severity.
     *
     * @param severity Alert severity: LOW, MEDIUM, HIGH, CRITICAL
     */
    public void recordFraudAlert(String severity) {
        try {
            List<MetricDatum> metrics = List.of(
                MetricDatum.builder()
                    .metricName("FraudAlertsCreated")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(Instant.now())
                    .dimensions(
                        environmentDimension()[0],
                        Dimension.builder().name("Severity").value(severity).build()
                    )
                    .build()
            );
            publishMetrics(metrics);
        } catch (Throwable ex) {
            log.error("Failed to publish fraud alert metric to CloudWatch: {}", ex.getMessage());
        }
    }

    /**
     * Records an API endpoint response time.
     *
     * @param endpoint    API path (e.g., "/api/transactions")
     * @param method      HTTP method (GET, POST, etc.)
     * @param statusCode  HTTP response status
     * @param durationMs  Request processing duration in milliseconds
     */
    public void recordApiLatency(String endpoint, String method, int statusCode, long durationMs) {
        try {
            List<MetricDatum> metrics = List.of(
                MetricDatum.builder()
                    .metricName("ApiLatency")
                    .value((double) durationMs)
                    .unit(StandardUnit.MILLISECONDS)
                    .timestamp(Instant.now())
                    .dimensions(
                        environmentDimension()[0],
                        Dimension.builder().name("Endpoint").value(sanitizeEndpoint(endpoint)).build(),
                        Dimension.builder().name("Method").value(method).build(),
                        Dimension.builder().name("StatusCode").value(String.valueOf(statusCode)).build()
                    )
                    .build()
            );
            publishMetrics(metrics);
        } catch (Throwable ex) {
            log.error("Failed to publish API latency to CloudWatch: {}", ex.getMessage());
        }
    }

    /**
     * Records ML service call performance.
     *
     * @param durationMs    ML inference duration in ms
     * @param mlScore       The ML-predicted fraud score
     * @param wasTimeout    Whether the call timed out (fallback was used)
     */
    public void recordMlServiceCall(long durationMs, double mlScore, boolean wasTimeout) {
        try {
            List<MetricDatum> metrics = new ArrayList<>();

            metrics.add(MetricDatum.builder()
                .metricName("MLServiceLatency")
                .value((double) durationMs)
                .unit(StandardUnit.MILLISECONDS)
                .timestamp(Instant.now())
                .dimensions(environmentDimension())
                .build());

            if (wasTimeout) {
                metrics.add(MetricDatum.builder()
                    .metricName("MLServiceTimeouts")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(Instant.now())
                    .dimensions(environmentDimension())
                    .build());
            }

            publishMetrics(metrics);
        } catch (Throwable ex) {
            log.error("Failed to publish ML service metrics to CloudWatch: {}", ex.getMessage());
        }
    }

    /**
     * Records Redis cache hit or miss for monitoring cache efficiency.
     *
     * @param cacheKey  Cache key name (sanitized for grouping)
     * @param isHit     true = cache hit, false = cache miss
     */
    public void recordCacheEvent(String cacheKey, boolean isHit) {
        try {
            String metricName = isHit ? "CacheHits" : "CacheMisses";
            List<MetricDatum> metrics = List.of(
                MetricDatum.builder()
                    .metricName(metricName)
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(Instant.now())
                    .dimensions(environmentDimension())
                    .build()
            );
            publishMetrics(metrics);
        } catch (Throwable ex) {
            log.error("Failed to publish cache metric to CloudWatch: {}", ex.getMessage());
        }
    }

    // ─── Scheduled Batch Metrics ───────────────────────────────────

    /**
     * Publishes a batch of aggregate metrics every minute via Micrometer gauges.
     * Scheduled to run every 60 seconds.
     */
    @Scheduled(fixedRateString = "${aws.cloudwatch.metrics-interval-ms:60000}")
    public void publishAggregateMetrics() {
        try {
            // Push Micrometer registry metrics to CloudWatch
            double jvmMemoryUsed = getGaugeValue("jvm.memory.used");
            double jvmMemoryMax  = getGaugeValue("jvm.memory.max");

            if (jvmMemoryMax > 0) {
                double memoryUsagePct = (jvmMemoryUsed / jvmMemoryMax) * 100;

                List<MetricDatum> metrics = List.of(
                    MetricDatum.builder()
                        .metricName("JVMMemoryUsagePct")
                        .value(memoryUsagePct)
                        .unit(StandardUnit.PERCENT)
                        .timestamp(Instant.now())
                        .dimensions(environmentDimension())
                        .build()
                );
                publishMetrics(metrics);
                log.debug("Published aggregate CloudWatch metrics: memoryUsage={}%", String.format("%.1f", memoryUsagePct));
            }
        } catch (Throwable ex) {
            log.error("Failed to publish aggregate metrics to CloudWatch: {}", ex.getMessage());
        }
    }

    // ─── Alarm Helpers ─────────────────────────────────────────────

    /**
     * Retrieves a list of CloudWatch alarms in ALARM state.
     * Used by health endpoints.
     */
    public List<String> getActiveAlarms() {
        try {
            DescribeAlarmsResponse response = cloudWatchClient.describeAlarms(
                DescribeAlarmsRequest.builder()
                    .stateValue(StateValue.ALARM)
                    .alarmNamePrefix("FinGuard-")
                    .build()
            );

            return response.metricAlarms().stream()
                .map(MetricAlarm::alarmName)
                .toList();

        } catch (Throwable ex) {
            log.error("Failed to fetch CloudWatch alarms: {}", ex.getMessage());
            return List.of();
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private void publishMetrics(List<MetricDatum> metrics) {
        if (metrics.isEmpty()) return;

        // CloudWatch allows max 20 metrics per PutMetricData call
        for (int i = 0; i < metrics.size(); i += 20) {
            List<MetricDatum> batch = metrics.subList(i, Math.min(i + 20, metrics.size()));
            cloudWatchClient.putMetricData(PutMetricDataRequest.builder()
                .namespace(namespace)
                .metricData(batch)
                .build());
        }
    }

    private Dimension[] environmentDimension() {
        return new Dimension[]{
            Dimension.builder().name("Environment").value(activeProfile).build(),
            Dimension.builder().name("Region").value(awsRegion).build()
        };
    }

    private String sanitizeEndpoint(String endpoint) {
        // Replace path variable segments with placeholders for metric grouping
        return endpoint.replaceAll("/[0-9a-fA-F-]{8,}", "/{id}");
    }

    private double getGaugeValue(String metricName) {
        try {
            var gauge = meterRegistry.find(metricName).gauge();
            return gauge != null ? gauge.value() : 0.0;
        } catch (Throwable ex) {
            return 0.0;
        }
    }
}
