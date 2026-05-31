package com.labelhub.modules.dataset.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("dataset_items")
public class DatasetItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String externalId;

    private DatasetType datasetType;

    private String itemJson;

    private String metadataJson;

    private Integer assignedCount;

    private Integer submittedCount;

    private Integer approvedCount;

    private Boolean deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
