package com.finguard.repository;

import com.finguard.model.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudAlertRepository
        extends JpaRepository<FraudAlert, Long>, FraudAlertRepositoryCustom {

    Optional<FraudAlert> findByTransactionId(String transactionId);

    Page<FraudAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<FraudAlert> findBySeverityOrderByCreatedAtDesc(
        FraudAlert.Severity severity, Pageable pageable);

    Page<FraudAlert> findByStatusOrderByCreatedAtDesc(
        FraudAlert.AlertStatus status, Pageable pageable);

    long countBySeverity(FraudAlert.Severity severity);

    long countByStatus(FraudAlert.AlertStatus status);

    /** Recent open/investigating alerts for dashboard widget */
    List<FraudAlert> findTop5ByStatusOrderByCreatedAtDesc(FraudAlert.AlertStatus status);
}
