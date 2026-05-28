package com.financeapp.application.service;

import com.financeapp.application.dto.AccountDTO;
import com.financeapp.domain.entity.Account;
import com.financeapp.domain.entity.User;
import com.financeapp.domain.enums.AccountType;
import com.financeapp.domain.repository.AccountRepository;
import com.financeapp.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private User user;
    private Account account;
    private AccountDTO accountDTO;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .password("$2a$10$hashedpassword")
                .build();

        account = Account.builder()
                .id(1L)
                .user(user)
                .name("Nubank")
                .type(AccountType.CHECKING)
                .balance(new BigDecimal("3200.00"))
                .build();

        accountDTO = new AccountDTO();
        accountDTO.setName("Nubank");
        accountDTO.setType(AccountType.CHECKING);
        accountDTO.setBalance(new BigDecimal("3200.00"));
    }

    @Test
    @DisplayName("Deve listar todas as contas do usuário")
    void shouldListAllAccountsByUser() {
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(account));

        List<Account> accounts = accountService.findAllByUser(1L);

        assertEquals(1, accounts.size());
        assertEquals("Nubank", accounts.get(0).getName());
        verify(accountRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("Deve criar uma conta com sucesso")
    void shouldCreateAccountSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        Account created = accountService.create(1L, accountDTO);

        assertNotNull(created);
        assertEquals("Nubank", created.getName());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar conta para usuário inexistente")
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> accountService.create(99L, accountDTO));
    }

    @Test
    @DisplayName("Deve lançar exceção ao acessar conta de outro usuário")
    void shouldThrowExceptionWhenAccessingOtherUserAccount() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(SecurityException.class,
                () -> accountService.delete(99L, 1L));
    }

    @Test
    @DisplayName("Deve excluir conta com sucesso")
    void shouldDeleteAccountSuccessfully() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        doNothing().when(accountRepository).delete(account);

        assertDoesNotThrow(() -> accountService.delete(1L, 1L));
        verify(accountRepository, times(1)).delete(account);
    }
}