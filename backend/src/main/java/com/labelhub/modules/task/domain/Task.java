package com.labelhub.modules.task.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("tasks")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ownerId;

    private String title;

    private String description;

    private String instructionRichText;

    private TaskStatus status;

    private Integer quota;

    private Integer claimedCount;

    private Integer overlapCount;

    private LocalDateTime deadlineAt;

    private Long publishedTemplateVersionId;

    private Long aiReviewConfigId;

    private Boolean rewardVisible;

    private LocalDateTime publishedAt;

    private LocalDateTime endedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
