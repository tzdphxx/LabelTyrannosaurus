package com.labelhub.modules.assignment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("assignment_dispatches")
public class AssignmentDispatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long datasetItemId;

    private Long labelerId;

    private String status;

    private LocalDateTime dispatchedAt;

    private LocalDateTime claimedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
