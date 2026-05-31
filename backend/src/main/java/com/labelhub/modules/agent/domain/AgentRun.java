package com.labelhub.modules.agent.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("agent_runs")
public class AgentRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String agentType;

    private Long submissionId;

    private Long assignmentId;

    private Long providerId;

    private String modelName;

    private String promptVersion;

    private String inputSnapshot;

    private String outputSnapshot;

    private AgentRunStatus status;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;
}
