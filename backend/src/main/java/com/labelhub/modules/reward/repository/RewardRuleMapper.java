package com.labelhub.modules.reward.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.reward.domain.RewardRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 奖励规则 Mapper。规则按任务递增版本保存，历史版本不覆盖。
 */
@Mapper
public interface RewardRuleMapper extends BaseMapper<RewardRuleEntity> {

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
}
