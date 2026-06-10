package com.labelhub.modules.review.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("conflict_groups")
public class ConflictGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long datasetItemId;

    private ConflictStatus status;

    private BigDecimal consensusScore;

    private Long goldenSubmissionId;

    private Long resolvedBy;

    private String resolvedReason;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;
}
