package com.labelhub.modules.submission.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * BE-A 导出快照只读 Mapper。
 *
 * <p>该 Mapper 只读查询 APPROVED 金标提交，禁止用于推进 submission 状态。</p>
 */
@Mapper
public interface SubmissionExportMapper {

    @Select("""
            <script>
            select s.id as submissionId,
                   s.dataset_item_id as datasetItemId,
                   di.item_json as itemJson,
                   s.answer_json as answerJson,
                   case when #{includeAiReview} and ar.id is not null then cast(json_object(
                       'status', ar.status,
                       'decision', ar.decision,
                       'averageScore', ar.average_score,
                       'dimensionScores', ar.dimension_scores,
                       'riskFlags', ar.risk_flags,
                       'suggestion', ar.suggestion,
                       'promptSnapshot', ar.prompt_snapshot,
                       'providerId', ar.provider_id,
                       'modelName', ar.model_name,
                       'retryCount', ar.retry_count,
                       'createdAt', ar.created_at,
                       'updatedAt', ar.updated_at
                   ) as char) else null end as aiReviewJson,
                   case when #{includeReviewComment} then (
                       select rr.comment
                       from review_records rr
                       where rr.submission_id = s.id
                         and rr.comment is not null
                       order by rr.created_at desc, rr.id desc
                       limit 1
                   ) else null end as reviewComment,
                   u.id as labelerId,
                   u.username as username,
                   u.display_name as displayName,
                   u.email as email,
                   s.submitted_at as submittedAt
            from submissions s
            join dataset_items di on di.id = s.dataset_item_id
            join users u on u.id = s.labeler_id
            left join ai_review_results ar on ar.submission_id = s.id
            where s.task_id = #{taskId}
              and s.status = 'APPROVED'
              and s.is_golden = 1
              <if test="afterSubmissionId != null">
                and s.id &gt; #{afterSubmissionId}
              </if>
            order by s.id asc
            limit #{pageSize}
            </script>
            """)
    List<ExportableSubmissionRecord> selectExportableGoldenSubmissions(@Param("taskId") Long taskId,
                                                                       @Param("afterSubmissionId") Long afterSubmissionId,
                                                                       @Param("pageSize") int pageSize,
                                                                       @Param("includeAiReview") boolean includeAiReview,
                                                                       @Param("includeReviewComment") boolean includeReviewComment);

    @Select("""
            <script>
            select a.id as auditId,
                   a.biz_id as submissionId,
                   a.action as action,
                   a.trace_id as traceId,
                   a.created_at as createdAt
            from audit_logs a
            where a.biz_type = 'SUBMISSION'
              and a.biz_id in
            <foreach collection="submissionIds" item="submissionId" open="(" separator="," close=")">
                #{submissionId}
            </foreach>
            order by a.created_at asc, a.id asc
            </script>
            """)
    List<AuditRefRecord> selectAuditRefs(@Param("submissionIds") List<Long> submissionIds);
}
