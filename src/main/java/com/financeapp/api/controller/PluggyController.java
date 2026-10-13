package com.financeapp.api.controller;

import com.financeapp.application.dto.ApiResponse;
import com.financeapp.application.service.PluggyService;
import com.financeapp.domain.entity.PluggyConnection;
import com.financeapp.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pluggy")
@RequiredArgsConstructor
public class PluggyController {

    private final PluggyService   pluggyService;
    private final UserRepository  userRepository;

    private Long getUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"))
                .getId();
    }

    // Gera o token para o widget frontend conectar ao banco
    @GetMapping("/connect-token")
    public ResponseEntity<ApiResponse<Map<String, String>>> connectToken() {
        String token = pluggyService.createConnectToken(getUserId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("accessToken", token)));
    }

    // Lista conexões bancárias do usuário
    @GetMapping("/connections")
    public ResponseEntity<ApiResponse<List<PluggyConnection>>> listConnections() {
        return ResponseEntity.ok(ApiResponse.ok(
                pluggyService.listConnections(getUserId())
        ));
    }

    // Sincroniza dados após conectar um banco via widget
    @PostMapping("/sync/{itemId}")
    public ResponseEntity<ApiResponse<Void>> sync(@PathVariable String itemId) {
        pluggyService.syncItem(getUserId(), itemId);
        return ResponseEntity.ok(ApiResponse.ok("Sincronizado com sucesso", null));
    }

    // Remove conexão bancária
    @DeleteMapping("/connections/{itemId}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable String itemId) {
        pluggyService.removeConnection(getUserId(), itemId);
        return ResponseEntity.ok(ApiResponse.ok("Conexão removida", null));
    }
}