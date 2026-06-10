package com.labelhub.modules.assignment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("assignments")
public class Assignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long datasetItemId;

    private Long labelerId;

    private Long templateVersionId;

    private AssignmentStatus status;

    private String draftAnswerJson;

    private Integer draftVersion;

    private LocalDateTime claimedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime returnedAt;

    private LocalDateTime aiReturnedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime updatedAt;
}
