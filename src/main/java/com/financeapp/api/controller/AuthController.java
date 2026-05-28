package com.financeapp.api.controller;

import com.financeapp.application.dto.ApiResponse;
import com.financeapp.application.dto.AuthResponseDTO;
import com.financeapp.application.dto.LoginDTO;
import com.financeapp.application.dto.RegisterDTO;
import com.financeapp.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> register(
            @Valid @RequestBody RegisterDTO dto) {
        AuthResponseDTO response = authService.register(dto);
        return ResponseEntity.ok(ApiResponse.ok("Usuário criado com sucesso", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginDTO dto) {
        AuthResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(ApiResponse.ok("Login realizado com sucesso", response));
    }
}