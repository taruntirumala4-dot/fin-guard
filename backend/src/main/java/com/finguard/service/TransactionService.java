package com.finguard.service;

import com.finguard.dto.TransactionRequest;
import com.finguard.dto.TransactionResponse;
import com.finguard.exception.ResourceNotFoundException;
import com.finguard.kafka.TransactionEvent;
import com.finguard.kafka.TransactionProducer;
import com.finguard.model.Transaction;
import com.finguard.model.User;
import com.finguard.redis.RedisKeyGenerator;
import com.finguard.redis.RedisService;
import com.finguard.repository.TransactionRepository;
import com.finguard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * TransactionService – handles transaction creation and retrieval.
 *
 * Flow for new transaction:
 *   1. Validate user exists
 *   2. Generate unique transaction ID
 *   3. Persist with PENDING status
 *   4. Publish to Kafka (asynchronous fraud processing follows)
 *   5. Return response with initial state
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionProducer transactionProducer;
    private final RedisService redisService;
    private final FraudDetectionService fraudDetectionService;

    @Value("${finguard.redis.ttl.transaction-seconds:1800}")
    private long transactionTtlSeconds;

    @Value("${finguard.redis.ttl.user-transactions-seconds:600}")
    private long userTransactionsTtlSeconds;

    // ─── Create ───────────────────────────────────────────────

    public TransactionResponse createTransaction(TransactionRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        String transactionId = generateTransactionId();

        Transaction transaction = Transaction.builder()
            .transactionId(transactionId)
            .userId(user.getIdString())
            .userName(user.getName())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .merchant(request.getMerchant())
            .location(request.getLocation())
            .transactionType(request.getTransactionType())
            .deviceId(request.getDeviceId())
            .timestamp(Instant.now())
            .status(Transaction.TransactionStatus.PENDING)
            .fraudScore(BigDecimal.ZERO)
            .fraudStatus(Transaction.FraudStatus.SAFE)
            .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created: id={} user={} amount={}",
            transactionId, userEmail, request.getAmount());

        // Publish to Kafka for async fraud detection
        TransactionEvent event = TransactionEvent.builder()
            .transactionId(saved.getTransactionId())
            .userId(user.getIdString())
            .amount(saved.getAmount())
            .currency(saved.getCurrency())
            .merchant(saved.getMerchant())
            .location(saved.getLocation())
            .transactionType(saved.getTransactionType())
            .deviceId(saved.getDeviceId())
            .timestamp(saved.getTimestamp())
            .build();

        transactionProducer.publishTransaction(event);

        // Evaluate fraud rules and ML score synchronously for instant feedback
        try {
            fraudDetectionService.processTransaction(event);
        } catch (Exception ex) {
            log.warn("Inline fraud detection notice: {}", ex.getMessage());
        }

        // Invalidate user transaction cache
        redisService.evict(RedisKeyGenerator.userTransactionsKey(user.getIdString()));
        redisService.evict(RedisKeyGenerator.dashboardStatsKey());

        Transaction result = transactionRepository.findByTransactionId(transactionId).orElse(saved);
        return TransactionResponse.from(result);
    }

    // ─── Read ─────────────────────────────────────────────────

    public Page<TransactionResponse> getAllTransactions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return transactionRepository.findAllByOrderByTimestampDesc(pageable)
            .map(TransactionResponse::from);
    }

    public TransactionResponse getTransactionById(String id) {
        Transaction t = transactionRepository.findById(Long.valueOf(id))
            .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        return TransactionResponse.from(t);
    }

    public TransactionResponse getTransactionByTransactionId(String transactionId) {
        // Try Redis cache first
        Optional<TransactionResponse> cached = tryCacheGet(
            RedisKeyGenerator.transactionKey(transactionId));
        if (cached.isPresent()) return cached.get();

        Transaction t = transactionRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction", "transactionId", transactionId));

        TransactionResponse response = TransactionResponse.from(t);

        // Cache the result
        redisService.set(RedisKeyGenerator.transactionKey(transactionId),
            response, Duration.ofSeconds(transactionTtlSeconds));

        return response;
    }

    public Page<TransactionResponse> getUserTransactions(String userId, int page, int size) {
        userRepository.findById(Long.valueOf(userId))
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId, pageable)
            .map(TransactionResponse::from);
    }

    public Page<TransactionResponse> searchTransactions(
            String userId, String fraudStatus, String status,
            BigDecimal minAmount, BigDecimal maxAmount,
            String location, String merchant,
            String since, String until,
            int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

        Transaction.FraudStatus fs = fraudStatus != null ?
            Transaction.FraudStatus.valueOf(fraudStatus.toUpperCase()) : null;
        Transaction.TransactionStatus ts = status != null ?
            Transaction.TransactionStatus.valueOf(status.toUpperCase()) : null;
        Instant sinceInstant = since != null ? Instant.parse(since) : null;
        Instant untilInstant = until != null ? Instant.parse(until) : null;

        return transactionRepository.searchTransactions(
            userId, fs, ts, minAmount, maxAmount, location, merchant,
            sinceInstant, untilInstant, pageable
        ).map(TransactionResponse::from);
    }

    // ─── Helpers ──────────────────────────────────────────────

    private String generateTransactionId() {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(java.time.ZoneOffset.UTC)
            .format(Instant.now());
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "TXN-" + date + "-" + suffix;
    }

    @SuppressWarnings("unchecked")
    private <T> java.util.Optional<T> tryCacheGet(String key) {
        return (java.util.Optional<T>) (java.util.Optional<?>) redisService.get(key, Object.class);
    }
}
