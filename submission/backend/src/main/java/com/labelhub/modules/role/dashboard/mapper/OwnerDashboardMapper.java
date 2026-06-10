package com.labelhub.modules.role.dashboard.mapper;

import com.labelhub.modules.role.dashboard.dto.OwnerDashboardOverviewResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OwnerDashboardMapper {

    @Select("select count(1) from tasks where owner_id = #{ownerId}")
    Long countTotalTasks(@Param("ownerId") Long ownerId);

    @Select("""
            select count(1)
            from tasks
            where owner_id = #{ownerId}
              and status in ('PUBLISHED', 'PAUSED')
            """)
    Long countRunningTasks(@Param("ownerId") Long ownerId);

    @Select("""
            select count(1)
            from assignments a
            inner join tasks t on t.id = a.task_id
            where t.owner_id = #{ownerId}
            """)
    Long countClaimedItems(@Param("ownerId") Long ownerId);

    @Select("""
            select count(1)
            from submissions s
            inner join tasks t on t.id = s.task_id
            where t.owner_id = #{ownerId}
              and s.status <> 'SUPERSEDED'
            """)
    Long countSubmittedItems(@Param("ownerId") Long ownerId);

    @Select("""
            select count(1)
            from submissions s
            inner join tasks t on t.id = s.task_id
            where t.owner_id = #{ownerId}
              and s.status = 'PENDING_FINAL'
            """)
    Long countPendingReview(@Param("ownerId") Long ownerId);

    @Select("""
            select coalesce(sum(case when s.status = 'APPROVED' then 1 else 0 end), 0) as approvedCount,
                   coalesce(sum(case when s.status = 'REJECTED' then 1 else 0 end), 0) as rejectedCount
            from submissions s
            inner join tasks t on t.id = s.task_id
            where t.owner_id = #{ownerId}
              and s.status in ('APPROVED', 'REJECTED')
            """)
    ReviewCountRow selectReviewCounts(@Param("ownerId") Long ownerId);

    @Select("""
            select coalesce(sum(case
                     when rl.direction = 'CREDIT' then rl.amount
                     when rl.direction = 'DEBIT' then -rl.amount
                     else 0
                   end), 0)
            from reward_ledger rl
            inner join tasks t on t.id = rl.task_id
            where t.owner_id = #{ownerId}
            """)
    BigDecimal sumRewardCost(@Param("ownerId") Long ownerId);

    @Select("""
            select count(1)
            from tasks
            where owner_id = #{ownerId}
              and reward_visible = 1
            """)
    Long countRewardVisibleTasks(@Param("ownerId") Long ownerId);

    @Select("""
            select status as name, count(1) as count
            from tasks
            where owner_id = #{ownerId}
            group by status
            """)
    List<KeyCountRow> selectTaskStatusDistribution(@Param("ownerId") Long ownerId);

    @Select("""
            select date(a.claimed_at) as statDate, count(1) as count
            from assignments a
            inner join tasks t on t.id = a.task_id
            where t.owner_id = #{ownerId}
              and a.claimed_at >= #{startAt}
              and a.claimed_at < #{endExclusive}
            group by date(a.claimed_at)
            order by statDate asc
            """)
    List<DateCountRow> selectClaimedTrend(@Param("ownerId") Long ownerId,
                                          @Param("startAt") LocalDateTime startAt,
                                          @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select date(s.submitted_at) as statDate, count(1) as count
            from submissions s
            inner join tasks t on t.id = s.task_id
            where t.owner_id = #{ownerId}
              and s.status <> 'SUPERSEDED'
              and s.submitted_at >= #{startAt}
              and s.submitted_at < #{endExclusive}
            group by date(s.submitted_at)
            order by statDate asc
            """)
    List<DateCountRow> selectSubmittedTrend(@Param("ownerId") Long ownerId,
                                            @Param("startAt") LocalDateTime startAt,
                                            @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select date(s.updated_at) as statDate, count(1) as count
            from submissions s
            inner join tasks t on t.id = s.task_id
            where t.owner_id = #{ownerId}
              and s.status = 'APPROVED'
              and s.updated_at >= #{startAt}
              and s.updated_at < #{endExclusive}
            group by date(s.updated_at)
            order by statDate asc
            """)
    List<DateCountRow> selectApprovedTrend(@Param("ownerId") Long ownerId,
                                           @Param("startAt") LocalDateTime startAt,
                                           @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select t.id as taskId,
                   t.title,
                   coalesce(c.claimedCount, 0) as claimedCount,
                   t.quota,
                   coalesce(p.pendingReviewCount, 0) as pendingReviewCount,
                   coalesce(r.approvedCount, 0) as approvedCount,
                   coalesce(r.rejectedCount, 0) as rejectedCount,
                   t.published_at as publishedAt,
                   ls.lastSubmittedAt as lastSubmittedAt
            from tasks t
            left join (
                select task_id, count(1) as claimedCount
                from assignments
                group by task_id
            ) c on c.task_id = t.id
            left join (
                select task_id, count(1) as pendingReviewCount
                from submissions
                where status = 'PENDING_FINAL'
                group by task_id
            ) p on p.task_id = t.id
            left join (
                select task_id,
                       coalesce(sum(case when status = 'APPROVED' then 1 else 0 end), 0) as approvedCount,
                       coalesce(sum(case when status = 'REJECTED' then 1 else 0 end), 0) as rejectedCount
                from submissions
                where status in ('APPROVED', 'REJECTED')
                group by task_id
            ) r on r.task_id = t.id
            left join (
                select task_id, max(submitted_at) as lastSubmittedAt
                from submissions
                where status <> 'SUPERSEDED'
                group by task_id
            ) ls on ls.task_id = t.id
            where t.owner_id = #{ownerId}
              and t.status = 'PUBLISHED'
              and (
                  coalesce(p.pendingReviewCount, 0) >= 10
                  or t.published_at < #{staleAt}
                  or coalesce(r.approvedCount, 0) + coalesce(r.rejectedCount, 0) >= 5
              )
            order by coalesce(p.pendingReviewCount, 0) desc, t.updated_at desc
            limit 20
            """)
    List<AttentionTaskRow> selectAttentionTaskCandidates(@Param("ownerId") Long ownerId,
                                                         @Param("staleAt") LocalDateTime staleAt);

    @Select("""
            select t.id as taskId,
                   t.title,
                   t.status,
                   case
                     when t.quota = 0 then 0
                     else least(coalesce(ap.approvedCount, 0) * 1.0 / t.quota, 1)
                   end as progressRate,
                   coalesce(pr.pendingReviewCount, 0) as pendingReviewCount,
                   t.updated_at as updatedAt
            from tasks t
            left join (
                select task_id, count(1) as pendingReviewCount
                from submissions
                where status = 'PENDING_FINAL'
                group by task_id
            ) pr on pr.task_id = t.id
            left join (
                select task_id, count(1) as approvedCount
                from submissions
                where status = 'APPROVED'
                group by task_id
            ) ap on ap.task_id = t.id
            where t.owner_id = #{ownerId}
            order by t.updated_at desc
            limit #{limit}
            """)
    List<OwnerDashboardOverviewResponse.RecentTask> selectRecentTasks(@Param("ownerId") Long ownerId,
                                                                      @Param("limit") int limit);

    record ReviewCountRow(Long approvedCount, Long rejectedCount) {
    }

    record KeyCountRow(String name, Long count) {
    }

    record DateCountRow(LocalDate statDate, Long count) {
    }

    record AttentionTaskRow(Long taskId,
                            String title,
                            Long claimedCount,
                            Long quota,
                            Long pendingReviewCount,
                            Long approvedCount,
                            Long rejectedCount,
                            LocalDateTime publishedAt,
                            LocalDateTime lastSubmittedAt) {
    }
}
