package com.labelhub.modules.role.dashboard.mapper;

import com.labelhub.modules.role.dashboard.dto.LabelerDashboardOverviewResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LabelerDashboardMapper {

    @Select("""
            select count(1)
            from assignments
            where labeler_id = #{labelerId}
              and claimed_at >= #{startAt}
              and claimed_at < #{endExclusive}
            """)
    Long countClaimed(@Param("labelerId") Long labelerId,
                      @Param("startAt") LocalDateTime startAt,
                      @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select coalesce(sum(case
                     when status <> 'SUPERSEDED'
                      and submitted_at >= #{startAt}
                      and submitted_at < #{endExclusive}
                     then 1 else 0
                   end), 0) as submittedCount,
                   coalesce(sum(case
                     when status = 'APPROVED'
                      and updated_at >= #{startAt}
                      and updated_at < #{endExclusive}
                     then 1 else 0
                   end), 0) as approvedCount,
                   coalesce(sum(case
                     when status = 'REJECTED'
                      and updated_at >= #{startAt}
                      and updated_at < #{endExclusive}
                     then 1 else 0
                   end), 0) as rejectedCount
            from submissions
            where labeler_id = #{labelerId}
              and (
                (submitted_at >= #{startAt} and submitted_at < #{endExclusive})
                or (updated_at >= #{startAt} and updated_at < #{endExclusive})
              )
            """)
    SubmissionCountRow selectSubmissionCounts(@Param("labelerId") Long labelerId,
                                              @Param("startAt") LocalDateTime startAt,
                                              @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select coalesce(sum(case
                     when direction = 'CREDIT' then amount
                     when direction = 'DEBIT' then -amount
                     else 0
                   end), 0)
            from reward_ledger
            where labeler_id = #{labelerId}
              and created_at >= #{startAt}
              and created_at < #{endExclusive}
            """)
    BigDecimal sumPeriodReward(@Param("labelerId") Long labelerId,
                               @Param("startAt") LocalDateTime startAt,
                               @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select coalesce(sum(case
                     when direction = 'CREDIT' then amount
                     when direction = 'DEBIT' then -amount
                     else 0
                   end), 0)
            from reward_ledger
            where labeler_id = #{labelerId}
            """)
    BigDecimal sumTotalReward(@Param("labelerId") Long labelerId);

    @Select("""
            select count(1)
            from assignments
            where labeler_id = #{labelerId}
              and status in ('CLAIMED', 'DRAFTING', 'AI_RETURNED', 'RETURNED')
            """)
    Long countClaimedNotSubmitted(@Param("labelerId") Long labelerId);

    @Select("""
            select count(1)
            from assignments
            where labeler_id = #{labelerId}
              and status in ('AI_RETURNED', 'RETURNED')
            """)
    Long countRejectedNeedFix(@Param("labelerId") Long labelerId);

    @Select("""
            select count(distinct task_id)
            from assignments
            where labeler_id = #{labelerId}
              and status in ('CLAIMED', 'DRAFTING', 'AI_RETURNED', 'RETURNED')
            """)
    Long countContinuableTasks(@Param("labelerId") Long labelerId);

    @Select("""
            select count(1)
            from submissions
            where labeler_id = #{labelerId}
              and status <> 'SUPERSEDED'
              and submitted_at >= #{startAt}
              and submitted_at < #{endExclusive}
            """)
    Long countRecentSubmitted(@Param("labelerId") Long labelerId,
                              @Param("startAt") LocalDateTime startAt,
                              @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select count(distinct a.task_id)
            from assignments a
            inner join tasks t on t.id = a.task_id
            where a.labeler_id = #{labelerId}
              and a.status <> 'CANCELLED'
              and t.reward_visible = 0
            """)
    Long countRewardNotVisibleTasks(@Param("labelerId") Long labelerId);

    @Select("""
            select date(submitted_at) as statDate, count(1) as count
            from submissions
            where labeler_id = #{labelerId}
              and status <> 'SUPERSEDED'
              and submitted_at >= #{startAt}
              and submitted_at < #{endExclusive}
            group by date(submitted_at)
            order by statDate asc
            """)
    List<DateCountRow> selectSubmittedTrend(@Param("labelerId") Long labelerId,
                                            @Param("startAt") LocalDateTime startAt,
                                            @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select date(updated_at) as statDate, count(1) as count
            from submissions
            where labeler_id = #{labelerId}
              and status = 'APPROVED'
              and updated_at >= #{startAt}
              and updated_at < #{endExclusive}
            group by date(updated_at)
            order by statDate asc
            """)
    List<DateCountRow> selectApprovedTrend(@Param("labelerId") Long labelerId,
                                           @Param("startAt") LocalDateTime startAt,
                                           @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select date(created_at) as statDate,
                   coalesce(sum(case
                     when direction = 'CREDIT' then amount
                     when direction = 'DEBIT' then -amount
                     else 0
                   end), 0) as reward
            from reward_ledger
            where labeler_id = #{labelerId}
              and created_at >= #{startAt}
              and created_at < #{endExclusive}
            group by date(created_at)
            order by statDate asc
            """)
    List<DateRewardRow> selectRewardTrend(@Param("labelerId") Long labelerId,
                                          @Param("startAt") LocalDateTime startAt,
                                          @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select sc.task_id as taskId,
                   t.title as taskTitle,
                   sc.submittedCount,
                   sc.approvedCount,
                   coalesce(r.totalReward, 0) as totalReward,
                   concat('/app/labeler/submissions?taskId=', sc.task_id) as targetPath
            from (
              select task_id,
                     coalesce(sum(case
                       when status <> 'SUPERSEDED'
                        and submitted_at >= #{startAt}
                        and submitted_at < #{endExclusive}
                       then 1 else 0
                     end), 0) as submittedCount,
                     coalesce(sum(case
                       when status = 'APPROVED'
                        and updated_at >= #{startAt}
                        and updated_at < #{endExclusive}
                       then 1 else 0
                     end), 0) as approvedCount
              from submissions
              where labeler_id = #{labelerId}
                and (
                  (submitted_at >= #{startAt} and submitted_at < #{endExclusive})
                  or (updated_at >= #{startAt} and updated_at < #{endExclusive})
                )
              group by task_id
            ) sc
            inner join tasks t on t.id = sc.task_id
            left join (
              select task_id,
                     coalesce(sum(case
                       when direction = 'CREDIT' then amount
                       when direction = 'DEBIT' then -amount
                       else 0
                     end), 0) as totalReward
              from reward_ledger
              where labeler_id = #{labelerId}
                and created_at >= #{startAt}
                and created_at < #{endExclusive}
              group by task_id
            ) r on r.task_id = sc.task_id
            where sc.submittedCount > 0
               or sc.approvedCount > 0
               or coalesce(r.totalReward, 0) <> 0
            order by sc.submittedCount desc, sc.approvedCount desc, coalesce(r.totalReward, 0) desc
            limit #{limit}
            """)
    List<LabelerDashboardOverviewResponse.TaskContribution> selectTaskContributions(
            @Param("labelerId") Long labelerId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("limit") int limit);

    record SubmissionCountRow(Long submittedCount, Long approvedCount, Long rejectedCount) {
    }

    record DateCountRow(LocalDate statDate, Long count) {
    }

    record DateRewardRow(LocalDate statDate, BigDecimal reward) {
    }
}
