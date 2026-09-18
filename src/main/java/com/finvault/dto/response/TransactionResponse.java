package com.finvault.dto.response;

import com.finvault.entity.TransactionStatus;
import com.finvault.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID id;
    private String reference;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String description;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
