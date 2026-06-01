package com.financeapp.api.controller;

import com.financeapp.application.dto.ApiResponse;
import com.financeapp.application.dto.TransactionDTO;
import com.financeapp.application.dto.TransactionResponseDTO;
import com.financeapp.application.service.TransactionService;
import com.financeapp.domain.entity.Transaction;
import com.financeapp.domain.enums.TransactionType;
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
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    private Long getUserId() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"))
                .getId();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> findAll(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        List<TransactionResponseDTO> transactions;

        if (year != null && month != null) {
            transactions = transactionService.findByUserAndMonth(getUserId(), year, month);
        } else if (type != null) {
            transactions = transactionService.findByUserAndType(getUserId(), type);
        } else {
            transactions = transactionService.findAllByUser(getUserId());
        }

        return ResponseEntity.ok(ApiResponse.ok(transactions));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> create(
            @Valid @RequestBody TransactionDTO dto) {
        TransactionResponseDTO transaction = transactionService.create(getUserId(), dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Lançamento criado com sucesso", transaction));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionDTO dto){
        TransactionResponseDTO transaction = transactionService.update(getUserId(), id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Categoria atualizada", transaction));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        transactionService.delete(getUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Lançamento removido", null));
    }
}