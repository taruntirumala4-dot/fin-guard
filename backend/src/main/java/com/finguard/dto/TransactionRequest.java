package com.finguard.dto;

import com.finguard.model.Transaction;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "9999999.99", message = "Amount exceeds maximum allowed")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 10, message = "Currency must be a valid code (e.g. INR, USD)")
    private String currency;

    @NotBlank(message = "Merchant is required")
    @Size(max = 255, message = "Merchant name is too long")
    private String merchant;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location is too long")
    private String location;

    @NotNull(message = "Transaction type is required")
    private Transaction.TransactionType transactionType;

    @NotBlank(message = "Device ID is required")
    @Size(max = 100, message = "Device ID is too long")
    private String deviceId;
}
