package com.labelhub.modules.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.audit.domain.AuditLogEntity;
import com.labelhub.modules.audit.dto.AuditLogResponse;
import com.labelhub.modules.audit.repository.AuditLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * BE-B 维护的审计日志追加与查询服务。
 *
 * <p>BE-A 可以调用 {@link #append(AuditCommand)} 记录 AI 或审核动作，但本服务只追加审计行，
 * 不修改 BE-A 负责的业务状态。</p>
 */
@Service
public class AuditLogService implements AuditAppender {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 追加一条审计记录。虽然当前 baseline 表结构仍允许 traceId 为空，但业务层强制要求携带，
     * 便于跨模块问题追踪。
     */
    @Override
    @Transactional
    public Long append(AuditCommand command) {
        validate(command);
        AuditLogEntity entity = new AuditLogEntity();
        entity.setActorType(command.actorType());
        entity.setActorId(command.actorId());
        entity.setBizType(command.bizType());
        entity.setBizId(command.bizId());
        entity.setAction(command.action());
        entity.setBeforeJson(toJson(command.beforeJson()));
        entity.setAfterJson(toJson(command.afterJson()));
        entity.setTraceId(command.traceId());
        entity.setAgentRunId(command.agentRunId());
        auditLogMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 返回指定业务对象的审计时间线。
     */
    public List<AuditLogResponse> listByBiz(String bizType, Long bizId) {
        if (!StringUtils.hasText(bizType) || bizId == null) {
            throw new BusinessException(400102, "Invalid audit query parameter");
        }
        return auditLogMapper.selectByBiz(bizType, bizId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validate(AuditCommand command) {
        if (command == null
                || !StringUtils.hasText(command.traceId())
                || !StringUtils.hasText(command.actorType())
                || !StringUtils.hasText(command.bizType())
                || command.bizId() == null
                || !StringUtils.hasText(command.action())) {
            throw new BusinessException(400102, "Invalid audit command");
        }
    }

    private String toJson(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(400102, "Invalid audit json snapshot");
        }
    }

    private AuditLogResponse toResponse(AuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getBizType(),
                entity.getBizId(),
                entity.getActorType(),
                entity.getActorId(),
                entity.getAction(),
                parseJson(entity.getBeforeJson()),
                parseJson(entity.getAfterJson()),
                entity.getTraceId(),
                entity.getAgentRunId(),
                entity.getCreatedAt()
        );
    }

    private JsonNode parseJson(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500001, "Invalid audit json stored");
        }
    }
}
