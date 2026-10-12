package com.finguard.controller;

import com.finguard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * DashboardController – aggregated statistics endpoints for the dashboard.
 *
 * GET /api/dashboard/summary           – overall summary cards
 * GET /api/dashboard/transaction-stats – transaction breakdown
 * GET /api/dashboard/fraud-stats       – fraud summary
 * GET /api/health                      – health check (public)
 */
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/dashboard/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/api/dashboard/transaction-stats")
    public ResponseEntity<Map<String, Object>> getTransactionStats() {
        return ResponseEntity.ok(dashboardService.getTransactionStats());
    }

    @GetMapping("/api/dashboard/fraud-stats")
    public ResponseEntity<Map<String, Object>> getFraudStats() {
        return ResponseEntity.ok(dashboardService.getFraudStats());
    }

    /**
     * Health check endpoint – publicly accessible.
     * Returns service name and timestamp so clients can verify connectivity.
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status",    "UP",
            "service",   "FinGuard",
            "timestamp", Instant.now().toString()
        ));
    }
}
