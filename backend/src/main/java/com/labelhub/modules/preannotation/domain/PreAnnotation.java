package com.labelhub.modules.preannotation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("pre_annotations")
public class PreAnnotation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Long taskId;

    private Long datasetItemId;

    private Long labelerId;

    private Long agentRunId;

    private PreAnnotationStatus status;

    private String suggestedAnswerJson;

    private String fieldSuggestions;

    private String riskFlags;

    private BigDecimal overallConfidence;

    private String limitations;

    private String promptMode;

    private Boolean degraded;

    private String rawResponse;

    private String errorCode;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
