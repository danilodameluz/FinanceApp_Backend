package com.financeapp.api.controller;

import com.financeapp.application.dto.AccountDTO;
import com.financeapp.application.dto.ApiResponse;
import com.financeapp.application.dto.InvoicePaymentDTO;
import com.financeapp.application.service.AccountService;
import com.financeapp.domain.entity.Account;
import com.financeapp.domain.repository.AccountRepository;
import com.financeapp.domain.entity.User;
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
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    private Long getUserId() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"))
                .getId();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Account>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(accountService.findAllByUser(getUserId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Account>> create(@Valid @RequestBody AccountDTO dto) {
        Account account = accountService.create(getUserId(), dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Conta criada com sucesso", account));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Account>> update(
            @PathVariable Long id,
            @Valid @RequestBody AccountDTO dto) {
        Account account = accountService.update(getUserId(), id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Conta atualizada", account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        accountService.delete(getUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Conta removida", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Account>> findById(@PathVariable Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));
        return ResponseEntity.ok(ApiResponse.ok(account));
    }

    @PostMapping("/{id}/pay-invoice")
    public ResponseEntity<ApiResponse<Void>> payInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoicePaymentDTO dto) {
        accountService.payInvoice(getUserId(), id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Fatura paga com sucesso", null));
    }
}