package com.labelhub.modules.dataset.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 数据集题目实体。
 *
 * <p>{@code externalId} 是同一任务内的业务唯一键；题目主体存储在 {@code itemJson}，
 * 计数类字段由领取、提交和审核流程通过明确入口递增。
 * {@code labelerId} 和 {@code assignmentStatus} 是列表查询时的计算字段，不存入数据库。</p>
 */
@Getter
@Setter
@TableName("dataset_items")
public class DatasetItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String externalId;

    private String itemJson;

    private String metadataJson;

    private Integer assignedCount;

    private Integer submittedCount;

    private Integer approvedCount;

    private Boolean deleted;

    /** 当前有效领取人，列表查询时通过 JOIN assignments 计算，非数据库字段。 */
    @TableField(exist = false)
    private Long labelerId;

    /** 当前领取状态，列表查询时通过 JOIN assignments 计算，非数据库字段。 */
    @TableField(exist = false)
    private String assignmentStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
