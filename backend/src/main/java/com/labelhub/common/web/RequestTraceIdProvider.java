package com.labelhub.common.web;

import jakarta.servlet.http.HttpServletRequest;
import com.labelhub.infrastructure.llmtask.LlmTaskExecutionContext;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class RequestTraceIdProvider implements TraceIdProvider {

    private final ObjectProvider<HttpServletRequest> requestProvider;

    public RequestTraceIdProvider(ObjectProvider<HttpServletRequest> requestProvider) {
        this.requestProvider = requestProvider;
    }

    @Override
    public String currentTraceId() {
        String queuedTraceId = LlmTaskExecutionContext.currentTraceId();
        if (queuedTraceId != null && !queuedTraceId.isBlank()) {
            return queuedTraceId;
        }
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request == null) {
            return newTraceId();
        }
        try {
            String traceId = request.getHeader("X-Trace-Id");
            return traceId == null || traceId.isBlank() ? newTraceId() : traceId;
        } catch (IllegalStateException ex) {
            return newTraceId();
        }
    }

    private String newTraceId() {
        return UUID.randomUUID().toString();
    }
}
