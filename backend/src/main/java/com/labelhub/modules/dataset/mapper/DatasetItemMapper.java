package com.labelhub.modules.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 数据集题目表 Mapper —— 所有 dataset_items 表查询的唯一入口。
 */
@Mapper
public interface DatasetItemMapper extends BaseMapper<DatasetItem> {

    // ==================== 市场/领取相关 ====================

    @Select("""
            SELECT di.id
            FROM dataset_items di
            WHERE di.task_id = #{taskId}
              AND di.deleted = 0
              AND di.assigned_count = 0
              AND NOT EXISTS (
                SELECT 1
                FROM assignments a
                WHERE a.dataset_item_id = di.id
                  AND a.status != 'CANCELLED'
              )
            ORDER BY di.id
            LIMIT 1
            """)
    Long selectClaimableItemId(@Param("taskId") Long taskId,
                               @Param("labelerId") Long labelerId,
                               @Param("overlapCount") Integer overlapCount);

    @Update("""
            UPDATE dataset_items
            SET assigned_count = 1
            WHERE id = #{datasetItemId}
              AND deleted = 0
              AND assigned_count = 0
            """)
    int reserveIfAvailable(@Param("datasetItemId") Long datasetItemId,
                           @Param("overlapCount") Integer overlapCount);

    @Select("""
            SELECT COUNT(1)
            FROM dataset_items di
            WHERE di.task_id = #{taskId}
              AND di.deleted = 0
              AND di.assigned_count = 0
              AND NOT EXISTS (
                SELECT 1
                FROM assignments a
                WHERE a.dataset_item_id = di.id
                  AND a.status != 'CANCELLED'
              )
            """)
    Integer countAvailableForLabeler(@Param("taskId") Long taskId,
                                     @Param("labelerId") Long labelerId,
                                     @Param("overlapCount") Integer overlapCount);

    @Select("""
            SELECT di.*
            FROM dataset_items di
            WHERE di.task_id = #{taskId}
              AND di.deleted = 0
              AND di.assigned_count = 0
              AND NOT EXISTS (
                SELECT 1
                FROM assignments a
                WHERE a.dataset_item_id = di.id
                  AND a.status != 'CANCELLED'
              )
            ORDER BY di.id
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<DatasetItem> selectClaimableItems(@Param("taskId") Long taskId,
                                           @Param("labelerId") Long labelerId,
                                           @Param("overlapCount") Integer overlapCount,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    // ==================== Owner 管理/批量编辑 ====================

    @Select("""
            select count(1) from dataset_items
            where task_id = #{taskId}
              and external_id = #{externalId}
              and deleted = 0
            """)
    int countActiveByTaskIdAndExternalId(@Param("taskId") Long taskId,
                                         @Param("externalId") String externalId);

    @Select("""
            <script>
            select di.*,
                   a.labeler_id as labeler_id,
                   a.status as assignment_status
            from dataset_items di
            left join assignments a on a.dataset_item_id = di.id
                                      and a.status != 'CANCELLED'
            where di.task_id = #{taskId}
              and di.deleted = 0
              <if test="externalId != null and externalId != ''">
                and di.external_id like concat('%', #{externalId}, '%')
              </if>
            order by di.id asc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<DatasetItem> selectActivePage(@Param("taskId") Long taskId,
                                       @Param("externalId") String externalId,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);

    @Select("""
            <script>
            select count(1) from dataset_items
            where task_id = #{taskId}
              and deleted = 0
              <if test="externalId != null and externalId != ''">
                and external_id like concat('%', #{externalId}, '%')
              </if>
            </script>
            """)
    long countActivePage(@Param("taskId") Long taskId,
                         @Param("externalId") String externalId);

    @Select("""
            select * from dataset_items
            where task_id = #{taskId}
              and external_id = #{externalId}
              and deleted = 0
            limit 1
            """)
    DatasetItem selectActiveByTaskIdAndExternalId(@Param("taskId") Long taskId,
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

    @Update("""
            update dataset_items
            set deleted = 1
            where task_id = #{taskId}
              and deleted = 0
            """)
    int softDeleteActiveByTaskId(@Param("taskId") Long taskId);

    @Update("""
            update dataset_items
            set deleted = 1
            where id = #{itemId}
              and deleted = 0
              and assigned_count = 0
              and submitted_count = 0
            """)
    int softDeleteById(@Param("itemId") Long itemId);

    // ==================== 领取预留 ====================

    @Select("""
            select di.*
            from dataset_items di
            where di.task_id = #{taskId}
              and di.deleted = 0
              and di.assigned_count = 0
              and not exists (
                select 1
                from assignments a
                where a.dataset_item_id = di.id
                  and a.status != 'CANCELLED'
              )
            order by di.id asc
            limit 1
            """)
    DatasetItem selectClaimableItem(@Param("taskId") Long taskId);

    @Update("""
            update dataset_items
            set assigned_count = 1
            where id = #{itemId}
              and deleted = 0
              and assigned_count = 0
            """)
    int markAssignedIfUnassigned(@Param("itemId") Long itemId);

    // ==================== 计数递增/递减 ====================

    @Update("""
            update dataset_items
            set submitted_count = submitted_count + 1
            where id = #{itemId}
              and deleted = 0
            """)
    int increaseSubmittedCount(@Param("itemId") Long itemId);

    @Update("""
            update dataset_items
            set approved_count = approved_count + 1
            where id = #{itemId}
              and deleted = 0
            """)
    int increaseApprovedCount(@Param("itemId") Long itemId);

    @Update("""
            UPDATE dataset_items
            SET assigned_count = assigned_count - 1
            WHERE id = #{datasetItemId}
              AND deleted = 0
              AND assigned_count > 0
            """)
    int decreaseAssignedCount(@Param("datasetItemId") Long datasetItemId);

    @Select("""
            SELECT COUNT(1)
            FROM dataset_items
            WHERE task_id = #{taskId}
              AND deleted = 0
            """)
    int countByTaskId(@Param("taskId") Long taskId);
}
