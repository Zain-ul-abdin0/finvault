package com.finvault.service;

import com.finvault.dto.request.DepositRequest;
import com.finvault.dto.request.RegisterRequest;
import com.finvault.dto.request.TransferRequest;
import com.finvault.dto.response.AuthResponse;
import com.finvault.dto.response.TransactionResponse;
import com.finvault.dto.response.WalletResponse;
import com.finvault.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"finvault.transactions", "finvault.notifications"})
@ActiveProfiles("test")
class TransactionServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("finvault")
            .withUsername("finvault")
            .withPassword("finvault");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> System.getProperty("spring.embedded.kafka.brokers"));
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletService walletService;

    private UUID user1Id;
    private UUID user2Id;
    private UUID user2WalletId;

    @BeforeEach
    void setUp() {
        AuthResponse user1 = authService.register(RegisterRequest.builder()
                .email("alice" + UUID.randomUUID() + "@test.com")
                .password("password123")
                .firstName("Alice")
                .lastName("Smith")
                .build());
        user1Id = user1.getUser().getId();

        AuthResponse user2 = authService.register(RegisterRequest.builder()
                .email("bob" + UUID.randomUUID() + "@test.com")
                .password("password123")
                .firstName("Bob")
                .lastName("Jones")
                .build());
        user2Id = user2.getUser().getId();
        user2WalletId = walletService.getWalletByUserId(user2Id).getId();
    }

    @Test
    void deposit_increasesBalance() {
        TransactionResponse deposit = transactionService.deposit(user1Id, DepositRequest.builder()
                .amount(new BigDecimal("100.00"))
                .description("Initial deposit")
                .build());

        assertThat(deposit.getStatus().name()).isEqualTo("COMPLETED");
        assertThat(deposit.getAmount()).isEqualByComparingTo("100.00");

        WalletResponse wallet = walletService.getWalletByUserId(user1Id);
        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void transfer_movesFundsBetweenWallets() {
        transactionService.deposit(user1Id, DepositRequest.builder()
                .amount(new BigDecimal("200.00"))
                .build());

        TransactionResponse transfer = transactionService.transfer(user1Id, TransferRequest.builder()
                .receiverWalletId(user2WalletId)
                .amount(new BigDecimal("75.50"))
                .description("Payment for services")
                .build());

        assertThat(transfer.getType().name()).isEqualTo("TRANSFER");
        assertThat(walletService.getWalletByUserId(user1Id).getBalance()).isEqualByComparingTo("124.50");
        assertThat(walletService.getWalletByUserId(user2Id).getBalance()).isEqualByComparingTo("75.50");
    }

    @Test
    void transfer_throwsWhenInsufficientFunds() {
        assertThatThrownBy(() -> transactionService.transfer(user1Id, TransferRequest.builder()
                .receiverWalletId(user2WalletId)
                .amount(new BigDecimal("50.00"))
                .build()))
                .isInstanceOf(InsufficientFundsException.class);
    }
}
