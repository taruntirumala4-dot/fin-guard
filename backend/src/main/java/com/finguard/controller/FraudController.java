package com.finguard.controller;

import com.finguard.dto.FraudAlertResponse;
import com.finguard.model.FraudResult;
import com.finguard.service.AlertService;
import com.finguard.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FraudController – REST endpoints for fraud alert management.
 *
 * Access: ANALYST and ADMIN roles only (enforced by SecurityConfig + @PreAuthorize)
 *
 * GET  /api/fraud/alerts            – all alerts (paginated)
 * GET  /api/fraud/alerts/{id}       – single alert
 * GET  /api/fraud/statistics        – alert statistics
 * POST /api/fraud/analyze/{txnId}   – trigger manual re-analysis
 */
@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final AlertService alertService;
    private final FraudDetectionService fraudDetectionService;

    @GetMapping("/alerts")
    public ResponseEntity<Page<FraudAlertResponse>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(alertService.getAlerts(page, size));
    }

    @GetMapping("/alerts/recent")
    public ResponseEntity<List<FraudAlertResponse>> getRecentAlerts() {
        return ResponseEntity.ok(alertService.getRecentAlerts());
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<FraudAlertResponse> getAlertById(@PathVariable String id) {
        return ResponseEntity.ok(alertService.getAlertById(id));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(alertService.getStatistics());
    }

    @PostMapping("/analyze/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<FraudResult> analyzeTransaction(
            @PathVariable String transactionId) {
        FraudResult result = fraudDetectionService.analyzeTransaction(transactionId);
        return ResponseEntity.ok(result);
    }
}
