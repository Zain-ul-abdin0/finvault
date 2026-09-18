package com.finvault.controller;

import com.finvault.dto.request.DepositRequest;
import com.finvault.dto.request.TransferRequest;
import com.finvault.dto.response.TransactionResponse;
import com.finvault.service.CurrentUserService;
import com.finvault.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Deposits, transfers, and transaction history")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrentUserService currentUserService;

    @PostMapping("/deposit")
    @Operation(summary = "Deposit funds into wallet")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
        TransactionResponse response = transactionService.deposit(currentUserService.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds to another wallet")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.transfer(currentUserService.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get paginated transaction history")
    public ResponseEntity<Page<TransactionResponse>> getHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(currentUserService.getCurrentUserId(), pageable));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get transaction by reference")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String reference) {
        return ResponseEntity.ok(transactionService.getTransaction(currentUserService.getCurrentUserId(), reference));
    }
}
