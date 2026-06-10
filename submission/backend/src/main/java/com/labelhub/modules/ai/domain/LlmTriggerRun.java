package com.labelhub.modules.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("llm_trigger_runs")
public class LlmTriggerRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long assignmentId;

    private Long templateVersionId;

    private Long datasetItemId;

    private String componentId;

    private Long providerId;

    private String modelName;

    private Long agentRunId;

    private String status;

    private String targetFieldsJson;

    private String inputSnapshotJson;

    private String resultJson;

    private String contentText;

    private Long latencyMs;

    private String errorCode;

    private String errorMessage;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
