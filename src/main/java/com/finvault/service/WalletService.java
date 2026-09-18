package com.finvault.service;

import com.finvault.dto.response.WalletResponse;
import com.finvault.entity.Wallet;
import com.finvault.entity.WalletStatus;
import com.finvault.exception.BusinessException;
import com.finvault.exception.ResourceNotFoundException;
import com.finvault.mapper.EntityMapper;
import com.finvault.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final EntityMapper mapper;

    @Cacheable(value = "wallets", key = "#userId")
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user"));
        return mapper.toWalletResponse(wallet);
    }

    @CacheEvict(value = "wallets", key = "#userId")
    public void evictWalletCache(UUID userId) {
        // Cache eviction handled by annotation
    }

    public Wallet getActiveWalletForUser(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException("Wallet is not active");
        }
        return wallet;
    }
}
