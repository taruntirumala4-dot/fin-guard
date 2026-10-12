package com.finguard.repository;

import com.finguard.model.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA implementation of the custom TransactionRepository fragment.
 *
 * - searchTransactions builds a dynamic Criteria predicate chain for optional filters.
 * - sumAllAmounts uses a JPQL aggregate to sum the amount column.
 */
@Repository
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Transaction> searchTransactions(
            String userId,
            Transaction.FraudStatus fraudStatus,
            Transaction.TransactionStatus status,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String location,
            String merchant,
            Instant since,
            Instant until,
            Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Transaction> query = cb.createQuery(Transaction.class);
        Root<Transaction> root = query.from(Transaction.class);
        List<Predicate> predicates = buildPredicates(cb, root,
            userId, fraudStatus, status, minAmount, maxAmount, location, merchant, since, until);
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        query.orderBy(cb.desc(root.get("timestamp")));

        List<Transaction> content = entityManager.createQuery(query)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Transaction> countRoot = countQuery.from(Transaction.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot,
            userId, fraudStatus, status, minAmount, maxAmount, location, merchant, since, until);
        countQuery.select(cb.count(countRoot));
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private List<Predicate> buildPredicates(
            CriteriaBuilder cb, Root<Transaction> root,
            String userId,
            Transaction.FraudStatus fraudStatus,
            Transaction.TransactionStatus status,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String location,
            String merchant,
            Instant since,
            Instant until) {

        List<Predicate> predicates = new ArrayList<>();
        if (userId != null && !userId.isBlank()) {
            predicates.add(cb.equal(cb.lower(root.get("userId")), userId.toLowerCase()));
        }
        if (fraudStatus != null) {
            predicates.add(cb.equal(root.get("fraudStatus"), fraudStatus));
        }
        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (minAmount != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
        }
        if (maxAmount != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
        }
        if (location != null && !location.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("location")),
                "%" + escapeSqlLike(location.toLowerCase()) + "%"));
        }
        if (merchant != null && !merchant.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("merchant")),
                "%" + escapeSqlLike(merchant.toLowerCase()) + "%"));
        }
        if (since != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), since));
        }
        if (until != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), until));
        }
        return predicates;
    }

    @Override
    public BigDecimal sumAllAmounts() {
        BigDecimal result = entityManager
            .createQuery("SELECT SUM(t.amount) FROM Transaction t", BigDecimal.class)
            .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Escapes SQL LIKE wildcard characters in a search term.
     * Prevents injection of %, _, and \ into LIKE patterns.
     */
    private String escapeSqlLike(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
