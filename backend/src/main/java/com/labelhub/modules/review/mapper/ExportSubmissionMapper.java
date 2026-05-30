package com.labelhub.modules.review.mapper;

import com.labelhub.modules.review.dto.ExportGoldenItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExportSubmissionMapper {

    @Results({
            @Result(property = "submissionId",  column = "submission_id"),
            @Result(property = "taskId",        column = "task_id"),
            @Result(property = "datasetItemId", column = "dataset_item_id"),
            @Result(property = "itemJsonRef",   column = "item_json_ref"),
            @Result(property = "labelerId",     column = "labeler_id"),
            @Result(property = "versionNo",     column = "version_no"),
            @Result(property = "answerJson",    column = "answer_json"),
            @Result(property = "aiDecision",    column = "ai_decision"),
            @Result(property = "aiSummary",     column = "ai_summary"),
            @Result(property = "reviewSummary", column = "review_summary"),
            @Result(property = "auditRef",      column = "audit_ref")
    })
    @Select("""
            SELECT s.id              AS submission_id,
                   s.task_id         AS task_id,
                   s.dataset_item_id AS dataset_item_id,
                   di.item_json      AS item_json_ref,
                   s.labeler_id      AS labeler_id,
                   s.version_no      AS version_no,
                   s.answer_json     AS answer_json,
                   arr.decision      AS ai_decision,
                   arr.raw_response  AS ai_summary,
                   COALESCE(rr.reason, rr.comment) AS review_summary,
                   al.id             AS audit_ref
            FROM submissions s
            JOIN dataset_items di ON di.id = s.dataset_item_id
            LEFT JOIN ai_review_results arr ON arr.submission_id = s.id
            LEFT JOIN review_records rr ON rr.id = (
                SELECT rr2.id
                FROM review_records rr2
                WHERE rr2.submission_id = s.id
                ORDER BY rr2.created_at DESC, rr2.id DESC
                LIMIT 1
            )
            LEFT JOIN audit_logs al ON al.id = (
                SELECT al2.id
                FROM audit_logs al2
                WHERE al2.biz_type = 'SUBMISSION'
                  AND al2.biz_id = s.id
                  AND al2.action IN ('CONFLICT_RESOLVED', 'SUBMISSION_APPROVED')
                ORDER BY CASE WHEN al2.action = 'CONFLICT_RESOLVED' THEN 0 ELSE 1 END,
                         al2.created_at DESC,
                         al2.id DESC
                LIMIT 1
            )
            WHERE s.task_id = #{taskId}
              AND s.status = 'APPROVED'
              AND s.is_golden = 1
              AND s.id > #{lastId}
            ORDER BY s.id ASC
            LIMIT #{limit}
            """)
    List<ExportGoldenItem> selectGoldenPage(@Param("taskId") Long taskId,
                                             @Param("lastId") Long lastId,
                                             @Param("limit") int limit);
}
