package com.financeapp.application.service;

import com.financeapp.domain.entity.*;
import com.financeapp.domain.enums.AccountType;
import com.financeapp.domain.enums.TransactionStatus;
import com.financeapp.domain.enums.TransactionType;
import com.financeapp.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PluggyService {

    @Value("${pluggy.client-id}")
    private String clientId;

    @Value("${pluggy.client-secret}")
    private String clientSecret;

    @Value("${pluggy.base-url}")
    private String baseUrl;

    private final PluggyConnectionRepository pluggyConnectionRepository;
    private final AccountRepository          accountRepository;
    private final TransactionRepository      transactionRepository;
    private final UserRepository             userRepository;

    private String cachedApiKey;
    private LocalDateTime apiKeyExpiry;

    // ── AUTENTICAÇÃO ─────────────────────────────────
    private String getApiKey() {
        if (cachedApiKey != null && apiKeyExpiry != null
                && LocalDateTime.now().isBefore(apiKeyExpiry)) {
            return cachedApiKey;
        }

        WebClient client = WebClient.create(baseUrl);

        Map<String, String> body = Map.of(
                "clientId", clientId,
                "clientSecret", clientSecret
        );

        Map response = client.post()
                .uri("/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        cachedApiKey = (String) response.get("apiKey");
        apiKeyExpiry = LocalDateTime.now().plusHours(1);

        return cachedApiKey;
    }

    private WebClient pluggyClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-KEY", getApiKey())
                .build();
    }

    // ── CONNECT TOKEN (para o widget frontend) ────────
    public String createConnectToken(Long userId) {
        Map<String, Object> body = new HashMap<>();
        body.put("clientUserId", String.valueOf(userId));

        Map response = pluggyClient().post()
                .uri("/connect_token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) response.get("accessToken");
    }

    // ── LISTAR CONEXÕES DO USUÁRIO ────────────────────
    public List<PluggyConnection> listConnections(Long userId) {
        return pluggyConnectionRepository.findByUserId(userId);
    }

    // ── SINCRONIZAR ITEM ─────────────────────────────
    @Transactional
    public void syncItem(Long userId, String itemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Busca o item na Pluggy
        Map item = pluggyClient().get()
                .uri("/items/" + itemId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String status        = (String) item.get("status");
        Map    connectorMap  = (Map) item.get("connector");
        String connectorName = connectorMap != null ? (String) connectorMap.get("name") : "Banco";
        Integer connectorId  = connectorMap != null ? (Integer) connectorMap.get("id") : null;

        // Salva ou atualiza a conexão
        PluggyConnection conn = pluggyConnectionRepository
                .findByItemIdAndUserId(itemId, userId)
                .orElse(PluggyConnection.builder().user(user).itemId(itemId).build());

        conn.setConnectorName(connectorName);
        conn.setConnectorId(connectorId);
        conn.setStatus(status);
        pluggyConnectionRepository.save(conn);

        // Busca as contas do item
        Map accountsResponse = pluggyClient().get()
                .uri("/accounts?itemId=" + itemId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map> pluggyAccounts = (List<Map>) accountsResponse.get("results");
        if (pluggyAccounts == null) return;

        for (Map pluggyAcc : pluggyAccounts) {
            syncAccount(user, pluggyAcc, itemId, connectorName);
        }
    }

    // ── SINCRONIZAR CONTA ─────────────────────────────
    private void syncAccount(User user, Map pluggyAcc, String itemId, String connectorName) {
        String pluggyAccountId = (String) pluggyAcc.get("id");
        String accType         = (String) pluggyAcc.get("type");
        String accName         = (String) pluggyAcc.get("name");
        Double balance         = (Double) pluggyAcc.get("balance");

        // Mapeia o tipo da Pluggy para o nosso enum
        AccountType localType = switch (accType.toUpperCase()) {
            case "CREDIT"    -> AccountType.CREDIT_CARD;
            case "SAVINGS"   -> AccountType.SAVINGS;
            case "BANK"      -> AccountType.CHECKING;
            default          -> AccountType.CHECKING;
        };

        // Verifica se conta já existe pelo pluggy_account_id
        Account account = accountRepository
                .findByPluggyAccountId(pluggyAccountId)
                .orElse(null);

        if (account == null) {
            // Cria nova conta
            account = Account.builder()
                    .user(user)
                    .name(connectorName + " — " + accName)
                    .type(localType)
                    .balance(BigDecimal.valueOf(balance != null ? balance : 0))
                    .pluggyAccountId(pluggyAccountId)
                    .pluggyItemId(itemId)
                    .build();
        } else {
            // Atualiza saldo
            account.setBalance(BigDecimal.valueOf(balance != null ? balance : 0));
        }

        accountRepository.save(account);

        // Sincroniza transações dos últimos 90 dias
        syncTransactions(user, account, pluggyAccountId);
    }

    // ── SINCRONIZAR TRANSAÇÕES ────────────────────────
    private void syncTransactions(User user, Account account, String pluggyAccountId) {
        LocalDate from = LocalDate.now().minusDays(90);

        Map txResponse = pluggyClient().get()
                .uri(u -> u.path("/transactions")
                        .queryParam("accountId", pluggyAccountId)
                        .queryParam("from", from.toString())
                        .queryParam("pageSize", 500)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map> pluggyTxs = (List<Map>) txResponse.get("results");
        if (pluggyTxs == null) return;

        for (Map pluggyTx : pluggyTxs) {
            String pluggyTxId = (String) pluggyTx.get("id");

            // Evita duplicatas
            if (transactionRepository.existsByPluggyTransactionId(pluggyTxId)) continue;

            String  typeStr     = (String) pluggyTx.get("type");
            Double  amount      = (Double) pluggyTx.get("amount");
            String  description = (String) pluggyTx.get("description");
            String  dateStr     = (String) pluggyTx.get("date");

            TransactionType txType = "CREDIT".equalsIgnoreCase(typeStr)
                    ? TransactionType.INCOME
                    : TransactionType.EXPENSE;

            LocalDate date = dateStr != null
                    ? LocalDate.parse(dateStr.substring(0, 10))
                    : LocalDate.now();

            Transaction tx = Transaction.builder()
                    .user(user)
                    .account(account)
                    .description(description != null ? description : "Transação importada")
                    .amount(BigDecimal.valueOf(Math.abs(amount != null ? amount : 0)))
                    .type(txType)
                    .date(date)
                    .future(false)
                    .status(TransactionStatus.CONFIRMED)
                    .pluggyTransactionId(pluggyTxId)
                    .build();

            transactionRepository.save(tx);
        }
    }

    // ── REMOVER CONEXÃO ───────────────────────────────
    @Transactional
    public void removeConnection(Long userId, String itemId) {
        pluggyClient().delete()
                .uri("/items/" + itemId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();

        pluggyConnectionRepository.deleteByItemIdAndUserId(itemId, userId);
    }
}