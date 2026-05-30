package com.labelhub.modules.review.mapper;

import com.labelhub.modules.review.dto.SubmissionReviewItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewSubmissionMapper {

    @Results({
            @Result(property = "submissionId",     column = "submission_id"),
            @Result(property = "taskId",           column = "task_id"),
            @Result(property = "datasetItemId",    column = "dataset_item_id"),
            @Result(property = "labelerId",        column = "labeler_id"),
            @Result(property = "submissionStatus", column = "submission_status"),
            @Result(property = "aiDecision",       column = "ai_decision"),
            @Result(property = "conflictStatus",   column = "conflict_status"),
            @Result(property = "reviewLevel",      column = "review_level")
    })
    @Select("""
            SELECT s.id              AS submission_id,
                   s.task_id         AS task_id,
                   s.dataset_item_id AS dataset_item_id,
                   s.labeler_id      AS labeler_id,
                   s.status          AS submission_status,
                   arr.ai_decision   AS ai_decision,
                   NULL              AS conflict_status,
                   1                 AS review_level
            FROM submissions s
            LEFT JOIN ai_review_results arr ON arr.submission_id = s.id
            WHERE s.status = 'PENDING_FINAL'
            ORDER BY s.submitted_at ASC
            """)
    List<SubmissionReviewItem> selectPendingFinalItems();
}
