package com.labelhub.modules.reward.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.reward.domain.RewardLedgerEntity;
import com.labelhub.modules.reward.dto.RewardLedgerResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 奖励流水 Mapper。流水表只追加，幂等由业务预查和数据库唯一约束共同兜底。
 */
@Mapper
public interface RewardLedgerMapper extends BaseMapper<RewardLedgerEntity> {

    @Select("""
            select id from reward_ledger
            where source_event_id = #{sourceEventId}
            limit 1
            """)
    Long selectIdBySourceEventId(@Param("sourceEventId") String sourceEventId);

    @Select("""
            select id from reward_ledger
            where direction = 'CREDIT'
              and submission_id = #{submissionId}
            limit 1
            """)
    Long selectPositiveIdBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("""
            select id from reward_ledger
            where direction = 'CREDIT'
              and assignment_id = #{assignmentId}
            limit 1
            """)
    Long selectPositiveIdByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select("""
            select * from reward_ledger
            where direction = 'CREDIT'
              and submission_id = #{submissionId}
            order by id desc
            limit 1
            """)
    RewardLedgerEntity selectLatestPositiveBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("""
            select id as ledgerId,
                   task_id as taskId,
                   submission_id as submissionId,
                   assignment_id as assignmentId,
                   amount,
                   direction,
                   reason,
                   source_event_id as sourceEventId,
                   reward_type as rewardType,
                   created_at as createdAt
            from reward_ledger
            where labeler_id = #{labelerId}
            order by created_at desc, id desc
            limit #{limit} offset #{offset}
            """)
    List<RewardLedgerResponse> selectLedgerPage(@Param("labelerId") Long labelerId,
                                                @Param("limit") int limit,
                                                @Param("offset") int offset);
}
