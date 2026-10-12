package com.finguard.fraud;

import com.finguard.model.Transaction;
import com.finguard.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudRuleEngineTest {

    @Mock
    private TransactionRepository transactionRepository;

    private FraudRuleEngine fraudRuleEngine;

    @BeforeEach
    void setUp() {
        HighAmountRule highAmountRule = new HighAmountRule();
        ReflectionTestUtils.setField(highAmountRule, "highAmountThreshold", 100000d);
        ReflectionTestUtils.setField(highAmountRule, "riskScore", 30);

        DeviceChangeRule deviceChangeRule = new DeviceChangeRule(transactionRepository);
        ReflectionTestUtils.setField(deviceChangeRule, "riskScore", 15);

        fraudRuleEngine = new FraudRuleEngine(List.of(highAmountRule, deviceChangeRule));
    }

    @Test
    @DisplayName("Should flag transaction when amount exceeds 100,000 INR")
    void testHighAmountRuleTriggers() {
        Transaction tx = Transaction.builder()
            .transactionId("TXN-TEST-001")
            .amount(new BigDecimal("250000.00"))
            .currency("INR")
            .build();

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(tx);

        assertTrue(result.triggeredRules().contains("HIGH_AMOUNT"));
        assertTrue(result.ruleScore() >= 30);
    }

    @Test
    @DisplayName("Should not flag normal transaction amount")
    void testNormalAmountDoesNotTrigger() {
        Transaction tx = Transaction.builder()
            .transactionId("TXN-TEST-002")
            .amount(new BigDecimal("1500.00"))
            .currency("INR")
            .build();

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(tx);

        assertFalse(result.triggeredRules().contains("HIGH_AMOUNT"));
    }

    @Test
    @DisplayName("Should detect device change when new device is used")
    void testDeviceChangeTriggers() {
        Transaction tx = Transaction.builder()
            .transactionId("TXN-TEST-003")
            .userId("5")
            .deviceId("DEV-NEW-UNKNOWN")
            .timestamp(Instant.now())
            .amount(new BigDecimal("5000"))
            .build();

        when(transactionRepository.findDistinctDeviceIdsByUserId("5"))
            .thenReturn(List.of("DEV-KNOWN-LAPTOP"));

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(tx);

        assertTrue(result.triggeredRules().contains("DEVICE_CHANGE"));
    }
}
