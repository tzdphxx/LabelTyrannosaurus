package com.labelhub.modules.reward.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.reward.domain.LabelerContributionStatsEntity;
import com.labelhub.modules.reward.dto.DailyContributionPoint;
import com.labelhub.modules.reward.dto.TaskContributionResponse;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 贡献统计 Mapper。结算链路通过原子 upsert 维护总体、日维度和任务维度统计。
 */
@Mapper
public interface ContributionStatsMapper extends BaseMapper<LabelerContributionStatsEntity> {

    @Select("""
            select * from labeler_contribution_stats
            where labeler_id = #{labelerId}
            """)
    LabelerContributionStatsEntity selectOverviewByLabelerId(@Param("labelerId") Long labelerId);

    @Select("""
            select stat_date as statDate,
                   submitted_count as submittedCount,
                   approved_count as approvedCount,
                   rejected_count as rejectedCount,
                   reward_amount as rewardAmount
            from labeler_daily_stats
            where labeler_id = #{labelerId}
              and stat_date between #{startDate} and #{endDate}
            order by stat_date asc
            """)
    List<DailyContributionPoint> selectDailyTrend(@Param("labelerId") Long labelerId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Select("""
            select s.task_id as taskId,
                   t.title as taskTitle,
                   s.submitted_count as submittedCount,
                   s.approved_count as approvedCount,
                   s.rejected_count as rejectedCount,
                   s.total_reward as totalReward
            from labeler_task_stats s
            left join tasks t on t.id = s.task_id
            where s.labeler_id = #{labelerId}
            order by s.updated_at desc, s.id desc
            limit #{limit} offset #{offset}
            """)
    List<TaskContributionResponse> selectTaskPage(@Param("labelerId") Long labelerId,
                                                  @Param("limit") int limit,
                                                  @Param("offset") int offset);

    /**
     * 审核通过或金标选中后增加奖励和通过数。未审核提交不进入通过率分母。
     */
    @Insert("""
            insert into labeler_contribution_stats
                (labeler_id, approved_count, total_reward)
            values
                (#{labelerId}, 1, #{amount})
            on duplicate key update
                approved_count = approved_count + 1,
                total_reward = total_reward + values(total_reward)
            """)
    int upsertContributionApproved(@Param("labelerId") Long labelerId, @Param("amount") BigDecimal amount);

    @Insert("""
            insert into labeler_daily_stats
                (labeler_id, stat_date, approved_count, reward_amount)
            values
                (#{labelerId}, #{statDate}, 1, #{amount})
            on duplicate key update
                approved_count = approved_count + 1,
                reward_amount = reward_amount + values(reward_amount)
            """)
    int upsertDailyApproved(@Param("labelerId") Long labelerId,
                            @Param("statDate") LocalDate statDate,
                            @Param("amount") BigDecimal amount);

    @Insert("""
            insert into labeler_task_stats
                (labeler_id, task_id, approved_count, total_reward)
            values
                (#{labelerId}, #{taskId}, 1, #{amount})
            on duplicate key update
                approved_count = approved_count + 1,
                total_reward = total_reward + values(total_reward)
            """)
    int upsertTaskApproved(@Param("labelerId") Long labelerId,
                           @Param("taskId") Long taskId,
                           @Param("amount") BigDecimal amount);

    /**
     * 统一的正向统计入口，供服务层测试和业务编排使用。
     */
    default void increaseApprovedReward(Long labelerId, Long taskId, BigDecimal amount, LocalDate statDate) {
        upsertContributionApproved(labelerId, amount);
        upsertDailyApproved(labelerId, statDate, amount);
        upsertTaskApproved(labelerId, taskId, amount);
    }

    @Insert("""
            insert into labeler_contribution_stats
                (labeler_id, approved_count, total_reward)
            values
                (#{labelerId}, 0, 0)
            on duplicate key update
                approved_count = greatest(approved_count - 1, 0),
                total_reward = greatest(total_reward - #{amount}, 0)
            """)
    int upsertContributionReversed(@Param("labelerId") Long labelerId, @Param("amount") BigDecimal amount);

    @Insert("""
            insert into labeler_daily_stats
                (labeler_id, stat_date, approved_count, reward_amount)
            values
                (#{labelerId}, #{statDate}, 0, 0)
            on duplicate key update
                approved_count = greatest(approved_count - 1, 0),
                reward_amount = greatest(reward_amount - #{amount}, 0)
            """)
    int upsertDailyReversed(@Param("labelerId") Long labelerId,
                            @Param("statDate") LocalDate statDate,
                            @Param("amount") BigDecimal amount);

    @Insert("""
            insert into labeler_task_stats
                (labeler_id, task_id, approved_count, total_reward)
            values
                (#{labelerId}, #{taskId}, 0, 0)
            on duplicate key update
                approved_count = greatest(approved_count - 1, 0),
                total_reward = greatest(total_reward - #{amount}, 0)
            """)
    int upsertTaskReversed(@Param("labelerId") Long labelerId,
                           @Param("taskId") Long taskId,
                           @Param("amount") BigDecimal amount);

    /**
     * 统一的冲正统计入口，保持与负向流水追加处于同一事务。
     */
    default void decreaseApprovedReward(Long labelerId, Long taskId, BigDecimal amount, LocalDate statDate) {
        upsertContributionReversed(labelerId, amount);
        upsertDailyReversed(labelerId, statDate, amount);
        upsertTaskReversed(labelerId, taskId, amount);
    }
}
