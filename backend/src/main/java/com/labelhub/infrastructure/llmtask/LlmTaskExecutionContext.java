package com.labelhub.infrastructure.llmtask;

/**
 * LLM 任务执行上下文，通过 {@link ThreadLocal} 在异步任务线程中传播 trace ID。
 *
 * <p>使用场景：LLM 任务通过 Redis Stream 异步分发，不在 HTTP 请求线程中执行。
 * 入队时从 {@code LlmTaskQueueMessage} 携带 traceId，Worker 线程通过
 * {@link #runWithTraceId(String, Runnable)} 注入该 ID，
 * 使 {@link com.labelhub.common.web.RequestTraceIdProvider} 在非 HTTP 上下文中也能返回一致的 trace ID。
 *
 * <p>典型用法：
 * <pre>{@code
 * LlmTaskExecutionContext.runWithTraceId(message.traceId(), () -> {
 *     // handler 执行期间，TraceIdProvider.currentTraceId() 返回该 traceId
 *     handler.handle(message);
 * });
 * }</pre>
 */
public final class LlmTaskExecutionContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private LlmTaskExecutionContext() {
    }

    /** 返回当前线程注入的 trace ID，可能为 null。 */
    public static String currentTraceId() {
        return TRACE_ID.get();
    }

    /**
     * 在指定 trace ID 上下文中执行 action，执行完毕后恢复原值。
     *
     * @param traceId 要注入的 trace ID；为空时清除当前线程的 trace ID
     * @param action  待执行的任务
     */
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
