package com.labelhub.modules.media.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("media_derivatives")
public class MediaDerivativeEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private String derivativeType;
    private Integer sequenceNo;
    private Long sourceFileId;
    private String url;
    private String textJson;
    private String metadataJson;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public String getDerivativeType() { return derivativeType; }
    public void setDerivativeType(String derivativeType) { this.derivativeType = derivativeType; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
    public Long getSourceFileId() { return sourceFileId; }
    public void setSourceFileId(Long sourceFileId) { this.sourceFileId = sourceFileId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTextJson() { return textJson; }
    public void setTextJson(String textJson) { this.textJson = textJson; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
