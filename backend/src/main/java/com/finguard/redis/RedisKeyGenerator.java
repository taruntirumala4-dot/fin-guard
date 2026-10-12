package com.finguard.redis;

import org.springframework.stereotype.Component;

/**
 * RedisKeyGenerator – centralizes all Redis key patterns.
 *
 * Prevents typos and ensures consistent key naming across the application.
 * Using a static utility class keeps key definitions in a single place.
 */
@Component
public class RedisKeyGenerator {

    // ─── Key Patterns ─────────────────────────────────────────

    /**
     * Single transaction cache: transaction:{transactionId}
     */
    public static String transactionKey(String transactionId) {
        return "transaction:" + transactionId;
    }

    /**
     * User's transaction list cache: user:{userId}:transactions
     */
    public static String userTransactionsKey(String userId) {
        return "user:" + userId + ":transactions";
    }

    /**
     * Dashboard statistics cache: dashboard:statistics
     */
    public static String dashboardStatsKey() {
        return "dashboard:statistics";
    }

    /**
     * Recent fraud alerts cache: fraud:alerts:recent
     */
    public static String recentFraudAlertsKey() {
        return "fraud:alerts:recent";
    }

    /**
     * User info cache: user:{userId}:info
     */
    public static String userInfoKey(String userId) {
        return "user:" + userId + ":info";
    }
}
