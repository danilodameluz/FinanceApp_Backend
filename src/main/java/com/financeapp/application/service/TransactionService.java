package com.financeapp.application.service;

import com.financeapp.application.dto.TransactionDTO;
import com.financeapp.application.dto.TransactionResponseDTO;
import com.financeapp.domain.entity.*;
import com.financeapp.domain.enums.TransactionType;
import com.financeapp.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import com.financeapp.domain.enums.AccountType;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // Converte entidade → DTO de resposta
    private TransactionResponseDTO toDTO(Transaction t) {
        return TransactionResponseDTO.builder()
                .id(t.getId())
                .description(t.getDescription())
                .amount(t.getAmount())
                .type(t.getType())
                .date(t.getDate())
                .createdAt(t.getCreatedAt())
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

    public List<TransactionResponseDTO> findAllByUser(Long userId) {
        return transactionRepository.findByUserIdOrderByDateDesc(userId)
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

    @Transactional
    public TransactionResponseDTO create(Long userId, TransactionDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));

        if (!account.getUser().getId().equals(userId)) {
            throw new SecurityException("Acesso negado");
        }

        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));
        }

        Account destinationAccount = null;

        if (dto.getType() == TransactionType.TRANSFER) {

            if (dto.getDestinationAccountId() != null) {
                // Transferência entre contas próprias
                destinationAccount = accountRepository.findById(dto.getDestinationAccountId())
                        .orElseThrow(() -> new EntityNotFoundException("Conta destino não encontrada"));

                if (!destinationAccount.getUser().getId().equals(userId)) {
                    throw new SecurityException("Acesso negado à conta destino");
                }

                // Debita da conta origem
                account.setBalance(account.getBalance().subtract(dto.getAmount()));

                // Credita na conta destino
                destinationAccount.setBalance(destinationAccount.getBalance().add(dto.getAmount()));
                accountRepository.save(destinationAccount);

            } else {
                // Transferência para terceiro — trata como despesa no saldo
                account.setBalance(account.getBalance().subtract(dto.getAmount()));
            }

        } else if (account.getType() == AccountType.CREDIT_CARD) {
            // Cartão de crédito: acumula na fatura
            if (dto.getType() == TransactionType.EXPENSE) {
                BigDecimal current = account.getInvoice() != null
                        ? account.getInvoice() : BigDecimal.ZERO;
                account.setInvoice(current.add(dto.getAmount()));
            }
        } else {
            // Conta normal
            if (dto.getType() == TransactionType.INCOME) {
                account.setBalance(account.getBalance().add(dto.getAmount()));
            } else if (dto.getType() == TransactionType.EXPENSE) {
                account.setBalance(account.getBalance().subtract(dto.getAmount()));
            }
        }

        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .destinationAccount(destinationAccount)
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .type(dto.getType())
                .date(dto.getDate())
                .build();

        return toDTO(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponseDTO update(Long userId, Long transactionId, TransactionDTO dto) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transação não encontrada"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new SecurityException("Acesso negado");
        }

        Account oldAccount = transaction.getAccount();

        // Estorna o efeito do lançamento antigo no saldo
        if (transaction.getType() == TransactionType.TRANSFER) {
            if (transaction.getDestinationAccount() != null) {
                oldAccount.setBalance(oldAccount.getBalance().add(transaction.getAmount()));
                Account oldDest = transaction.getDestinationAccount();
                oldDest.setBalance(oldDest.getBalance().subtract(transaction.getAmount()));
                accountRepository.save(oldDest);
            } else {
                oldAccount.setBalance(oldAccount.getBalance().add(transaction.getAmount()));
            }
        } else if (oldAccount.getType() == AccountType.CREDIT_CARD) {
            if (transaction.getType() == TransactionType.EXPENSE) {
                oldAccount.setInvoice(oldAccount.getInvoice().subtract(transaction.getAmount()));
            }
        } else {
            if (transaction.getType() == TransactionType.INCOME) {
                oldAccount.setBalance(oldAccount.getBalance().subtract(transaction.getAmount()));
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                oldAccount.setBalance(oldAccount.getBalance().add(transaction.getAmount()));
            }
        }
        accountRepository.save(oldAccount);

        // Aplica o novo lançamento
        Account newAccount = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));

        if (!newAccount.getUser().getId().equals(userId)) {
            throw new SecurityException("Acesso negado");
        }

        Category newCategory = null;
        if (dto.getCategoryId() != null) {
            newCategory = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));
        }

        Account newDestination = null;
        if (dto.getType() == TransactionType.TRANSFER) {
            if (dto.getDestinationAccountId() != null) {
                newDestination = accountRepository.findById(dto.getDestinationAccountId())
                        .orElseThrow(() -> new EntityNotFoundException("Conta destino não encontrada"));
                newAccount.setBalance(newAccount.getBalance().subtract(dto.getAmount()));
                newDestination.setBalance(newDestination.getBalance().add(dto.getAmount()));
                accountRepository.save(newDestination);
            } else {
                newAccount.setBalance(newAccount.getBalance().subtract(dto.getAmount()));
            }
        } else if (newAccount.getType() == AccountType.CREDIT_CARD) {
            if (dto.getType() == TransactionType.EXPENSE) {
                BigDecimal current = newAccount.getInvoice() != null
                        ? newAccount.getInvoice() : BigDecimal.ZERO;
                newAccount.setInvoice(current.add(dto.getAmount()));
            }
        } else {
            if (dto.getType() == TransactionType.INCOME) {
                newAccount.setBalance(newAccount.getBalance().add(dto.getAmount()));
            } else if (dto.getType() == TransactionType.EXPENSE) {
                newAccount.setBalance(newAccount.getBalance().subtract(dto.getAmount()));
            }
        }
        accountRepository.save(newAccount);

        // Atualiza os campos da transação
        transaction.setAccount(newAccount);
        transaction.setCategory(newCategory);
        transaction.setDestinationAccount(newDestination);
        transaction.setDescription(dto.getDescription());
        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setDate(dto.getDate());

        return toDTO(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transação não encontrada"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new SecurityException("Acesso negado");
        }

        Account account = transaction.getAccount();

        if (transaction.getType() == TransactionType.TRANSFER) {
            if (transaction.getDestinationAccount() != null) {
                // Estorna transferência entre contas
                account.setBalance(account.getBalance().add(transaction.getAmount()));
                Account dest = transaction.getDestinationAccount();
                dest.setBalance(dest.getBalance().subtract(transaction.getAmount()));
                accountRepository.save(dest);
            } else {
                // Estorna transferência para terceiro
                account.setBalance(account.getBalance().add(transaction.getAmount()));
            }
        } else if (account.getType() == AccountType.CREDIT_CARD) {
            if (transaction.getType() == TransactionType.EXPENSE) {
                BigDecimal current = account.getInvoice() != null
                        ? account.getInvoice() : BigDecimal.ZERO;
                account.setInvoice(current.subtract(transaction.getAmount()));
            }
        } else {
            if (transaction.getType() == TransactionType.INCOME) {
                account.setBalance(account.getBalance().subtract(transaction.getAmount()));
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                account.setBalance(account.getBalance().add(transaction.getAmount()));
            }
        }

        accountRepository.save(account);
        transactionRepository.delete(transaction);
    }
}