package com.labelhub.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAuditAppender implements AuditAppender {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public DatabaseAuditAppender(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Long append(String bizType,
                       Long bizId,
                       String actorType,
                       Long actorId,
                       String action,
                       Map<String, Object> beforeJson,
                       Map<String, Object> afterJson,
                       String traceId,
                       Long agentRunId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setBizType(bizType);
        auditLog.setBizId(bizId);
        auditLog.setActorType(actorType);
        auditLog.setActorId(actorId);
        auditLog.setAction(action);
        auditLog.setBeforeJson(toJson(beforeJson));
        auditLog.setAfterJson(toJson(afterJson));
        auditLog.setTraceId(traceId);
        auditLog.setAgentRunId(agentRunId);
        auditLogMapper.insert(auditLog);
        return auditLog.getId();
    }

    private String toJson(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize audit snapshot", ex);
        }
    }
}
