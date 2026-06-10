package com.labelhub.modules.review.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("review_tasks")
public class ReviewTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submissionId;

    private Long taskId;

    private Integer reviewLevel;

    private Long assignedReviewerId;

    private Long assignedBy;

    private ReviewTaskStatus status;

    private Integer reviewVersion;

    private LocalDateTime assignedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime dueAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
