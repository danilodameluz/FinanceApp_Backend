package com.financeapp.domain.repository;

import com.financeapp.domain.entity.Category;
import com.financeapp.domain.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserId(Long userId);
    List<Category> findByUserIdAndType(Long userId, CategoryType type);
}