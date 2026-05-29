package com.labelhub.modules.export.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 导出任务实体。该表只记录导出配置、执行状态和结果文件引用。
 */
@TableName("export_jobs")
public class ExportJobEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long requestedBy;
    private String exportFormat;
    private String status;
    private Boolean includeAiReview;
    private Boolean includeAuditTrail;
    private Boolean includeReviewComment;
    private Boolean includeLabelerInfo;
    private String fieldMappingJson;
    private Long resultFileId;
    private String downloadUrl;
    private String errorMessage;
    private String traceId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getIncludeAiReview() { return includeAiReview; }
    public void setIncludeAiReview(Boolean includeAiReview) { this.includeAiReview = includeAiReview; }
    public Boolean getIncludeAuditTrail() { return includeAuditTrail; }
    public void setIncludeAuditTrail(Boolean includeAuditTrail) { this.includeAuditTrail = includeAuditTrail; }
    public Boolean getIncludeReviewComment() { return includeReviewComment; }
    public void setIncludeReviewComment(Boolean includeReviewComment) { this.includeReviewComment = includeReviewComment; }
    public Boolean getIncludeLabelerInfo() { return includeLabelerInfo; }
    public void setIncludeLabelerInfo(Boolean includeLabelerInfo) { this.includeLabelerInfo = includeLabelerInfo; }
    public String getFieldMappingJson() { return fieldMappingJson; }
    public void setFieldMappingJson(String fieldMappingJson) { this.fieldMappingJson = fieldMappingJson; }
    public Long getResultFileId() { return resultFileId; }
    public void setResultFileId(Long resultFileId) { this.resultFileId = resultFileId; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
