package com.labelhub.modules.ai.service;

public interface AiReviewDispatcher {

    void enqueue(Long submissionId);
}
