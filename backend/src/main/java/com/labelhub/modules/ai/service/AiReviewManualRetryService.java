package com.labelhub.modules.ai.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiReviewManualRetryService {

    private static final Logger log = LoggerFactory.getLogger(AiReviewManualRetryService.class);
    private static final int NOT_FOUND = 404710;
    private static final int NOT_RETRYABLE = 400710;
    private static final String BIZ_TYPE = "AI_REVIEW";
    private static final Set<AiReviewStatus> RETRYABLE_STATUSES = Set.of(
            AiReviewStatus.FAILED, AiReviewStatus.RATE_LIMITED, AiReviewStatus.MANUAL_REQUIRED);

    private final AiReviewResultMapper aiReviewResultMapper;
    private final SubmissionMapper submissionMapper;
    private final AiAutoReviewService aiAutoReviewService;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;

    public AiReviewManualRetryService(AiReviewResultMapper aiReviewResultMapper,
                                      SubmissionMapper submissionMapper,
                                      AiAutoReviewService aiAutoReviewService,
                                      AuditAppender auditAppender,
                                      TraceIdProvider traceIdProvider) {
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.submissionMapper = submissionMapper;
        this.aiAutoReviewService = aiAutoReviewService;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
    }

    public AiReviewResultResponse retry(Long submissionId, Long reviewerId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(NOT_FOUND, "Submission not found");
        }

        AiReviewResult existing = aiReviewResultMapper.selectBySubmissionId(submissionId);
        if (existing == null) {
            throw new BusinessException(NOT_FOUND, "AI review result not found");
        }
        if (!RETRYABLE_STATUSES.contains(existing.getStatus())) {
            throw new BusinessException(NOT_RETRYABLE,
                    "Cannot retry AI review in status: " + existing.getStatus());
        }

        // For MANUAL_REQUIRED, reset status to FAILED so retryReview() will proceed
        if (existing.getStatus() == AiReviewStatus.MANUAL_REQUIRED) {
            aiReviewResultMapper.updateForRetry(submissionId, existing.getRetryCount(),
                    AiReviewStatus.FAILED.name(), existing.getRetryCount(), null,
                    existing.getEffectiveRunId(), existing.getErrorCode(),
                    existing.getErrorMessage(), existing.getRawResponse());
        }

        appendRetryAudit(submissionId, reviewerId, existing.getEffectiveRunId());
        aiAutoReviewService.retryReview(submissionId);

        AiReviewResult updated = aiReviewResultMapper.selectBySubmissionId(submissionId);
        return aiAutoReviewService.toResponse(updated);
    }

    private void appendRetryAudit(Long submissionId, Long reviewerId, Long previousRunId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("submissionId", submissionId);
        snapshot.put("triggeredBy", reviewerId);
        snapshot.put("previousRunId", previousRunId);
        snapshot.put("action", "MANUAL_RETRY");
        auditAppender.append(new AuditCommand("USER", reviewerId,
                BIZ_TYPE, submissionId,
                "AI_REVIEW_MANUAL_RETRY", null, snapshot,
                traceIdProvider.currentTraceId(), null));
    }
}
