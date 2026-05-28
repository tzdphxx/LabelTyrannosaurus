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
    public Long append(AuditCommand command) {
        AuditLog auditLog = new AuditLog();
        auditLog.setBizType(command.bizType());
        auditLog.setBizId(command.bizId());
        auditLog.setActorType(command.actorType());
        auditLog.setActorId(command.actorId());
        auditLog.setAction(command.action());
        auditLog.setBeforeJson(toJson(command.beforeJson()));
        auditLog.setAfterJson(toJson(command.afterJson()));
        auditLog.setTraceId(command.traceId());
        auditLog.setAgentRunId(command.agentRunId());
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
