package com.labelhub.modules.review.service;

import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReviewAutoAssignScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReviewAutoAssignScheduler.class);

    private final SubmissionMapper submissionMapper;
    private final ReviewerPoolService reviewerPoolService;

    @Value("${labelhub.review.auto-assign-enabled:true}")
    private boolean enabled;

    @Value("${labelhub.review.max-pending-per-reviewer:50}")
    private int maxPendingPerReviewer;

    public ReviewAutoAssignScheduler(SubmissionMapper submissionMapper,
                                     ReviewerPoolService reviewerPoolService) {
        this.submissionMapper = submissionMapper;
        this.reviewerPoolService = reviewerPoolService;
    }

    @Scheduled(fixedDelayString = "${labelhub.review.auto-assign-delay-ms:30000}")
    public void autoAssign() {
        if (!enabled) {
            return;
        }
        List<Long> unassigned = submissionMapper.selectUnassignedPendingFinal(200);
        if (unassigned.isEmpty()) {
            return;
        }
        List<ReviewerLoad> reviewers = reviewerPoolService.getActiveReviewersWithLoad();
        if (reviewers.isEmpty()) {
            log.warn("No active reviewers available for auto-assign");
            return;
        }
        int assigned = 0;
        for (Long submissionId : unassigned) {
            ReviewerLoad target = pickLeastLoaded(reviewers);
            if (target == null || target.pendingCount >= maxPendingPerReviewer) {
                break;
            }
            int affected = submissionMapper.assignReviewer(submissionId, target.reviewerId);
            if (affected > 0) {
                target.pendingCount++;
                assigned++;
            }
        }
        if (assigned > 0) {
            log.info("Auto-assigned {} submissions to {} reviewers", assigned, reviewers.size());
        }
    }

    private ReviewerLoad pickLeastLoaded(List<ReviewerLoad> reviewers) {
        return reviewers.stream()
                .filter(r -> r.pendingCount < maxPendingPerReviewer)
                .min(Comparator.comparingInt(r -> r.pendingCount))
                .orElse(null);
    }

    public static class ReviewerLoad {
        public final Long reviewerId;
        public int pendingCount;

        public ReviewerLoad(Long reviewerId, int pendingCount) {
            this.reviewerId = reviewerId;
            this.pendingCount = pendingCount;
        }
    }
}