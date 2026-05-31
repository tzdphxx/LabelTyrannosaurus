package com.labelhub.modules.template.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("template_versions")
public class TemplateVersion {

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
}
