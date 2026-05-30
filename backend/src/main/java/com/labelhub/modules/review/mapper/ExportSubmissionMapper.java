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
            @Result(property = "datasetItemId", column = "dataset_item_id"),
            @Result(property = "labelerId",     column = "labeler_id"),
            @Result(property = "versionNo",     column = "version_no"),
            @Result(property = "answerJson",    column = "answer_json"),
            @Result(property = "aiDecision",    column = "ai_decision"),
            @Result(property = "aiSummary",     column = "ai_summary"),
            @Result(property = "auditLogId",    column = "audit_log_id")
    })
    @Select("""
            SELECT s.id              AS submission_id,
                   s.dataset_item_id AS dataset_item_id,
                   s.labeler_id      AS labeler_id,
                   s.version_no      AS version_no,
                   s.answer_json     AS answer_json,
                   arr.ai_decision   AS ai_decision,
                   arr.raw_response  AS ai_summary,
                   al.id             AS audit_log_id
            FROM submissions s
            LEFT JOIN ai_review_results arr ON arr.submission_id = s.id
            LEFT JOIN audit_logs al ON al.biz_type = 'SUBMISSION'
                                    AND al.biz_id = s.id
                                    AND al.action = 'SUBMISSION_APPROVED'
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
