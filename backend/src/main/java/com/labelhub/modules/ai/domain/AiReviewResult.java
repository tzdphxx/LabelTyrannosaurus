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
@TableName("ai_review_results")
public class AiReviewResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submissionId;

    private Long effectiveRunId;

    private Long providerId;

    private String modelName;

    private AiReviewStatus status;

    private String decision;

    private BigDecimal averageScore;

    private String dimensionScores;

    private String riskFlags;

    private String suggestion;

    private String flowAction;

    private BigDecimal confidence;

    private String promptMode;

    private Boolean degraded;

    private String limitations;

    private String promptSnapshot;

    private String rawResponse;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    private String errorCode;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
