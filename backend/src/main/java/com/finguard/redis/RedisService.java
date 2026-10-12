package com.finguard.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * RedisService – wrapper around RedisTemplate providing typed get/set operations.
 *
 * Design principle:
 *   Redis is ONLY a cache. PostgreSQL is the source of truth.
 *   All cache operations are wrapped in try-catch to prevent Redis failures
 *   from impacting the primary application flow.
 *
 * TTLs are configurable via application.yml under finguard.redis.ttl.*
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // ─── Write ────────────────────────────────────────────────

    /**
     * Stores a value in Redis with the given TTL.
     * Silently fails if Redis is unavailable.
     */
    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Redis SET: key={} ttl={}s", key, ttl.getSeconds());
        } catch (Throwable ex) {
            log.warn("Redis SET failed for key={}: {}", key, ex.getMessage());
        }
    }

    // ─── Read ─────────────────────────────────────────────────

    /**
     * Retrieves a value from Redis.
     * Returns empty Optional if key is missing or Redis is unavailable.
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Redis MISS: key={}", key);
                return Optional.empty();
            }
            log.debug("Redis HIT: key={}", key);
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
            log.warn("Redis type mismatch for key={}: expected {} but got {}", key, type.getName(), value.getClass().getName());
            return Optional.empty();
        } catch (Throwable ex) {
            log.warn("Redis GET failed for key={}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves a raw Object from Redis (for complex types).
     */
    public Optional<Object> get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) return Optional.empty();
            return Optional.of(value);
        } catch (Throwable ex) {
            log.warn("Redis GET failed for key={}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    // ─── Invalidate ───────────────────────────────────────────

    /**
     * Deletes a key from Redis (cache invalidation).
     */
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Redis EVICT: key={}", key);
        } catch (Throwable ex) {
            log.warn("Redis EVICT failed for key={}: {}", key, ex.getMessage());
        }
    }

    /**
     * Checks whether a key exists in Redis.
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Throwable ex) {
            log.warn("Redis EXISTS check failed for key={}: {}", key, ex.getMessage());
            return false;
        }
    }
}
