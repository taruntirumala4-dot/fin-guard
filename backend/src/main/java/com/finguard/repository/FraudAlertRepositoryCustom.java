package com.finguard.repository;

import java.util.List;

/**
 * Custom query methods for FraudAlertRepository that require a JPQL
 * GROUP BY aggregation (e.g. count alerts grouped by severity).
 */
public interface FraudAlertRepositoryCustom {

    /** Returns [severity, count] pairs for all fraud alerts, grouped by severity. */
    List<Object[]> countBySeverityGrouped();
}
