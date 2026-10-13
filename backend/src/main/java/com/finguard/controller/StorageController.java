package com.finguard.controller;

import com.finguard.aws.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.*;

/**
 * StorageController – REST endpoints for interacting with AWS S3 storage.
 *
 * GET  /api/storage/status        – Check S3 connectivity and bucket access
 * GET  /api/storage/reports       – List reports stored in S3
 * POST /api/storage/upload        – Upload a report/document to S3
 * GET  /api/storage/presigned-url – Generate temporary pre-signed URL for direct download
 * GET  /api/storage/download      – Download object bytes via backend
 */
@RestController
@RequestMapping("/api/storage")
@Slf4j
public class StorageController {

    private final Optional<S3Service> s3Service;

    @Value("${aws.enabled:false}")
    private boolean awsEnabled;

    @Value("${aws.region:ap-southeast-2}")
    private String awsRegion;

    @Value("${aws.s3.bucket:${aws.s3.bucket-name:finguard-documents}}")
    private String bucketName;

    @Autowired
    public StorageController(Optional<S3Service> s3Service) {
        this.s3Service = s3Service;
    }

    /**
     * Checks S3 connection and bucket accessibility status.
     * Accessible publicly for health checks and AWS validation.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStorageStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("awsEnabled", awsEnabled);
        status.put("region", awsRegion);
        status.put("bucket", bucketName);

        if (!awsEnabled || s3Service.isEmpty()) {
            status.put("connected", false);
            status.put("message", "AWS S3 integration is disabled (aws.enabled=false). Set AWS_ENABLED=true in .env to activate.");
            return ResponseEntity.ok(status);
        }

        try {
            boolean connected = s3Service.get().testConnection();
            status.put("connected", connected);
            status.put("message", connected
                ? "S3 bucket is connected and reachable"
                : "Failed to access S3 bucket. Verify AWS credentials and bucket existence.");
            return ResponseEntity.ok(status);
        } catch (Throwable ex) {
            log.error("Error checking S3 connection: {}", ex.getMessage());
            status.put("connected", false);
            status.put("message", "Error connecting to S3: " + ex.getMessage());
            return ResponseEntity.ok(status);
        }
    }

    /**
     * Lists reports available in S3 under the configured reports prefix.
     */
    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> listReports() {
        if (s3Service.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "S3 service is not active"));
        }

        List<String> reports = s3Service.get().listReports();
        return ResponseEntity.ok(Map.of(
            "bucket", bucketName,
            "count", reports.size(),
            "reports", reports
        ));
    }

    /**
     * Uploads a file directly to the S3 bucket.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        if (s3Service.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "S3 service is not active"));
        }

        try {
            byte[] bytes = file.getBytes();
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "report.csv";
            String key = s3Service.get().uploadReport(bytes, originalName);
            URL presignedUrl = s3Service.get().generatePresignedUrl(key);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("key", key);
            response.put("bucket", bucketName);
            response.put("presignedUrl", presignedUrl.toString());
            return ResponseEntity.ok(response);
        } catch (Throwable ex) {
            log.error("Failed to upload file to S3: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Upload failed: " + ex.getMessage()));
        }
    }

    /**
     * Generates a pre-signed download URL for a given object key.
     */
    @GetMapping("/presigned-url")
    public ResponseEntity<Map<String, Object>> getPresignedUrl(@RequestParam String key) {
        if (s3Service.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "S3 service is not active"));
        }

        try {
            URL url = s3Service.get().generatePresignedUrl(key);
            return ResponseEntity.ok(Map.of(
                "key", key,
                "url", url.toString()
            ));
        } catch (Throwable ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Downloads an S3 object as raw bytes through backend.
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam String key) {
        if (s3Service.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        try {
            byte[] data = s3Service.get().downloadObject(key);
            String fileName = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
        } catch (Throwable ex) {
            log.error("Error downloading S3 object {}: {}", key, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
