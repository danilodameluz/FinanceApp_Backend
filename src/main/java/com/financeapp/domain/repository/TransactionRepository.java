package com.financeapp.domain.repository;

import com.financeapp.domain.entity.Transaction;
import com.financeapp.domain.enums.TransactionType;
import com.financeapp.domain.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByDateDesc(Long userId);

    List<Transaction> findByUserIdAndType(Long userId, TransactionType type);

    List<Transaction> findByUserIdAndDateBetweenOrderByDateDesc(
            Long userId, LocalDate start, LocalDate end
    );

    // Lançamentos confirmados
    List<Transaction> findByUserIdAndFutureFalseOrderByDateDesc(Long userId);

    // Lançamentos futuros pendentes
    List<Transaction> findByUserIdAndFutureTrueOrderByDateAsc(Long userId);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.id = :userId
        AND t.future = false
        AND YEAR(t.date) = :year
        AND MONTH(t.date) = :month
        ORDER BY t.date DESC
    """)
    List<Transaction> findByUserIdAndYearAndMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month
    );
}