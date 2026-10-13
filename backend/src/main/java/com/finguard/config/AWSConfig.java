package com.finguard.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * AWSConfig – Configures and exposes all AWS SDK v2 clients as Spring beans.
 *
 * Services configured:
 *  ┌─────────────────┬─────────────────────────────────────────────────┐
 *  │ Service         │ Bean                                             │
 *  ├─────────────────┼─────────────────────────────────────────────────┤
 *  │ S3              │ S3Client, S3Presigner                            │
 *  │ SQS             │ SqsClient                                        │
 *  │ SNS             │ SnsClient                                        │
 *  │ Lambda          │ LambdaClient                                     │
 *  │ CloudWatch      │ CloudWatchClient                                 │
 *  │ KMS             │ KmsClient                                        │
 *  └─────────────────┴─────────────────────────────────────────────────┘
 *
 * Credentials Resolution Order (AWS SDK Default Chain):
 *  1. Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
 *  2. Java system properties
 *  3. AWS credentials file (~/.aws/credentials)
 *  4. ECS task role / EC2 instance profile (preferred in production)
 *  5. IAM role (no static credentials needed on EC2)
 *
 * Only enabled when aws.enabled=true (not loaded in local dev profile).
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = false)
public class AWSConfig {

    @Value("${aws.region:ap-south-1}")
    private String awsRegion;

    @Value("${aws.access-key-id:#{null}}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:#{null}}")
    private String secretAccessKey;

    // ─── Credentials ───────────────────────────────────────────────

    /**
     * Resolves AWS credentials.
     * - In production (EC2/ECS): uses IAM role via DefaultCredentialsProvider (no static keys)
     * - In dev/CI: uses static credentials from environment variables if provided
     */
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (accessKeyId != null && !accessKeyId.isBlank()
                && secretAccessKey != null && !secretAccessKey.isBlank()) {
            log.warn("Using static AWS credentials – prefer IAM roles for EC2/ECS production deployments");
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            );
        }
        log.info("Using AWS DefaultCredentialsProvider (env vars / IAM role / EC2 instance profile)");
        return DefaultCredentialsProvider.create();
    }

    // ─── S3 ────────────────────────────────────────────────────────

    /**
     * S3Client for object storage operations (upload, download, delete, list).
     */
    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
        return S3Client.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider)
            .build();
    }

    /**
     * S3Presigner for generating pre-signed URLs for temporary, secure object access.
     */
    @Bean
    public S3Presigner s3Presigner(AwsCredentialsProvider credentialsProvider) {
        return S3Presigner.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider)
            .build();
    }

    // ─── SQS ───────────────────────────────────────────────────────

    /**
     * SqsClient for queue operations (send, receive, delete messages).
     */
    @Bean
    public SqsClient sqsClient(AwsCredentialsProvider credentialsProvider) {
        return SqsClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider)
            .build();
    }

    // ─── SNS ───────────────────────────────────────────────────────

    /**
     * SnsClient for publish/subscribe notifications (fraud alerts, system events).
     */
    @Bean
    public SnsClient snsClient(AwsCredentialsProvider credentialsProvider) {
        return SnsClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider)
            .build();
    }

    // ─── Lambda ────────────────────────────────────────────────────

    /**
     * LambdaClient for invoking serverless functions (fraud enrichment, reports, compliance).
     */
    @Bean
    public LambdaClient lambdaClient(AwsCredentialsProvider credentialsProvider) {
        return LambdaClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider)
            .build();
    }

    // ─── CloudWatch ────────────────────────────────────────────────

    /**
     * CloudWatchClient for publishing custom metrics and querying alarms.
     */
    @Bean
    public CloudWatchClient cloudWatchClient(AwsCredentialsProvider credentialsProvider) {
        return CloudWatchClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider)
            .build();
    }

    // ─── KMS ───────────────────────────────────────────────────────

    /**
     * KmsClient for field-level encryption/decryption and DEK generation.
     */
    @Bean
    public KmsClient kmsClient(AwsCredentialsProvider credentialsProvider) {
        return KmsClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider)
            .build();
    }
}
