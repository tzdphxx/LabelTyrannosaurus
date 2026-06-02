package com.labelhub.common.web;

import jakarta.servlet.http.HttpServletRequest;
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
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request == null) {
            return UUID.randomUUID().toString();
        }
        String traceId = request.getHeader("X-Trace-Id");
        return traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId;
    }
}
