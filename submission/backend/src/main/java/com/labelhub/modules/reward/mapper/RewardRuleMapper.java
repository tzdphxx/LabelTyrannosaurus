package com.labelhub.modules.reward.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.reward.domain.RewardRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RewardRuleMapper extends BaseMapper<RewardRule> {

    @Select("SELECT COUNT(1) FROM reward_rules WHERE task_id = #{taskId}")
    int countByTaskId(@Param("taskId") Long taskId);
}
