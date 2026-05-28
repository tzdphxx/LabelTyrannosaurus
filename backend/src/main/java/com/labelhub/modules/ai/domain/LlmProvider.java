package com.labelhub.modules.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("llm_providers")
public class LlmProvider {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String providerCode;

    private String providerName;

    private String baseUrl;

    private String encryptedApiKey;

    private String defaultModel;

    private String customHeadersJson;

    private Boolean enabled;

    private Integer platformRateLimitPerMinute;

    private Integer taskRateLimitPerMinute;

    private Integer userRateLimitPerMinute;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
