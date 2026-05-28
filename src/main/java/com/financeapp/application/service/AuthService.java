package com.financeapp.application.service;

import com.financeapp.api.config.JwtService;
import com.financeapp.application.dto.AuthResponseDTO;
import com.financeapp.application.dto.LoginDTO;
import com.financeapp.application.dto.RegisterDTO;
import com.financeapp.domain.entity.User;
import com.financeapp.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponseDTO register(RegisterDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponseDTO(token, user.getName(), user.getEmail());
    }

    public AuthResponseDTO login(LoginDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou senha inválidos"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Email ou senha inválidos");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponseDTO(token, user.getName(), user.getEmail());
    }
}