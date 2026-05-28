package com.financeapp.application.service;

import com.financeapp.api.config.JwtService;
import com.financeapp.application.dto.AuthResponseDTO;
import com.financeapp.application.dto.LoginDTO;
import com.financeapp.application.dto.RegisterDTO;
import com.financeapp.domain.entity.User;
import com.financeapp.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterDTO registerDTO;
    private LoginDTO loginDTO;
    private User user;

    @BeforeEach
    void setUp() {
        registerDTO = new RegisterDTO();
        registerDTO.setName("João Silva");
        registerDTO.setEmail("joao@email.com");
        registerDTO.setPassword("123456");

        loginDTO = new LoginDTO();
        loginDTO.setEmail("joao@email.com");
        loginDTO.setPassword("123456");

        user = User.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .password("$2a$10$hashedpassword")
                .build();
    }

    @Test
    @DisplayName("Deve registrar um novo usuário com sucesso")
    void shouldRegisterUserSuccessfully() {
        when(userRepository.existsByEmail(registerDTO.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerDTO.getPassword())).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(), any())).thenReturn("token123");

        AuthResponseDTO response = authService.register(registerDTO);

        assertNotNull(response);
        assertEquals("token123", response.getToken());
        assertEquals("João Silva", response.getName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao registrar email já existente")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(registerDTO.getEmail())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(registerDTO));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve realizar login com sucesso")
    void shouldLoginSuccessfully() {
        when(userRepository.findByEmail(loginDTO.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user.getId(), user.getEmail())).thenReturn("token123");

        AuthResponseDTO response = authService.login(loginDTO);

        assertNotNull(response);
        assertEquals("token123", response.getToken());
        assertEquals("joao@email.com", response.getEmail());
    }

    @Test
    @DisplayName("Deve lançar exceção ao fazer login com senha incorreta")
    void shouldThrowExceptionWhenPasswordIsWrong() {
        when(userRepository.findByEmail(loginDTO.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(loginDTO));
    }

    @Test
    @DisplayName("Deve lançar exceção ao fazer login com email inexistente")
    void shouldThrowExceptionWhenEmailNotFound() {
        when(userRepository.findByEmail(loginDTO.getEmail())).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(loginDTO));
    }
}