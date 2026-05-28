package com.financeapp.api.controller;

import com.financeapp.application.dto.ApiResponse;
import com.financeapp.application.dto.CategoryDTO;
import com.financeapp.application.service.CategoryService;
import com.financeapp.domain.entity.Category;
import com.financeapp.domain.enums.CategoryType;
import com.financeapp.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    private Long getUserId() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"))
                .getId();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> findAll(
            @RequestParam(required = false) CategoryType type) {
        List<Category> categories = type != null
                ? categoryService.findByUserAndType(getUserId(), type)
                : categoryService.findAllByUser(getUserId());
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> create(@Valid @RequestBody CategoryDTO dto) {
        Category category = categoryService.create(getUserId(), dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Categoria criada com sucesso", category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDTO dto) {
        Category category = categoryService.update(getUserId(), id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Categoria atualizada", category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(getUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Categoria removida", null));
    }
}