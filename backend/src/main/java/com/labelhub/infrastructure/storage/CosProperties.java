package com.labelhub.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "labelhub.cos")
public record CosProperties(String secretId,
                            String secretKey,
                            String region,
                            String bucket,
                            String endpoint) {
}
