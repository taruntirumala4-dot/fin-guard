package com.finguard.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * ValidationUtil – reusable validation helpers.
 *
 * Centralizes validation logic to avoid duplication across services.
 */
@Component
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern DEVICE_ID_PATTERN =
        Pattern.compile("^[A-Za-z0-9_\\-]{1,100}$");

    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public boolean isValidDeviceId(String deviceId) {
        return deviceId != null && DEVICE_ID_PATTERN.matcher(deviceId.trim()).matches();
    }

    public boolean isValidCurrency(String currency) {
        return currency != null && currency.trim().length() >= 3 &&
               currency.trim().length() <= 10 &&
               currency.trim().matches("[A-Z]{3,10}");
    }
}
