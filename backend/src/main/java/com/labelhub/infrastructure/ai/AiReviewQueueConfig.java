package com.labelhub.infrastructure.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiReviewQueueProperties.class)
public class AiReviewQueueConfig {
}
