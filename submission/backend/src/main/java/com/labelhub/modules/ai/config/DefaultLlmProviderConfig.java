package com.labelhub.modules.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DefaultLlmProviderProperties.class)
public class DefaultLlmProviderConfig {
}
