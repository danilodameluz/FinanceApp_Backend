package com.financeapp.application.service;

import com.financeapp.application.dto.CategoryDTO;
import com.financeapp.domain.entity.Category;
import com.financeapp.domain.entity.User;
import com.financeapp.domain.enums.CategoryType;
import com.financeapp.domain.repository.CategoryRepository;
import com.financeapp.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<Category> findAllByUser(Long userId) {
        return categoryRepository.findByUserId(userId);
    }

    public List<Category> findByUserAndType(Long userId, CategoryType type) {
        return categoryRepository.findByUserIdAndType(userId, type);
    }

    @Transactional
    public Category create(Long userId, CategoryDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Category category = Category.builder()
                .user(user)
                .name(dto.getName())
                .type(dto.getType())
                .icon(dto.getIcon())
                .color(dto.getColor())
                .budget(dto.getBudget())
                .build();

        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long userId, Long categoryId, CategoryDTO dto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));

        if (!category.getUser().getId().equals(userId)) {
            throw new SecurityException("Acesso negado");
        }

        category.setName(dto.getName());
        category.setType(dto.getType());
        category.setIcon(dto.getIcon());
        category.setColor(dto.getColor());
        category.setBudget(dto.getBudget());

        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));

        if (!category.getUser().getId().equals(userId)) {
            throw new SecurityException("Acesso negado");
        }

        categoryRepository.delete(category);
    }
}