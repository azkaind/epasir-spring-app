package com.example.security.service;

import com.example.security.entity.AuditLog;
import com.example.security.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service untuk mencatat audit log secara asinkron.
 * Menggunakan @Async agar tidak memblokir request utama.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String username, String action, String ip, String status, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .username(username)
                    .action(action)
                    .ipAddress(ip)
                    .status(status)
                    .details(details)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Gagal menyimpan audit log: {}", e.getMessage());
        }
    }
}
