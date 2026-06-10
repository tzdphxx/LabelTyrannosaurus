package com.labelhub.infrastructure.llm;

public record OpenAiCompatibleResponse(
        boolean success,
        Integer httpStatus,
        String rawResponse,
        Long latencyMs,
        String errorMessage,
        boolean timedOut
) {

    public static OpenAiCompatibleResponse success(Integer httpStatus, String rawResponse, Long latencyMs) {
        return new OpenAiCompatibleResponse(true, httpStatus, rawResponse, latencyMs, null, false);
    }

    public static OpenAiCompatibleResponse failure(Integer httpStatus,
                                                   String rawResponse,
                                                   Long latencyMs,
                                                   String errorMessage,
                                                   boolean timedOut) {
        return new OpenAiCompatibleResponse(false, httpStatus, rawResponse, latencyMs, errorMessage, timedOut);
    }
}
