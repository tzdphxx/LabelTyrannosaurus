package com.labelhub.infrastructure.llmtask;

public final class LlmTaskExecutionContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private LlmTaskExecutionContext() {
    }

    public static String currentTraceId() {
        return TRACE_ID.get();
    }

    public static void runWithTraceId(String traceId, Runnable action) {
        String previous = TRACE_ID.get();
        if (traceId == null || traceId.isBlank()) {
            TRACE_ID.remove();
        } else {
            TRACE_ID.set(traceId);
        }
        try {
            action.run();
        } finally {
            if (previous == null) {
                TRACE_ID.remove();
            } else {
                TRACE_ID.set(previous);
            }
        }
    }
}
