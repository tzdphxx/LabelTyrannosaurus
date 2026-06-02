package com.labelhub.modules.media.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("dataset_item_media_contexts")
public class DatasetItemMediaContextEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long datasetItemId;
    private Long taskId;
    private String mediaType;
    private String processingStatus;
    private String contextJson;
    private String limitationsJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDatasetItemId() { return datasetItemId; }
    public void setDatasetItemId(Long datasetItemId) { this.datasetItemId = datasetItemId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public String getLimitationsJson() { return limitationsJson; }
    public void setLimitationsJson(String limitationsJson) { this.limitationsJson = limitationsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
