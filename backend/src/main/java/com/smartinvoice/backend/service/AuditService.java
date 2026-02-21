package com.smartinvoice.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinvoice.backend.domain.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.smartinvoice.backend.repository.AuditLogRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void log(UUID businessId,
                    String entityType,
                    UUID entityId,
                    String action,
                    Object oldValue,
                    Object newValue) {

        try {
            AuditLog log = new AuditLog();
            log.setBusinessId(businessId);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setAction(action);

            if (oldValue != null) {
                log.setOldValue(objectMapper.writeValueAsString(oldValue));
            }

            if (newValue != null) {
                log.setNewValue(objectMapper.writeValueAsString(newValue));
            }

            auditLogRepository.save(log);

        } catch (Exception e) {
            throw new RuntimeException("Failed to write audit log");
        }
    }
}