package com.labelhub.modules.reward.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.reward.domain.RewardRuleEntity;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 奖励规则 Mapper。规则按任务递增版本保存，历史版本不覆盖。
 */
@Mapper
public interface RewardRuleRepositoryMapper extends BaseMapper<RewardRuleEntity> {

    /**
     * 查询任务当前最大规则版本，用于创建下一版本。
     */
    @Select("""
            select coalesce(max(effective_version), 0)
            from reward_rules
            where task_id = #{taskId}
            """)
    Integer selectMaxVersionByTaskId(@Param("taskId") Long taskId);

    /**
     * 查询任务最新奖励规则，结算时按该快照写入流水金额。
     */
    @Select("""
            select * from reward_rules
            where task_id = #{taskId}
            order by effective_version desc
            limit 1
            """)
    RewardRuleEntity selectLatestByTaskId(@Param("taskId") Long taskId);

    @Select("""
            <script>
            select rr.*
            from reward_rules rr
            join (
                select task_id, max(effective_version) as effective_version
                from reward_rules
                where task_id in
                <foreach collection="taskIds" item="taskId" open="(" separator="," close=")">
                    #{taskId}
                </foreach>
                group by task_id
            ) latest on rr.task_id = latest.task_id
                and rr.effective_version = latest.effective_version
            </script>
            """)
    List<RewardRuleEntity> selectLatestByTaskIds(@Param("taskIds") Collection<Long> taskIds);
}
