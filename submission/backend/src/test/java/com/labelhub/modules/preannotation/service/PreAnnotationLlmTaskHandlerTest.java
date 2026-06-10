package com.labelhub.modules.preannotation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.infrastructure.llmtask.LlmTaskQueueMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskType;
import com.labelhub.modules.preannotation.domain.PreAnnotation;
import com.labelhub.modules.preannotation.domain.PreAnnotationStatus;
import com.labelhub.modules.preannotation.mapper.PreAnnotationMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreAnnotationLlmTaskHandlerTest {

    @Mock private PreAnnotationService preAnnotationService;
    @Mock private PreAnnotationMapper preAnnotationMapper;

    @Test
    void completedWhenRecordIsMissing() {
        PreAnnotationLlmTaskHandler handler = new PreAnnotationLlmTaskHandler(
                preAnnotationService, preAnnotationMapper);
        when(preAnnotationMapper.selectById(500L)).thenReturn(null);

        assertThat(handler.isCompleted(message())).isTrue();
    }

    @Test
    void completedWhenRecordIsTerminal() {
        PreAnnotationLlmTaskHandler handler = new PreAnnotationLlmTaskHandler(
                preAnnotationService, preAnnotationMapper);
        when(preAnnotationMapper.selectById(500L)).thenReturn(record(PreAnnotationStatus.FAILED));

        assertThat(handler.isCompleted(message())).isTrue();
    }

    @Test
    void handleExecutesQueuedPreAnnotation() {
        PreAnnotationLlmTaskHandler handler = new PreAnnotationLlmTaskHandler(
                preAnnotationService, preAnnotationMapper);

        handler.handle(message());

        verify(preAnnotationService).executeQueuedPreAnnotation(500L);
    }

    private PreAnnotation record(PreAnnotationStatus status) {
        PreAnnotation record = new PreAnnotation();
        record.setId(500L);
        record.setStatus(status);
        return record;
    }

    private LlmTaskQueueMessage message() {
        return new LlmTaskQueueMessage(
                LlmTaskType.PRE_ANNOTATION,
                500L,
                10L,
                20L,
                null,
                500L,
                null,
                300L,
                "trace-pre",
                0,
                Instant.parse("2026-06-03T00:00:00Z")
        );
    }
}
