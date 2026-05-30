package com.labelhub.modules.reward.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 提交快照只读 Mapper。禁止通过该 Mapper 修改 BE-A 维护的 submission 状态字段。
 */
@Mapper
public interface SubmissionSnapshotMapper {

    /**
     * 只读查询提交快照，用于金标结算和冲正补齐 assignmentId。
     */
    @Select("""
            select id as submissionId,
                   assignment_id as assignmentId,
                   task_id as taskId,
                   dataset_item_id as datasetItemId,
                   labeler_id as labelerId
            from submissions
            where id = #{submissionId}
            """)
    SubmissionSnapshot selectRewardSnapshotById(@Param("submissionId") Long submissionId);
}
