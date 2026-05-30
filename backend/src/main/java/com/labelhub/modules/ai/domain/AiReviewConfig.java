package com.labelhub.modules.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ai_review_configs")
public class AiReviewConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long providerId;

    private String modelName;

    private String promptTemplate;

    private String scoringDimensionsJson;

    private BigDecimal passThreshold;

    private BigDecimal manualReviewThreshold;

    private String outputSchemaJson;

    private String promptVersion;

    private Integer maxRetry;

    private String aiRejectAction;

    private String agentMode;

    private String enabledToolsJson;

    private Integer maxIterations;

    private String aiFlowPolicy;

    private Boolean allowAiDirectApprove;

    private Boolean allowAiDirectReject;

    private BigDecimal rejectThreshold;

    private BigDecimal confidenceThreshold;

    private String riskFlagsForceManual;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
