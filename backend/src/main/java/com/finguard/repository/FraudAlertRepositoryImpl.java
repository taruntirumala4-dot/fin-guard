package com.finguard.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA implementation of the custom FraudAlertRepository fragment.
 *
 * countBySeverityGrouped uses a JPQL GROUP BY to count alerts per severity.
 */
@Repository
public class FraudAlertRepositoryImpl implements FraudAlertRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Object[]> countBySeverityGrouped() {
        return entityManager
            .createQuery(
                "SELECT fa.severity, COUNT(fa) FROM FraudAlert fa GROUP BY fa.severity",
                Object[].class)
            .getResultList();
    }
}
