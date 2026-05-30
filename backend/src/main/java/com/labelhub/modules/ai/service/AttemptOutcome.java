package com.labelhub.modules.ai.service;

import com.labelhub.modules.ai.domain.AiReviewResult;
import java.util.Map;

final class AttemptOutcome {

    private final boolean succeeded;
    private final AiReviewResult result;
    private final Map<String, Object> responseSnapshot;
    private final String errorCode;
    private final String errorMessage;
    private final String rawResponse;

    private AttemptOutcome(boolean succeeded, AiReviewResult result,
                           Map<String, Object> responseSnapshot,
                           String errorCode, String errorMessage, String rawResponse) {
        this.succeeded = succeeded;
        this.result = result;
        this.responseSnapshot = responseSnapshot;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.rawResponse = rawResponse;
    }

    static AttemptOutcome success(AiReviewResult result, Map<String, Object> responseSnapshot) {
        return new AttemptOutcome(true, result, responseSnapshot, null, null, null);
    }

    static AttemptOutcome failure(String errorCode, String errorMessage, String rawResponse) {
        return new AttemptOutcome(false, null, null, errorCode, errorMessage, rawResponse);
    }

    boolean success() { return succeeded; }
    AiReviewResult result() { return result; }
    Map<String, Object> responseSnapshot() { return responseSnapshot; }
    String errorCode() { return errorCode; }
    String errorMessage() { return errorMessage; }
    String rawResponse() { return rawResponse; }
}
