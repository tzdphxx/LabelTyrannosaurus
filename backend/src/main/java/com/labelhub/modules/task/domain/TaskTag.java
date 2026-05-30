package com.labelhub.modules.task.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("task_tags")
public class TaskTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String tagName;

    private LocalDateTime createdAt;
}
