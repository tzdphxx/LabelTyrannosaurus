package com.labelhub.modules.role.dashboard.mapper;

import com.labelhub.modules.role.dashboard.dto.ReviewerDashboardOverviewResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReviewerDashboardMapper {

    @Select("""
            select count(1)
            from review_tasks
            where assigned_reviewer_id = #{reviewerId}
              and status in ('PENDING', 'IN_REVIEW')
            """)
    Long countPending(@Param("reviewerId") Long reviewerId);

    @Select("""
            select count(1)
            from review_tasks
            where assigned_reviewer_id = #{reviewerId}
              and status in ('PENDING', 'IN_REVIEW')
              and (
                due_at < #{now}
                or (due_at is null and assigned_at < #{timeoutAt})
              )
            """)
    Long countOverdue(@Param("reviewerId") Long reviewerId,
                      @Param("now") LocalDateTime now,
                      @Param("timeoutAt") LocalDateTime timeoutAt);

    @Select("""
            select count(distinct s.id)
            from ai_review_results ar
            inner join submissions s on s.id = ar.submission_id
            left join review_tasks rt on rt.submission_id = s.id
              and rt.assigned_reviewer_id = #{reviewerId}
              and rt.status in ('PENDING', 'IN_REVIEW')
            where (s.assigned_reviewer_id = #{reviewerId} or rt.id is not null)
              and (ar.status = 'MANUAL_REQUIRED' or ar.decision = 'MANUAL_REVIEW')
            """)
    Long countManualRequired(@Param("reviewerId") Long reviewerId);

    @Select("""
            select count(distinct cg.id)
            from conflict_groups cg
            inner join submissions s on s.task_id = cg.task_id
              and s.dataset_item_id = cg.dataset_item_id
            left join review_tasks rt on rt.submission_id = s.id
              and rt.assigned_reviewer_id = #{reviewerId}
              and rt.status in ('PENDING', 'IN_REVIEW')
            where cg.status = 'CONFLICTED'
              and (s.assigned_reviewer_id = #{reviewerId} or rt.id is not null)
            """)
    Long countConflictRequired(@Param("reviewerId") Long reviewerId);

    @Select("""
            select count(1)
            from review_records
            where reviewer_id = #{reviewerId}
              and action in ('APPROVE', 'REJECT', 'RESOLVE_CONFLICT', 'MARK_MANUAL_REQUIRED')
              and created_at >= #{todayStart}
              and created_at < #{tomorrowStart}
            """)
    Long countTodayReviewed(@Param("reviewerId") Long reviewerId,
                            @Param("todayStart") LocalDateTime todayStart,
                            @Param("tomorrowStart") LocalDateTime tomorrowStart);

    @Select("""
            select coalesce(sum(case when action = 'APPROVE' then 1 else 0 end), 0) as approvedCount,
                   coalesce(sum(case when action = 'REJECT' then 1 else 0 end), 0) as rejectedCount
            from review_records
            where reviewer_id = #{reviewerId}
              and action in ('APPROVE', 'REJECT')
            """)
    ReviewCountRow selectTotalReviewCounts(@Param("reviewerId") Long reviewerId);

    @Select("""
            select count(distinct s.id)
            from ai_review_results ar
            inner join submissions s on s.id = ar.submission_id
            left join review_tasks rt on rt.submission_id = s.id
              and rt.assigned_reviewer_id = #{reviewerId}
              and rt.status in ('PENDING', 'IN_REVIEW')
            where (s.assigned_reviewer_id = #{reviewerId} or rt.id is not null)
              and (
                ar.status = 'MANUAL_REQUIRED'
                or ar.decision = 'MANUAL_REVIEW'
                or s.conflict_status = 'CONFLICTED'
              )
            """)
    Long countAiAttention(@Param("reviewerId") Long reviewerId);

    @Select("""
            select coalesce(sum(case when ar.decision = 'PASS' then 1 else 0 end), 0) as aiPassCount,
                   coalesce(sum(case when ar.decision = 'REJECT' then 1 else 0 end), 0) as aiRejectCount,
                   coalesce(sum(case
                     when ar.status = 'MANUAL_REQUIRED' or ar.decision = 'MANUAL_REVIEW'
                     then 1 else 0
                   end), 0) as manualReviewCount,
                   coalesce(sum(case
                     when (ar.decision = 'PASS' and rr.action = 'REJECT')
                       or (ar.decision = 'REJECT' and rr.action = 'APPROVE')
                     then 1 else 0
                   end), 0) as overriddenCount
            from ai_review_results ar
            inner join submissions s on s.id = ar.submission_id
            left join review_tasks rt on rt.submission_id = s.id
              and rt.assigned_reviewer_id = #{reviewerId}
            left join review_records rr on rr.submission_id = s.id
              and rr.reviewer_id = #{reviewerId}
              and rr.action in ('APPROVE', 'REJECT')
            where s.assigned_reviewer_id = #{reviewerId}
               or rt.id is not null
            """)
    AiReviewSummaryRow selectAiReviewSummary(@Param("reviewerId") Long reviewerId);

    @Select("""
            select date(created_at) as statDate,
                   count(1) as reviewedCount,
                   coalesce(sum(case when action = 'APPROVE' then 1 else 0 end), 0) as approvedCount,
                   coalesce(sum(case when action = 'REJECT' then 1 else 0 end), 0) as rejectedCount
            from review_records
            where reviewer_id = #{reviewerId}
              and action in ('APPROVE', 'REJECT', 'RESOLVE_CONFLICT', 'MARK_MANUAL_REQUIRED')
              and created_at >= #{startAt}
              and created_at < #{endExclusive}
            group by date(created_at)
            order by statDate asc
            """)
    List<ReviewTrendRow> selectReviewTrend(@Param("reviewerId") Long reviewerId,
                                           @Param("startAt") LocalDateTime startAt,
                                           @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            select reviewId,
                   submissionId,
                   taskId,
                   taskTitle,
                   type,
                   level,
                   description,
                   targetPath
            from (
                select rt.id as reviewId,
                       rt.submission_id as submissionId,
                       rt.task_id as taskId,
                       t.title as taskTitle,
                       'OVERDUE_REVIEW' as type,
                       'WARNING' as level,
                       '该提交已超过 24 小时未审核' as description,
                       concat('/app/reviewer/tasks/', rt.id) as targetPath,
                       rt.assigned_at as sortAt
                from review_tasks rt
                inner join tasks t on t.id = rt.task_id
                where rt.assigned_reviewer_id = #{reviewerId}
                  and rt.status in ('PENDING', 'IN_REVIEW')
                  and (rt.due_at < #{now} or (rt.due_at is null and rt.assigned_at < #{timeoutAt}))
                union all
                select rt.id as reviewId,
                       s.id as submissionId,
                       s.task_id as taskId,
                       t.title as taskTitle,
                       'AI_MANUAL_REQUIRED' as type,
                       'WARNING' as level,
                       'AI 结果需要人工复核' as description,
                       concat('/app/reviewer/tasks/', rt.id) as targetPath,
                       ar.updated_at as sortAt
                from ai_review_results ar
                inner join submissions s on s.id = ar.submission_id
                inner join tasks t on t.id = s.task_id
                left join review_tasks rt on rt.submission_id = s.id
                  and rt.assigned_reviewer_id = #{reviewerId}
                  and rt.status in ('PENDING', 'IN_REVIEW')
                where (s.assigned_reviewer_id = #{reviewerId} or rt.id is not null)
                  and (ar.status = 'MANUAL_REQUIRED' or ar.decision = 'MANUAL_REVIEW')
                union all
                select rt.id as reviewId,
                       s.id as submissionId,
                       s.task_id as taskId,
                       t.title as taskTitle,
                       'CONFLICT_REQUIRED' as type,
                       'WARNING' as level,
                       '该任务存在待处理冲突' as description,
                       '/app/reviewer/queue' as targetPath,
                       cg.updated_at as sortAt
                from conflict_groups cg
                inner join submissions s on s.task_id = cg.task_id
                  and s.dataset_item_id = cg.dataset_item_id
                inner join tasks t on t.id = s.task_id
                left join review_tasks rt on rt.submission_id = s.id
                  and rt.assigned_reviewer_id = #{reviewerId}
                  and rt.status in ('PENDING', 'IN_REVIEW')
                where cg.status = 'CONFLICTED'
                  and (s.assigned_reviewer_id = #{reviewerId} or rt.id is not null)
            ) attention
            order by sortAt desc
            limit #{limit}
            """)
    List<AttentionItemRow> selectAttentionItems(@Param("reviewerId") Long reviewerId,
                                                @Param("now") LocalDateTime now,
                                                @Param("timeoutAt") LocalDateTime timeoutAt,
                                                @Param("limit") int limit);

    @Select("""
            select rr.id as reviewId,
                   rr.submission_id as submissionId,
                   t.title as taskTitle,
                   coalesce(nullif(u.display_name, ''), u.username) as labelerName,
                   rr.action as result,
                   rr.created_at as reviewedAt
            from review_records rr
            inner join submissions s on s.id = rr.submission_id
            inner join tasks t on t.id = s.task_id
            inner join users u on u.id = s.labeler_id
            where rr.reviewer_id = #{reviewerId}
              and rr.action in ('APPROVE', 'REJECT', 'RESOLVE_CONFLICT', 'MARK_MANUAL_REQUIRED')
            order by rr.created_at desc
            limit #{limit}
            """)
    List<ReviewerDashboardOverviewResponse.RecentReviewed> selectRecentReviewed(
            @Param("reviewerId") Long reviewerId,
            @Param("limit") int limit);

    record ReviewCountRow(Long approvedCount, Long rejectedCount) {
    }

    record AiReviewSummaryRow(Long aiPassCount, Long aiRejectCount, Long manualReviewCount, Long overriddenCount) {
    }

    record ReviewTrendRow(LocalDate statDate, Long reviewedCount, Long approvedCount, Long rejectedCount) {
    }

    record AttentionItemRow(Long reviewId,
                            Long submissionId,
                            Long taskId,
                            String taskTitle,
                            String type,
                            String level,
                            String description,
                            String targetPath) {
    }
}
