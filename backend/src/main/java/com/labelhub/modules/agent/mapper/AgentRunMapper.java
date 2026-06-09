package com.labelhub.modules.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentRunMapper extends BaseMapper<AgentRun> {

    @Update("""
            UPDATE agent_runs
            SET status = #{nextStatus},
                started_at = #{startedAt}
            WHERE id = #{agentRunId}
              AND status = #{expectedStatus}
            """)
    int startIfStatus(@Param("agentRunId") Long agentRunId,
                      @Param("expectedStatus") AgentRunStatus expectedStatus,
                      @Param("nextStatus") AgentRunStatus nextStatus,
                      @Param("startedAt") LocalDateTime startedAt);

    @Update("""
            UPDATE agent_runs
            SET status = #{nextStatus},
                output_snapshot = #{outputSnapshot},
                finished_at = #{finishedAt},
                latency_ms = #{latencyMs}
            WHERE id = #{agentRunId}
              AND status = #{expectedStatus}
            """)
    int completeIfStatus(@Param("agentRunId") Long agentRunId,
                         @Param("expectedStatus") AgentRunStatus expectedStatus,
                         @Param("nextStatus") AgentRunStatus nextStatus,
                         @Param("outputSnapshot") String outputSnapshot,
                         @Param("finishedAt") LocalDateTime finishedAt,
                         @Param("latencyMs") Long latencyMs);

    @Update("""
            UPDATE agent_runs
            SET status = #{nextStatus},
                error_message = #{errorMessage},
                finished_at = #{finishedAt},
                latency_ms = #{latencyMs}
            WHERE id = #{agentRunId}
              AND status = #{expectedStatus}
            """)
    int failIfStatus(@Param("agentRunId") Long agentRunId,
                     @Param("expectedStatus") AgentRunStatus expectedStatus,
                     @Param("nextStatus") AgentRunStatus nextStatus,
                     @Param("errorMessage") String errorMessage,
                     @Param("finishedAt") LocalDateTime finishedAt,
                     @Param("latencyMs") Long latencyMs);
}
