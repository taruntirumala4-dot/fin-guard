package com.finguard.dto;

import com.finguard.model.FraudAlert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertResponse {

    private String id;
    private String transactionId;
    private String alertType;
    private FraudAlert.Severity severity;
    private String reason;
    private BigDecimal fraudScore;
    private FraudAlert.AlertStatus status;
    private Instant createdAt;

    public static FraudAlertResponse from(FraudAlert alert) {
        return FraudAlertResponse.builder()
            .id(String.valueOf(alert.getId()))
            .transactionId(alert.getTransactionId())
            .alertType(alert.getAlertType())
            .severity(alert.getSeverity())
            .reason(alert.getReason())
            .fraudScore(alert.getFraudScore())
            .status(alert.getStatus())
            .createdAt(alert.getCreatedAt())
            .build();
    }
}
