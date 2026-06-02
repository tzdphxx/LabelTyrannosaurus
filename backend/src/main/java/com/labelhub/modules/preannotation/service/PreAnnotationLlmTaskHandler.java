package com.labelhub.modules.preannotation.service;

import com.labelhub.infrastructure.llmtask.LlmTaskHandler;
import com.labelhub.infrastructure.llmtask.LlmTaskQueueMessage;
import com.labelhub.infrastructure.llmtask.LlmTaskType;
import com.labelhub.modules.preannotation.domain.PreAnnotation;
import com.labelhub.modules.preannotation.domain.PreAnnotationStatus;
import com.labelhub.modules.preannotation.mapper.PreAnnotationMapper;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PreAnnotationLlmTaskHandler implements LlmTaskHandler {

    private static final Set<PreAnnotationStatus> FINAL_STATUSES = Set.of(
            PreAnnotationStatus.SUCCESS,
            PreAnnotationStatus.FAILED,
            PreAnnotationStatus.RATE_LIMITED,
            PreAnnotationStatus.MANUAL_REQUIRED);

    private final PreAnnotationService preAnnotationService;
    private final PreAnnotationMapper preAnnotationMapper;

    public PreAnnotationLlmTaskHandler(PreAnnotationService preAnnotationService,
                                       PreAnnotationMapper preAnnotationMapper) {
        this.preAnnotationService = preAnnotationService;
        this.preAnnotationMapper = preAnnotationMapper;
    }

    @Override
    public LlmTaskType taskType() {
        return LlmTaskType.PRE_ANNOTATION;
    }

    @Override
    public boolean isCompleted(LlmTaskQueueMessage message) {
        PreAnnotation record = preAnnotationMapper.selectById(message.preAnnotationId());
        return record == null || FINAL_STATUSES.contains(record.getStatus());
    }

    @Override
    public void handle(LlmTaskQueueMessage message) {
        preAnnotationService.executeQueuedPreAnnotation(message.preAnnotationId());
    }
}
