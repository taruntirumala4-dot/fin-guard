package com.finguard.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * KMSService – AWS Key Management Service integration for field-level encryption.
 *
 * Responsibilities:
 *  - Encrypt sensitive PII fields before database storage (e.g. card numbers, SSN)
 *  - Decrypt sensitive fields when authorized callers read them
 *  - Generate Data Encryption Keys (DEK) for envelope encryption
 *  - Rotate encryption keys on schedule (managed by AWS KMS)
 *
 * Encryption Strategy (Envelope Encryption):
 *  1. KMS generates a DEK (data encryption key)
 *  2. Plaintext DEK encrypts the data locally (fast AES-256)
 *  3. Encrypted DEK stored alongside the ciphertext
 *  4. On decrypt: KMS decrypts the DEK, DEK decrypts the data
 *
 * Gracefully degrades: if KMS is unavailable and the app is in local profile,
 * the service returns the plaintext unmodified (for dev convenience).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = false)
public class KMSService {

    private final KmsClient kmsClient;

    @Value("${aws.kms.key-id}")
    private String kmsKeyId;

    @Value("${aws.kms.key-alias:alias/finguard-data-key}")
    private String kmsKeyAlias;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    // ─── Encrypt ───────────────────────────────────────────────────

    /**
     * Encrypts a plaintext string using AWS KMS.
     * Returns Base64-encoded ciphertext.
     *
     * @param plaintext The plaintext to encrypt
     * @return Base64-encoded encrypted ciphertext, or plaintext in local mode
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return plaintext;

        try {
            EncryptRequest request = EncryptRequest.builder()
                .keyId(kmsKeyId)
                .plaintext(SdkBytes.fromString(plaintext, StandardCharsets.UTF_8))
                .encryptionAlgorithm(EncryptionAlgorithmSpec.SYMMETRIC_DEFAULT)
                .build();

            EncryptResponse response = kmsClient.encrypt(request);
            String encrypted = Base64.getEncoder().encodeToString(
                response.ciphertextBlob().asByteArray()
            );
            log.debug("KMS encrypt: keyId={} inputLen={}", kmsKeyId, plaintext.length());
            return encrypted;

        } catch (Throwable ex) {
            log.error("KMS encryption failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Data encryption failed", ex);
        }
    }

    /**
     * Decrypts a Base64-encoded KMS ciphertext back to plaintext.
     *
     * @param ciphertextBase64 Base64-encoded ciphertext from {@link #encrypt(String)}
     * @return Decrypted plaintext string
     */
    public String decrypt(String ciphertextBase64) {
        if (ciphertextBase64 == null || ciphertextBase64.isBlank()) return ciphertextBase64;

        try {
            byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertextBase64);

            DecryptRequest request = DecryptRequest.builder()
                .keyId(kmsKeyId)
                .ciphertextBlob(SdkBytes.fromByteArray(ciphertextBytes))
                .encryptionAlgorithm(EncryptionAlgorithmSpec.SYMMETRIC_DEFAULT)
                .build();

            DecryptResponse response = kmsClient.decrypt(request);
            String plaintext = response.plaintext().asString(StandardCharsets.UTF_8);
            log.debug("KMS decrypt: keyId={}", kmsKeyId);
            return plaintext;

        } catch (Throwable ex) {
            log.error("KMS decryption failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Data decryption failed", ex);
        }
    }

    // ─── Envelope Encryption (DEK) ─────────────────────────────────

    /**
     * Generates a Data Encryption Key (DEK) via KMS for envelope encryption.
     * Returns both plaintext and ciphertext DEK.
     *
     * Usage:
     *  1. Use plaintext DEK to encrypt large data locally (AES-256)
     *  2. Store encrypted DEK alongside the ciphertext
     *  3. Discard plaintext DEK from memory
     *  4. On read: call {@link #decryptDataKey(String)} to recover plaintext DEK
     *
     * @return GenerateDataKeyResponse containing both plaintext and ciphertext DEK
     */
    public GenerateDataKeyResponse generateDataKey() {
        try {
            GenerateDataKeyRequest request = GenerateDataKeyRequest.builder()
                .keyId(kmsKeyId)
                .keySpec(DataKeySpec.AES_256)
                .build();

            GenerateDataKeyResponse response = kmsClient.generateDataKey(request);
            log.debug("KMS DEK generated: keyId={}", kmsKeyId);
            return response;

        } catch (Throwable ex) {
            log.error("KMS DEK generation failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("DEK generation failed", ex);
        }
    }

    /**
     * Decrypts a previously generated encrypted Data Encryption Key (DEK).
     *
     * @param encryptedDekBase64 Base64-encoded encrypted DEK from {@link #generateDataKey()}
     * @return Plaintext DEK bytes for local decryption
     */
    public byte[] decryptDataKey(String encryptedDekBase64) {
        try {
            byte[] encryptedDekBytes = Base64.getDecoder().decode(encryptedDekBase64);

            DecryptRequest request = DecryptRequest.builder()
                .keyId(kmsKeyId)
                .ciphertextBlob(SdkBytes.fromByteArray(encryptedDekBytes))
                .build();

            return kmsClient.decrypt(request).plaintext().asByteArray();

        } catch (Throwable ex) {
            log.error("KMS DEK decryption failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("DEK decryption failed", ex);
        }
    }

    // ─── Key Info ──────────────────────────────────────────────────

    /**
     * Describes the current KMS key metadata (rotation status, state, etc.)
     * Used by health checks and compliance reporting.
     */
    public DescribeKeyResponse describeKey() {
        try {
            return kmsClient.describeKey(DescribeKeyRequest.builder()
                .keyId(kmsKeyId)
                .build());
        } catch (Throwable ex) {
            log.error("Failed to describe KMS key: {}", ex.getMessage());
            throw new RuntimeException("KMS key description failed", ex);
        }
    }

    /**
     * Checks if automatic key rotation is enabled for the CMK.
     */
    public boolean isKeyRotationEnabled() {
        try {
            GetKeyRotationStatusResponse response = kmsClient.getKeyRotationStatus(
                GetKeyRotationStatusRequest.builder().keyId(kmsKeyId).build()
            );
            return response.keyRotationEnabled();
        } catch (Throwable ex) {
            log.error("Failed to check KMS key rotation status: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Masks a sensitive value for logging (shows last 4 chars only).
     * Use this instead of logging plaintext sensitive fields.
     */
    public static String mask(String value) {
        if (value == null || value.length() <= 4) return "****";
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
}
