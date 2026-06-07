package com.labelhub.modules.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.audit.domain.AuditLogEntity;
import com.labelhub.modules.audit.repository.AuditLogMapper;
import com.labelhub.modules.audit.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditAppenderTest {

    private final AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
    private final AuditLogService auditLogService = new AuditLogService(auditLogMapper, new ObjectMapper());

    @Test
    void appendRejectsMissingTraceId() {
        AuditCommand command = new AuditCommand(
                "USER",
                10L,
                "SUBMISSION",
                20L,
                "APPROVE",
                null,
                Map.of("status", "APPROVED"),
                " ",
                null
        );

        assertThatThrownBy(() -> auditLogService.append(command))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    @Test
    void appendWritesAiAuditWithAgentRunId() {
        when(auditLogMapper.insert(any(AuditLogEntity.class))).thenAnswer(invocation -> {
            AuditLogEntity entity = invocation.getArgument(0);
            entity.setId(200L);
            return 1;
        });
        AuditCommand command = new AuditCommand(
                "SYSTEM_AGENT",
                99L,
                "AI_REVIEW",
                20L,
                "AI_REVIEW_COMPLETED",
                Map.of("status", "RUNNING"),
                Map.of("status", "SUCCESS"),
                "trace-1",
                300L
        );

        Long auditLogId = auditLogService.append(command);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(auditLogId).isEqualTo(200L);
        assertThat(saved.getActorType()).isEqualTo("SYSTEM_AGENT");
        assertThat(saved.getAgentRunId()).isEqualTo(300L);
        assertThat(saved.getTraceId()).isEqualTo("trace-1");
        assertThat(saved.getAfterJson()).contains("\"status\":\"SUCCESS\"");
    }

    @Test
    void listByBizReturnsAuditTimeline() {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(200L);
        entity.setBizType("SUBMISSION");
        entity.setBizId(20L);
        entity.setActorType("USER");
        entity.setActorId(10L);
        entity.setAction("APPROVE");
        entity.setBeforeJson("{\"status\":\"PENDING_FINAL\"}");
        entity.setAfterJson("{\"status\":\"APPROVED\"}");
        entity.setTraceId("trace-2");
        entity.setCreatedAt(LocalDateTime.of(2026, 5, 27, 12, 0));
        when(auditLogMapper.selectByBiz("SUBMISSION", 20L)).thenReturn(List.of(entity));

        var logs = auditLogService.listByBiz("SUBMISSION", 20L);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).auditLogId()).isEqualTo(200L);
        assertThat(logs.get(0).afterJson().get("status").asText()).isEqualTo("APPROVED");
    }
}
