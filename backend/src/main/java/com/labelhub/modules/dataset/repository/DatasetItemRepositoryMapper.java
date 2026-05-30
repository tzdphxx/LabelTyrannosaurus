package com.labelhub.modules.dataset.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.dataset.domain.DatasetItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 数据集题目表 Mapper。
 */
@Mapper
public interface DatasetItemRepositoryMapper extends BaseMapper<DatasetItemEntity> {

    /**
     * 查询同一任务下未删除题目的 externalId 是否已存在。
     */
    @Select("""
            select count(1) from dataset_items
            where task_id = #{taskId}
              and external_id = #{externalId}
              and deleted = 0
    """)
    int countActiveByTaskIdAndExternalId(@Param("taskId") Long taskId, @Param("externalId") String externalId);

    /**
     * 分页查询任务下未删除题目，供 Owner 数据资产页面展示。
     */
    @Select("""
            <script>
            select * from dataset_items
            where task_id = #{taskId}
              and deleted = 0
              <if test="datasetType != null and datasetType != ''">
                and dataset_type = #{datasetType}
              </if>
              <if test="externalId != null and externalId != ''">
                and external_id like concat('%', #{externalId}, '%')
              </if>
            order by id asc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<DatasetItemEntity> selectActivePage(@Param("taskId") Long taskId,
                                             @Param("datasetType") String datasetType,
                                             @Param("externalId") String externalId,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);

    /**
     * 统计任务下未删除题目数量，过滤条件必须和分页查询保持一致。
     */
    @Select("""
            <script>
            select count(1) from dataset_items
            where task_id = #{taskId}
              and deleted = 0
              <if test="datasetType != null and datasetType != ''">
                and dataset_type = #{datasetType}
              </if>
              <if test="externalId != null and externalId != ''">
                and external_id like concat('%', #{externalId}, '%')
              </if>
            </script>
            """)
    long countActivePage(@Param("taskId") Long taskId,
                         @Param("datasetType") String datasetType,
                         @Param("externalId") String externalId);

    /**
     * 查询同一任务下未删除的业务题号，用于批量追加前判重。
     */
    @Select("""
            select * from dataset_items
            where task_id = #{taskId}
              and external_id = #{externalId}
              and deleted = 0
            limit 1
            """)
    DatasetItemEntity selectActiveByTaskIdAndExternalId(@Param("taskId") Long taskId,
                                                        @Param("externalId") String externalId);

    @Update("""
            update dataset_items
            set item_json = #{itemJson},
                metadata_json = #{metadataJson}
            where id = #{itemId}
              and task_id = #{taskId}
              and deleted = 0
              and assigned_count = 0
              and submitted_count = 0
            """)
    int updateEditableJsonById(@Param("itemId") Long itemId,
                               @Param("taskId") Long taskId,
                               @Param("itemJson") String itemJson,
                               @Param("metadataJson") String metadataJson);

    /**
     * 覆盖导入前软删除任务下现有题目，保留历史记录用于审计和回溯。
     */
    @Update("""
            update dataset_items
            set deleted = 1
            where task_id = #{taskId}
              and deleted = 0
            """)
    int softDeleteActiveByTaskId(@Param("taskId") Long taskId);

    /**
     * 单题软删除。数据资产不物理删除，历史题目仍可用于审计和导入回溯。
     */
    @Update("""
            update dataset_items
            set deleted = 1
            where id = #{itemId}
              and deleted = 0
              and assigned_count = 0
              and submitted_count = 0
            """)
    int softDeleteById(@Param("itemId") Long itemId);

    /**
     * 查询一个可领取题目。BE-B 只负责数据集预留，不创建 BE-A 的 assignment。
     */
    @Select("""
            select * from dataset_items
            where task_id = #{taskId}
              and deleted = 0
              and assigned_count < #{overlapLimit}
            order by assigned_count asc, id asc
            limit 1
            """)
    DatasetItemEntity selectClaimableItem(@Param("taskId") Long taskId, @Param("overlapLimit") int overlapLimit);

    /**
     * 原子递增领取计数，使用 overlapLimit 防止并发领取超发。
     */
    @Update("""
            update dataset_items
            set assigned_count = assigned_count + 1
            where id = #{itemId}
              and deleted = 0
              and assigned_count < #{overlapLimit}
            """)
    int increaseAssignedCount(@Param("itemId") Long itemId, @Param("overlapLimit") int overlapLimit);

    /**
     * 明确由 BE-A 提交成功入口调用，BE-B 不私自读取 submission 状态推断。
     */
    @Update("""
            update dataset_items
            set submitted_count = submitted_count + 1
            where id = #{itemId}
              and deleted = 0
            """)
    int increaseSubmittedCount(@Param("itemId") Long itemId);

    /**
     * 明确由奖励或审核事件消费入口调用，BE-B 不直接推进审核状态。
     */
    @Update("""
            update dataset_items
            set approved_count = approved_count + 1
            where id = #{itemId}
              and deleted = 0
            """)
    int increaseApprovedCount(@Param("itemId") Long itemId);
}
