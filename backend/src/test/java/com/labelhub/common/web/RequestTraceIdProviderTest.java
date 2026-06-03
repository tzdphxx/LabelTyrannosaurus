package com.labelhub.common.web;

import jakarta.servlet.http.HttpServletRequest;
import com.labelhub.infrastructure.llmtask.LlmTaskExecutionContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestTraceIdProviderTest {

    @Test
    void currentTraceIdUsesRequestHeaderWhenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "trace-from-client");
        RequestTraceIdProvider provider = new RequestTraceIdProvider(providerFor(request));

        assertThat(provider.currentTraceId()).isEqualTo("trace-from-client");
    }

    @Test
    void currentTraceIdGeneratesFallbackWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestTraceIdProvider provider = new RequestTraceIdProvider(providerFor(request));

        assertThat(provider.currentTraceId()).isNotBlank();
    }

    @Test
    void currentTraceIdGeneratesFallbackWhenRequestProxyHasNoBoundRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Trace-Id"))
                .thenThrow(new IllegalStateException("No thread-bound request found"));
        RequestTraceIdProvider provider = new RequestTraceIdProvider(providerFor(request));

        assertThat(provider.currentTraceId()).isNotBlank();
    }

    @Test
    void currentTraceIdUsesQueuedTaskContextBeforeRequestHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "trace-from-request");
        RequestTraceIdProvider provider = new RequestTraceIdProvider(providerFor(request));

        LlmTaskExecutionContext.runWithTraceId("trace-from-queue", () ->
                assertThat(provider.currentTraceId()).isEqualTo("trace-from-queue"));
    }

    private static ObjectProvider<HttpServletRequest> providerFor(HttpServletRequest request) {
        return new ObjectProvider<>() {
            @Override
            public HttpServletRequest getObject(Object... args) {
                return request;
            }

            @Override
            public HttpServletRequest getIfAvailable() {
                return request;
            }

            @Override
            public HttpServletRequest getIfUnique() {
                return request;
            }

            @Override
            public HttpServletRequest getObject() {
                return request;
            }
        };
    }
}
