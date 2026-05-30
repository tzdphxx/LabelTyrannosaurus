package com.labelhub.modules.review.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("review_records")
public class ReviewRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submissionId;

    private Long reviewerId;

    private ReviewAction action;

    private Integer reviewLevel;

    private String reason;

    private String reviewComment;

    private LocalDateTime createdAt;
}
