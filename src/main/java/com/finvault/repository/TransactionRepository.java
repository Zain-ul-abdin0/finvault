package com.finvault.repository;

import com.finvault.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByReference(String reference);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByWalletId(UUID walletId, Pageable pageable);
}
