package com.financeapp.application.service;

import com.financeapp.application.dto.TransactionDTO;
import com.financeapp.application.dto.TransactionResponseDTO;
import com.financeapp.domain.entity.*;
import com.financeapp.domain.enums.AccountType;
import com.financeapp.domain.enums.TransactionStatus;
import com.financeapp.domain.enums.TransactionType;
import com.financeapp.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository     accountRepository;
    private final CategoryRepository    categoryRepository;
    private final UserRepository        userRepository;

    // ── DTO ──────────────────────────────────────────
    private TransactionResponseDTO toDTO(Transaction t) {
        return TransactionResponseDTO.builder()
                .id(t.getId())
                .description(t.getDescription())
                .amount(t.getAmount())
                .type(t.getType())
                .date(t.getDate())
                .createdAt(t.getCreatedAt())
                .future(t.isFuture())
                .status(t.getStatus().name())
                .accountId(t.getAccount() != null ? t.getAccount().getId() : null)
                .accountName(t.getAccount() != null ? t.getAccount().getName() : null)
                .categoryId(t.getCategory() != null ? t.getCategory().getId() : null)
                .categoryName(t.getCategory() != null ? t.getCategory().getName() : null)
                .categoryIcon(t.getCategory() != null ? t.getCategory().getIcon() : null)
                .categoryColor(t.getCategory() != null ? t.getCategory().getColor() : null)
                .destinationAccountId(t.getDestinationAccount() != null ? t.getDestinationAccount().getId() : null)
                .destinationAccountName(t.getDestinationAccount() != null ? t.getDestinationAccount().getName() : null)
                .build();
    }

    // ── BUSCAS ───────────────────────────────────────
    public List<TransactionResponseDTO> findAllByUser(Long userId) {
        return transactionRepository.findByUserIdAndFutureFalseOrderByDateDesc(userId)
                .stream().map(this::toDTO).toList();
    }

    public List<TransactionResponseDTO> findFutureByUser(Long userId) {
        return transactionRepository.findByUserIdAndFutureTrueOrderByDateAsc(userId)
                .stream().map(this::toDTO).toList();
    }

    public List<TransactionResponseDTO> findByUserAndMonth(Long userId, int year, int month) {
        return transactionRepository.findByUserIdAndYearAndMonth(userId, year, month)
                .stream().map(this::toDTO).toList();
    }

    public List<TransactionResponseDTO> findByUserAndType(Long userId, TransactionType type) {
        return transactionRepository.findByUserIdAndType(userId, type)
                .stream().map(this::toDTO).toList();
    }

    // ── CRIAR ────────────────────────────────────────
    @Transactional
    public TransactionResponseDTO create(Long userId, TransactionDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));

        if (!account.getUser().getId().equals(userId))
            throw new SecurityException("Acesso negado");

        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));
        }

        Account destinationAccount = null;

        // Lançamentos futuros NÃO alteram saldo
        if (!dto.isFuture()) {
            destinationAccount = applyBalanceEffect(dto, account, userId);
        } else if (dto.getType() == TransactionType.TRANSFER
                && dto.getDestinationAccountId() != null) {
            destinationAccount = accountRepository.findById(dto.getDestinationAccountId())
                    .orElseThrow(() -> new EntityNotFoundException("Conta destino não encontrada"));
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .destinationAccount(destinationAccount)
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .type(dto.getType())
                .date(dto.getDate())
                .future(dto.isFuture())
                .status(dto.isFuture() ? TransactionStatus.PENDING : TransactionStatus.CONFIRMED)
                .build();

        return toDTO(transactionRepository.save(transaction));
    }

    // ── CONFIRMAR ────────────────────────────────────
    @Transactional
    public TransactionResponseDTO confirm(Long userId, Long transactionId) {
        Transaction t = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transação não encontrada"));

        if (!t.getUser().getId().equals(userId))
            throw new SecurityException("Acesso negado");

        if (!t.isFuture() || t.getStatus() == TransactionStatus.CONFIRMED)
            throw new IllegalArgumentException("Lançamento já confirmado");

        Account account = t.getAccount();

        // Aplica o efeito no saldo agora que foi confirmado
        TransactionDTO dto = new TransactionDTO();
        dto.setType(t.getType());
        dto.setAmount(t.getAmount());
        dto.setAccountId(account.getId());
        if (t.getDestinationAccount() != null)
            dto.setDestinationAccountId(t.getDestinationAccount().getId());

        applyBalanceEffect(dto, account, userId);

        t.setFuture(false);
        t.setStatus(TransactionStatus.CONFIRMED);
        t.setDate(LocalDate.now());

        return toDTO(transactionRepository.save(t));
    }

    // ── ATUALIZAR ─────────────────────────────────────
    @Transactional
    public TransactionResponseDTO update(Long userId, Long transactionId, TransactionDTO dto) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transação não encontrada"));

        if (!transaction.getUser().getId().equals(userId))
            throw new SecurityException("Acesso negado");

        // Estorna somente se já estava confirmado
        if (!transaction.isFuture()) {
            revertBalanceEffect(transaction);
        }

        Account newAccount = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));

        if (!newAccount.getUser().getId().equals(userId))
            throw new SecurityException("Acesso negado");

        Category newCategory = null;
        if (dto.getCategoryId() != null) {
            newCategory = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));
        }

        Account newDestination = null;
        if (!dto.isFuture()) {
            newDestination = applyBalanceEffect(dto, newAccount, userId);
        } else if (dto.getType() == TransactionType.TRANSFER
                && dto.getDestinationAccountId() != null) {
            newDestination = accountRepository.findById(dto.getDestinationAccountId())
                    .orElseThrow(() -> new EntityNotFoundException("Conta destino não encontrada"));
        }

        transaction.setAccount(newAccount);
        transaction.setCategory(newCategory);
        transaction.setDestinationAccount(newDestination);
        transaction.setDescription(dto.getDescription());
        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setDate(dto.getDate());
        transaction.setFuture(dto.isFuture());
        transaction.setStatus(dto.isFuture() ? TransactionStatus.PENDING : TransactionStatus.CONFIRMED);

        return toDTO(transactionRepository.save(transaction));
    }

    // ── EXCLUIR ───────────────────────────────────────
    @Transactional
    public void delete(Long userId, Long transactionId) {
        Transaction t = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transação não encontrada"));

        if (!t.getUser().getId().equals(userId))
            throw new SecurityException("Acesso negado");

        // Estorna somente se confirmado
        if (!t.isFuture()) {
            revertBalanceEffect(t);
        }

        transactionRepository.delete(t);
    }

    // ── HELPERS ───────────────────────────────────────
    private Account applyBalanceEffect(TransactionDTO dto, Account account, Long userId) {
        Account destinationAccount = null;

        if (dto.getType() == TransactionType.TRANSFER) {
            if (dto.getDestinationAccountId() != null) {
                destinationAccount = accountRepository.findById(dto.getDestinationAccountId())
                        .orElseThrow(() -> new EntityNotFoundException("Conta destino não encontrada"));
                if (!destinationAccount.getUser().getId().equals(userId))
                    throw new SecurityException("Acesso negado à conta destino");
                account.setBalance(account.getBalance().subtract(dto.getAmount()));
                destinationAccount.setBalance(destinationAccount.getBalance().add(dto.getAmount()));
                accountRepository.save(destinationAccount);
            } else {
                account.setBalance(account.getBalance().subtract(dto.getAmount()));
            }
        } else if (account.getType() == AccountType.CREDIT_CARD) {
            if (dto.getType() == TransactionType.EXPENSE) {
                BigDecimal current = account.getInvoice() != null
                        ? account.getInvoice() : BigDecimal.ZERO;
                account.setInvoice(current.add(dto.getAmount()));
            }
        } else {
            if (dto.getType() == TransactionType.INCOME) {
                account.setBalance(account.getBalance().add(dto.getAmount()));
            } else if (dto.getType() == TransactionType.EXPENSE) {
                account.setBalance(account.getBalance().subtract(dto.getAmount()));
            }
        }

        accountRepository.save(account);
        return destinationAccount;
    }

    private void revertBalanceEffect(Transaction t) {
        Account account = t.getAccount();

        if (t.getType() == TransactionType.TRANSFER) {
            if (t.getDestinationAccount() != null) {
                account.setBalance(account.getBalance().add(t.getAmount()));
                Account dest = t.getDestinationAccount();
                if (dest.getType() == AccountType.CREDIT_CARD) {
                    BigDecimal current = dest.getInvoice() != null
                            ? dest.getInvoice() : BigDecimal.ZERO;
                    dest.setInvoice(current.add(t.getAmount()));
                } else {
                    dest.setBalance(dest.getBalance().subtract(t.getAmount()));
                }
                accountRepository.save(dest);
            } else {
                account.setBalance(account.getBalance().add(t.getAmount()));
            }
        } else if (account.getType() == AccountType.CREDIT_CARD) {
            if (t.getType() == TransactionType.EXPENSE) {
                BigDecimal current = account.getInvoice() != null
                        ? account.getInvoice() : BigDecimal.ZERO;
                account.setInvoice(current.subtract(t.getAmount()));
            }
        } else {
            if (t.getType() == TransactionType.INCOME) {
                account.setBalance(account.getBalance().subtract(t.getAmount()));
            } else if (t.getType() == TransactionType.EXPENSE) {
                account.setBalance(account.getBalance().add(t.getAmount()));
            }
        }

        accountRepository.save(account);
    }
}