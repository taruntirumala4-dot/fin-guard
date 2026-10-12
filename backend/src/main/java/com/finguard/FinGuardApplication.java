package com.finguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * FinGuard – Real-Time Financial Transaction Monitoring & Fraud Detection System
 *
 * Entry point for the Spring Boot application.
 * - @EnableCaching: activates Spring's cache abstraction (backed by Redis)
 * - @EnableAsync:   allows @Async methods for non-blocking operations
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class FinGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinGuardApplication.class, args);
    }
}
