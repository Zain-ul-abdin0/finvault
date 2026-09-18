package com.finvault.service;

import com.finvault.dto.request.DepositRequest;
import com.finvault.dto.request.TransferRequest;
import com.finvault.dto.response.TransactionResponse;
import com.finvault.entity.*;
import com.finvault.event.NotificationEvent;
import com.finvault.event.TransactionEvent;
import com.finvault.exception.BusinessException;
import com.finvault.exception.InsufficientFundsException;
import com.finvault.exception.ResourceNotFoundException;
import com.finvault.mapper.EntityMapper;
import com.finvault.messaging.KafkaEventPublisher;
import com.finvault.repository.TransactionRepository;
import com.finvault.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final KafkaEventPublisher eventPublisher;
    private final EntityMapper mapper;
    private final AuditService auditService;

    @Transactional
    public TransactionResponse deposit(UUID userId, DepositRequest request) {
        Wallet wallet = walletService.getActiveWalletForUser(userId);
        Wallet lockedWallet = walletRepository.findByIdForUpdate(wallet.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        String reference = generateReference("DEP");

        Transaction transaction = Transaction.builder()
                .reference(reference)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(request.getAmount())
                .currency(lockedWallet.getCurrency())
                .description(request.getDescription())
                .receiverWallet(lockedWallet)
                .completedAt(LocalDateTime.now())
                .build();

        lockedWallet.setBalance(lockedWallet.getBalance().add(request.getAmount()));
        walletRepository.save(lockedWallet);
        transaction = transactionRepository.save(transaction);

        walletService.evictWalletCache(userId);
        publishEvents(transaction, userId, "Deposit successful");

        auditService.log(userId, "DEPOSIT", "Transaction", transaction.getId(),
                "Deposited " + request.getAmount());

        return mapper.toTransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse transfer(UUID senderUserId, TransferRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Transfer amount must be positive");
        }

        Wallet senderWallet = walletService.getActiveWalletForUser(senderUserId);
        Wallet receiverWallet = walletRepository.findById(request.getReceiverWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));

        if (receiverWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException("Receiver wallet is not active");
        }

        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new BusinessException("Cannot transfer to the same wallet");
        }

        Wallet lockedSender = walletRepository.findByIdForUpdate(senderWallet.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));
        Wallet lockedReceiver = walletRepository.findByIdForUpdate(receiverWallet.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));

        if (lockedSender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance for transfer");
        }

        String reference = generateReference("TRF");

        Transaction transaction = Transaction.builder()
                .reference(reference)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .amount(request.getAmount())
                .currency(lockedSender.getCurrency())
                .description(request.getDescription())
                .senderWallet(lockedSender)
                .receiverWallet(lockedReceiver)
                .completedAt(LocalDateTime.now())
                .build();

        lockedSender.setBalance(lockedSender.getBalance().subtract(request.getAmount()));
        lockedReceiver.setBalance(lockedReceiver.getBalance().add(request.getAmount()));

        walletRepository.save(lockedSender);
        walletRepository.save(lockedReceiver);
        transaction = transactionRepository.save(transaction);

        walletService.evictWalletCache(senderUserId);
        walletService.evictWalletCache(lockedReceiver.getUser().getId());

        publishEvents(transaction, senderUserId, "Transfer sent");
        eventPublisher.publishNotification(NotificationEvent.builder()
                .userId(lockedReceiver.getUser().getId())
                .type("TRANSFER_RECEIVED")
                .title("Money Received")
                .message("You received a transfer of " + request.getAmount())
                .amount(request.getAmount())
                .build());

        auditService.log(senderUserId, "TRANSFER", "Transaction", transaction.getId(),
                "Transferred " + request.getAmount() + " to wallet " + receiverWallet.getId());

        return mapper.toTransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(UUID userId, Pageable pageable) {
        Wallet wallet = walletService.getActiveWalletForUser(userId);
        return transactionRepository.findByWalletId(wallet.getId(), pageable)
                .map(mapper::toTransactionResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID userId, String reference) {
        Wallet wallet = walletService.getActiveWalletForUser(userId);
        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        boolean isParticipant = (transaction.getSenderWallet() != null && transaction.getSenderWallet().getId().equals(wallet.getId()))
                || (transaction.getReceiverWallet() != null && transaction.getReceiverWallet().getId().equals(wallet.getId()));

        if (!isParticipant) {
            throw new BusinessException("Access denied to this transaction");
        }

        return mapper.toTransactionResponse(transaction);
    }

    private void publishEvents(Transaction transaction, UUID userId, String notificationTitle) {
        eventPublisher.publishTransactionEvent(TransactionEvent.builder()
                .transactionId(transaction.getId())
                .reference(transaction.getReference())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .senderWalletId(transaction.getSenderWallet() != null ? transaction.getSenderWallet().getId() : null)
                .receiverWalletId(transaction.getReceiverWallet() != null ? transaction.getReceiverWallet().getId() : null)
                .timestamp(LocalDateTime.now())
                .build());

        eventPublisher.publishNotification(NotificationEvent.builder()
                .userId(userId)
                .type(transaction.getType().name())
                .title(notificationTitle)
                .message("Transaction " + transaction.getReference() + " completed")
                .amount(transaction.getAmount())
                .build());
    }

    private String generateReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
