package com.labelhub.modules.template.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 模板版本实体。
 *
 * <p>{@code schemaJson} 是渲染器和后端校验共同消费的版本快照；发布快照不可原地修改，只能 fork 新版本。</p>
 */
@TableName("template_versions")
public class TemplateVersionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long taskId;
    private Integer versionNo;
    private String schemaJson;
    private Boolean publishedSnapshot;
    private String changeNote;
    private Long createdBy;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(String schemaJson) {
        this.schemaJson = schemaJson;
    }

    public Boolean getPublishedSnapshot() {
        return publishedSnapshot;
    }

    public void setPublishedSnapshot(Boolean publishedSnapshot) {
        this.publishedSnapshot = publishedSnapshot;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public TemplateVersionState state() {
        return Boolean.TRUE.equals(publishedSnapshot)
                ? TemplateVersionState.PUBLISHED_SNAPSHOT
                : TemplateVersionState.DRAFT;
    }
}
