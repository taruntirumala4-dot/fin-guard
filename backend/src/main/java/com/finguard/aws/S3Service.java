package com.finguard.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * S3Service – Manages file storage in Amazon S3.
 *
 * Responsibilities:
 *  - Upload transaction reports (CSV/PDF) to S3
 *  - Store fraud alert audit trails
 *  - Generate pre-signed URLs for secure downloads
 *  - Manage backup lifecycle
 *
 * Gracefully degrades when AWS is unavailable (local dev mode).
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = false)
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.reports-prefix:reports/}")
    private String reportsPrefix;

    @Value("${aws.s3.backups-prefix:backups/}")
    private String backupsPrefix;

    @Value("${aws.s3.audit-prefix:audit/}")
    private String auditPrefix;

    @Value("${aws.s3.presigned-url-expiry-minutes:60}")
    private long presignedUrlExpiryMinutes;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    // ─── Upload ────────────────────────────────────────────────────

    /**
     * Uploads a transaction report CSV to S3.
     *
     * @param reportBytes Raw CSV/PDF bytes
     * @param fileName    e.g. "transactions-2024-01-01.csv"
     * @return S3 object key for the uploaded report
     */
    public String uploadReport(byte[] reportBytes, String fileName) {
        String key = reportsPrefix + datePrefixed(fileName);
        return uploadBytes(key, reportBytes, contentTypeFor(fileName));
    }

    /**
     * Uploads a database backup snapshot to S3.
     *
     * @param backupStream InputStream of the backup data
     * @param fileName     Backup file name
     * @param contentLength Size in bytes
     * @return S3 object key
     */
    public String uploadBackup(InputStream backupStream, String fileName, long contentLength) {
        String key = backupsPrefix + datePrefixed(fileName);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/octet-stream")
                .serverSideEncryption(ServerSideEncryption.AES256)
                .storageClass(StorageClass.STANDARD_IA) // Infrequent Access for backups
                .build();

            s3Client.putObject(request, RequestBody.fromInputStream(backupStream, contentLength));
            log.info("Backup uploaded to S3: bucket={} key={}", bucketName, key);
            return key;
        } catch (Throwable ex) {
            log.error("Failed to upload backup to S3: {}", ex.getMessage(), ex);
            throw new RuntimeException("S3 backup upload failed", ex);
        }
    }

    /**
     * Uploads a fraud audit trail JSON to S3.
     *
     * @param auditJson   JSON string of the audit record
     * @param transactionId Associated transaction ID
     * @return S3 object key
     */
    public String uploadAuditTrail(String auditJson, String transactionId) {
        String fileName = "fraud-audit-" + transactionId + ".json";
        String key = auditPrefix + datePrefixed(fileName);
        return uploadBytes(key, auditJson.getBytes(), "application/json");
    }

    // ─── Download ──────────────────────────────────────────────────

    /**
     * Generates a pre-signed URL for secure temporary access to an S3 object.
     *
     * @param objectKey S3 object key
     * @return Pre-signed URL valid for configured expiry duration
     */
    public URL generatePresignedUrl(String objectKey) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMinutes))
                .getObjectRequest(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build())
                .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            log.debug("Generated pre-signed URL for key={} expiresIn={}min", objectKey, presignedUrlExpiryMinutes);
            return presigned.url();
        } catch (Throwable ex) {
            log.error("Failed to generate pre-signed URL: {}", ex.getMessage(), ex);
            throw new RuntimeException("S3 presigned URL generation failed", ex);
        }
    }

    /**
     * Downloads an object from S3 as raw bytes.
     */
    public byte[] downloadObject(String objectKey) {
        try {
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(objectKey).build()
            );
            byte[] bytes = response.readAllBytes();
            log.debug("Downloaded S3 object: key={} size={}bytes", objectKey, bytes.length);
            return bytes;
        } catch (S3Exception ex) {
            log.error("S3 download error for key={}: {}", objectKey, ex.awsErrorDetails().errorMessage());
            throw new RuntimeException("S3 download failed: " + ex.awsErrorDetails().errorMessage(), ex);
        } catch (IOException ex) {
            log.error("IO error reading S3 object: {}", ex.getMessage());
            throw new RuntimeException("S3 read failed", ex);
        }
    }

    // ─── List ──────────────────────────────────────────────────────

    /**
     * Lists all report objects in S3 under the reports prefix.
     */
    public List<String> listReports() {
        return listObjects(reportsPrefix);
    }

    /**
     * Lists all backup objects in S3 under the backups prefix.
     */
    public List<String> listBackups() {
        return listObjects(backupsPrefix);
    }

    private List<String> listObjects(String prefix) {
        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .maxKeys(100)
                    .build()
            );
            return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
        } catch (Throwable ex) {
            log.error("Failed to list S3 objects with prefix={}: {}", prefix, ex.getMessage());
            return List.of();
        }
    }

    // ─── Delete ────────────────────────────────────────────────────

    /**
     * Deletes an S3 object. Used for cleanup operations.
     */
    public void deleteObject(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build());
            log.info("Deleted S3 object: key={}", objectKey);
        } catch (Throwable ex) {
            log.error("Failed to delete S3 object key={}: {}", objectKey, ex.getMessage());
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private String uploadBytes(String key, byte[] bytes, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength((long) bytes.length)
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();

            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            log.info("Uploaded to S3: bucket={} key={} size={}bytes", bucketName, key, bytes.length);
            return key;
        } catch (Throwable ex) {
            log.error("Failed to upload to S3 key={}: {}", key, ex.getMessage(), ex);
            throw new RuntimeException("S3 upload failed", ex);
        }
    }

    private String datePrefixed(String fileName) {
        String date = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now());
        return date + "/" + fileName;
    }

    private String contentTypeFor(String fileName) {
        if (fileName.endsWith(".csv")) return "text/csv";
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }
}
