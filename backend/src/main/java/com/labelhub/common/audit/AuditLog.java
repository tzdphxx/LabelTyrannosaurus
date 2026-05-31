package com.labelhub.common.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("audit_logs")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizType;

    private Long bizId;

    private String actorType;

    private Long actorId;

    private String action;

    private String beforeJson;

    private String afterJson;

    private String traceId;

    private Long agentRunId;

    private LocalDateTime createdAt;
}
