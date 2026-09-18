package com.finvault.service;

import com.finvault.entity.AuditLog;
import com.finvault.entity.User;
import com.finvault.repository.AuditLogRepository;
import com.finvault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Async
    public void log(UUID userId, String action, String entityType, UUID entityId, String details) {
        User user = userRepository.findById(userId).orElse(null);

        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
    }
}
