package com.financeapp.domain.repository;

import com.financeapp.domain.entity.PluggyConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PluggyConnectionRepository extends JpaRepository<PluggyConnection, Long> {
    List<PluggyConnection> findByUserId(Long userId);
    Optional<PluggyConnection> findByItemIdAndUserId(String itemId, Long userId);
    void deleteByItemIdAndUserId(String itemId, Long userId);
}