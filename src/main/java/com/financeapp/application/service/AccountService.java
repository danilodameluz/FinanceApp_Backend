package com.financeapp.application.service;

import com.financeapp.application.dto.AccountDTO;
import com.financeapp.domain.entity.Account;
import com.financeapp.domain.entity.User;
import com.financeapp.domain.repository.AccountRepository;
import com.financeapp.domain.repository.UserRepository;
import com.financeapp.application.dto.InvoicePaymentDTO;
import com.financeapp.domain.enums.AccountType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.financeapp.domain.repository.TransactionRepository;
import com.financeapp.domain.repository.UserRepository;
import com.financeapp.domain.entity.Transaction;
import com.financeapp.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public List<Account> findAllByUser(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    @Transactional
    public Account create(Long userId, AccountDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Account account = Account.builder()
                .user(user)
                .name(dto.getName())
                .type(dto.getType())
                .balance(dto.getBalance())
                .build();

        return accountRepository.save(account);
    }

    @Transactional
    public Account update(Long userId, Long accountId, AccountDTO dto) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));

        if (!account.getUser().getId().equals(userId)) {
            throw new SecurityException("Acesso negado");
        }

        account.setName(dto.getName());
        account.setType(dto.getType());
        account.setBalance(dto.getBalance());

        return accountRepository.save(account);
    }

    @Transactional
    public void delete(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));

        if (!account.getUser().getId().equals(userId)) {
            throw new SecurityException("Acesso negado");
        }

        accountRepository.delete(account);
    }

    @Transactional
    public void payInvoice(Long userId, Long cardId, InvoicePaymentDTO dto) {
        Account card = accountRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Cartão não encontrado"));

        if (!card.getUser().getId().equals(userId)) {
            throw new SecurityException("Acesso negado");
        }

        if (card.getType() != AccountType.CREDIT_CARD) {
            throw new IllegalArgumentException("Conta não é um cartão de crédito");
        }

        Account debitAccount = accountRepository.findById(dto.getDebitAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Conta de débito não encontrada"));

        if (!debitAccount.getUser().getId().equals(userId)) {
            throw new SecurityException("Acesso negado");
        }

        BigDecimal invoice    = card.getInvoice() != null ? card.getInvoice() : BigDecimal.ZERO;
        BigDecimal payAmount  = dto.getAmount() != null ? dto.getAmount() : invoice;

        if (payAmount.compareTo(invoice) > 0) {
            throw new IllegalArgumentException("Valor maior que a fatura atual");
        }

        if (debitAccount.getBalance().compareTo(payAmount) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente na conta de débito");
        }

        // Atualiza saldos
        debitAccount.setBalance(debitAccount.getBalance().subtract(payAmount));
        card.setInvoice(invoice.subtract(payAmount));
        accountRepository.save(debitAccount);
        accountRepository.save(card);

        // Registra lançamento na conta debitada (despesa)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Transaction payment = Transaction.builder()
                .user(user)
                .account(debitAccount)
                .destinationAccount(card)
                .description("Pagamento fatura — " + card.getName())
                .amount(payAmount)
                .type(TransactionType.TRANSFER)
                .date(java.time.LocalDate.now())
                .build();

        transactionRepository.save(payment);
    }
}