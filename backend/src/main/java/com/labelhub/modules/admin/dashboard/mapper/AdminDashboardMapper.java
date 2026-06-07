package com.labelhub.modules.admin.dashboard.mapper;

import com.labelhub.modules.admin.dashboard.dto.AdminDashboardTopLabeler;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardTopTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminDashboardMapper {

    @Select("""
            select count(distinct t.id)
            from tasks t
            where t.status = 'PUBLISHED'
               or exists (
                    select 1 from assignments a
                    where a.task_id = t.id
                      and a.claimed_at >= #{startAt}
                      and a.claimed_at < #{endExclusive}
               )
               or exists (
                    select 1 from submissions s
                    where s.task_id = t.id
                      and s.status <> 'SUPERSEDED'
                      and s.submitted_at >= #{startAt}
                      and s.submitted_at < #{endExclusive}
               )
            """)
    Long countActiveTasks(@Param("startAt") LocalDateTime startAt,
                          @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select count(1)
            from assignments
            where claimed_at >= #{startAt}
              and claimed_at < #{endExclusive}
            """)
    Long countClaimed(@Param("startAt") LocalDateTime startAt,
                      @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select count(1)
            from submissions
            where status <> 'SUPERSEDED'
              and submitted_at >= #{startAt}
              and submitted_at < #{endExclusive}
            """)
    Long countSubmitted(@Param("startAt") LocalDateTime startAt,
                        @Param("endExclusive") LocalDateTime endExclusive);

    @Select("select count(1) from submissions where status = 'PENDING_FINAL'")
    Long countPendingReview();

    @Select("""
            select coalesce(sum(case when status = 'APPROVED' then 1 else 0 end), 0) as approvedCount,
                   coalesce(sum(case when status = 'REJECTED' then 1 else 0 end), 0) as rejectedCount
            from submissions
            where status in ('APPROVED', 'REJECTED')
              and updated_at >= #{startAt}
              and updated_at < #{endExclusive}
            """)
    ReviewCountRow selectReviewCounts(@Param("startAt") LocalDateTime startAt,
                                      @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select coalesce(sum(amount), 0)
            from reward_ledger
            where direction = 'CREDIT'
              and created_at >= #{startAt}
              and created_at < #{endExclusive}
            """)
    BigDecimal sumRewardAmount(@Param("startAt") LocalDateTime startAt,
                               @Param("endExclusive") LocalDateTime endExclusive);

    @Select("select count(1) from users where user_type <> 'SYSTEM'")
    Long countTotalUsers();

    @Select("""
            select count(1)
            from users
            where user_type <> 'SYSTEM'
              and (enabled = false or login_enabled = false)
            """)
    Long countDisabledUsers();

    @Select("""
            select count(1)
            from users
            where user_type <> 'SYSTEM'
              and created_at >= #{startAt}
              and created_at < #{endExclusive}
            """)
    Long countNewUsers(@Param("startAt") LocalDateTime startAt,
                       @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select ur.role_code as name, count(1) as count
            from user_roles ur
            inner join users u on u.id = ur.user_id
            where u.user_type <> 'SYSTEM'
              and ur.role_code in ('ADMIN', 'OWNER', 'LABELER', 'REVIEWER')
            group by ur.role_code
            """)
    List<KeyCountRow> selectRoleCounts();

    @Select("""
            select date(submitted_at) as statDate, count(1) as count
            from submissions
            where status <> 'SUPERSEDED'
              and submitted_at >= #{startAt}
              and submitted_at < #{endExclusive}
            group by date(submitted_at)
            order by statDate asc
            """)
    List<DateCountRow> selectSubmittedTrend(@Param("startAt") LocalDateTime startAt,
                                            @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select date(updated_at) as statDate,
                   coalesce(sum(case when status = 'APPROVED' then 1 else 0 end), 0) as approvedCount,
                   coalesce(sum(case when status = 'REJECTED' then 1 else 0 end), 0) as rejectedCount
            from submissions
            where status in ('APPROVED', 'REJECTED')
              and updated_at >= #{startAt}
              and updated_at < #{endExclusive}
            group by date(updated_at)
            order by statDate asc
            """)
    List<DateReviewCountRow> selectReviewTrend(@Param("startAt") LocalDateTime startAt,
                                               @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select date(created_at) as statDate, coalesce(sum(amount), 0) as rewardAmount
            from reward_ledger
            where direction = 'CREDIT'
              and created_at >= #{startAt}
              and created_at < #{endExclusive}
            group by date(created_at)
            order by statDate asc
            """)
    List<DateRewardRow> selectRewardTrend(@Param("startAt") LocalDateTime startAt,
                                          @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select status as name, count(1) as count
            from tasks
            group by status
            """)
    List<KeyCountRow> selectTaskStatusDistribution();

    @Select("""
            select s.labeler_id as labelerId,
                   coalesce(nullif(u.display_name, ''), u.username) as displayName,
                   count(1) as submittedCount,
                   coalesce(sum(case when s.status = 'APPROVED' then 1 else 0 end), 0) as approvedCount,
                   coalesce(sum(case when rl.direction = 'CREDIT' then rl.amount else 0 end), 0) as rewardAmount
            from submissions s
            inner join users u on u.id = s.labeler_id
            left join reward_ledger rl on rl.submission_id = s.id and rl.direction = 'CREDIT'
            where s.status <> 'SUPERSEDED'
              and s.submitted_at >= #{startAt}
              and s.submitted_at < #{endExclusive}
            group by s.labeler_id, u.display_name, u.username
            order by submittedCount desc, approvedCount desc, rewardAmount desc
            limit #{limit}
            """)
    List<AdminDashboardTopLabeler> selectTopLabelers(@Param("startAt") LocalDateTime startAt,
                                                     @Param("endExclusive") LocalDateTime endExclusive,
                                                     @Param("limit") int limit);

    @Select("""
            select t.id as taskId,
                   t.title as title,
                   count(s.id) as submittedCount,
                   coalesce(sum(case when s.status = 'APPROVED' then 1 else 0 end), 0) as approvedCount,
                   coalesce(sum(case when s.status = 'REJECTED' then 1 else 0 end), 0) as rejectedCount
            from tasks t
            left join submissions s on s.task_id = t.id
                and s.status <> 'SUPERSEDED'
                and s.submitted_at >= #{startAt}
                and s.submitted_at < #{endExclusive}
            group by t.id, t.title
            having submittedCount > 0
            order by submittedCount desc, approvedCount desc, rejectedCount asc
            limit #{limit}
            """)
    List<AdminDashboardTopTask> selectTopTasks(@Param("startAt") LocalDateTime startAt,
                                               @Param("endExclusive") LocalDateTime endExclusive,
                                               @Param("limit") int limit);

    @Select("""
            select count(1) > 0
            from submissions
            where status = 'PENDING_FINAL'
              and submitted_at < #{timeoutAt}
            """)
    Boolean existsOverduePendingReview(@Param("timeoutAt") LocalDateTime timeoutAt);

    @Select("""
            select t.id as taskId,
                   t.title as title,
                   coalesce(sum(case when s.status = 'APPROVED' then 1 else 0 end), 0) as approvedCount,
                   coalesce(sum(case when s.status = 'REJECTED' then 1 else 0 end), 0) as rejectedCount
            from tasks t
            inner join submissions s on s.task_id = t.id
            where s.status in ('APPROVED', 'REJECTED')
              and s.updated_at >= #{startAt}
              and s.updated_at < #{endExclusive}
            group by t.id, t.title
            having approvedCount + rejectedCount >= 5
            order by rejectedCount desc, approvedCount asc
            """)
    List<HighRejectionTaskRow> selectHighRejectionRateTasks(@Param("startAt") LocalDateTime startAt,
                                                            @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select t.id as taskId,
                   t.title as title,
                   count(a.id) as claimedCount
            from tasks t
            inner join assignments a on a.task_id = t.id
                and a.claimed_at >= #{startAt}
                and a.claimed_at < #{endExclusive}
            where t.status = 'PUBLISHED'
              and not exists (
                    select 1 from submissions s
                    where s.task_id = t.id
                      and s.status <> 'SUPERSEDED'
                      and s.submitted_at >= #{startAt}
                      and s.submitted_at < #{endExclusive}
              )
            group by t.id, t.title
            order by claimedCount desc
            """)
    List<ZeroSubmissionTaskRow> selectZeroSubmissionActiveTasks(@Param("startAt") LocalDateTime startAt,
                                                                @Param("endExclusive") LocalDateTime endExclusive);

    record ReviewCountRow(Long approvedCount, Long rejectedCount) {
    }

    record KeyCountRow(String name, Long count) {
    }

    record DateCountRow(LocalDate statDate, Long count) {
    }

    record DateReviewCountRow(LocalDate statDate, Long approvedCount, Long rejectedCount) {
    }

    record DateRewardRow(LocalDate statDate, BigDecimal rewardAmount) {
    }

    record HighRejectionTaskRow(Long taskId, String title, Long approvedCount, Long rejectedCount) {
    }

    record ZeroSubmissionTaskRow(Long taskId, String title, Long claimedCount) {
    }
}
