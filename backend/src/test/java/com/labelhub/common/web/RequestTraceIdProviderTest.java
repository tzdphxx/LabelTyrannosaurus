package com.labelhub.common.web;

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
    void currentTraceIdGeneratesFallbackWhenRequestProxyIsNotBound() {
        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getHeader("X-Trace-Id")).thenThrow(new IllegalStateException("No thread-bound request found"));
        RequestTraceIdProvider provider = new RequestTraceIdProvider(providerFor(request));

        assertThat(provider.currentTraceId()).isNotBlank();
    }

    private static ObjectProvider<jakarta.servlet.http.HttpServletRequest> providerFor(
            jakarta.servlet.http.HttpServletRequest request) {
        return new ObjectProvider<>() {
            @Override
            public jakarta.servlet.http.HttpServletRequest getObject(Object... args) {
                return request;
            }

            @Override
            public jakarta.servlet.http.HttpServletRequest getIfAvailable() {
                return request;
            }

            @Override
            public jakarta.servlet.http.HttpServletRequest getIfUnique() {
                return request;
            }

            @Override
            public jakarta.servlet.http.HttpServletRequest getObject() {
                return request;
            }
        };
    }
}
