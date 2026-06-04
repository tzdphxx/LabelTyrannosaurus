package com.labelhub.infrastructure.llm;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class AiMetrics {

    private static final String UNKNOWN = "unknown";
    private static final String NONE = "none";

    private final MeterRegistry registry;

    public AiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(String bizType, Long providerId, String modelName,
                       String status, String errorCode, Long latencyMs) {
        Tags tags = tags(bizType, providerId, modelName, status, errorCode);
        registry.counter("labelhub.ai.requests", tags).increment();
        if (latencyMs != null) {
            registry.timer("labelhub.ai.latency", tags).record(Duration.ofMillis(Math.max(0L, latencyMs)));
        }
    }

    private Tags tags(String bizType, Long providerId, String modelName, String status, String errorCode) {
        return Tags.of(
                "biz_type", valueOrUnknown(bizType),
                "provider_id", providerId == null ? UNKNOWN : String.valueOf(providerId),
                "model_name", valueOrUnknown(modelName),
                "status", valueOrUnknown(status),
                "error_code", valueOrNone(errorCode)
        );
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? UNKNOWN : value;
    }

    private String valueOrNone(String value) {
        return value == null || value.isBlank() ? NONE : value;
    }
}
