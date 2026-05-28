package com.labelhub.modules.review.service;

import com.labelhub.modules.review.domain.ReviewFlowStatus;
import com.labelhub.modules.review.domain.ReviewTask;
import com.labelhub.modules.review.domain.ReviewTaskStatus;
import com.labelhub.modules.review.mapper.ReviewTaskMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewLevelEscalationService {

    private final ReviewTaskMapper reviewTaskMapper;
    private final SubmissionMapper submissionMapper;
    private final int defaultMaxLevel;

    public ReviewLevelEscalationService(
            ReviewTaskMapper reviewTaskMapper,
            SubmissionMapper submissionMapper,
            @Value("${labelhub.review.default-max-level:1}") int defaultMaxLevel) {
        this.reviewTaskMapper = reviewTaskMapper;
        this.submissionMapper = submissionMapper;
        this.defaultMaxLevel = defaultMaxLevel;
    }

    public int getMaxReviewLevel() {
        return defaultMaxLevel;
    }

    @Transactional
    public void escalate(Submission submission, int completedLevel, Long reviewerId) {
        ReviewTask currentTask = reviewTaskMapper.selectBySubmissionAndLevel(
                submission.getId(), completedLevel);
        if (currentTask != null) {
            currentTask.setStatus(ReviewTaskStatus.APPROVED);
            currentTask.setCompletedAt(LocalDateTime.now());
            reviewTaskMapper.updateById(currentTask);
        }

        int nextLevel = completedLevel + 1;
        submission.setCurrentReviewLevel(nextLevel);
        submission.setReviewFlowStatus(ReviewFlowStatus.UNASSIGNED.name());
        submission.setAssignedReviewerId(null);
        submissionMapper.updateById(submission);
    }
}
