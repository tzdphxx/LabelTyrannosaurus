package com.labelhub.modules.review.service;

import com.labelhub.modules.review.domain.ReviewFlowStatus;
import com.labelhub.modules.review.domain.ReviewTask;
import com.labelhub.modules.review.domain.ReviewTaskStatus;
import com.labelhub.modules.review.mapper.ReviewTaskClaimMapper;
import com.labelhub.modules.review.mapper.ReviewTaskMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewLevelEscalationService {

    private final ReviewTaskMapper reviewTaskMapper;
    private final ReviewTaskClaimMapper reviewTaskClaimMapper;
    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final int defaultMaxLevel;

    public ReviewLevelEscalationService(
            ReviewTaskMapper reviewTaskMapper,
            SubmissionMapper submissionMapper,
            TaskMapper taskMapper,
            ReviewTaskClaimMapper reviewTaskClaimMapper,
            @Value("${labelhub.review.default-max-level:1}") int defaultMaxLevel) {
        this.reviewTaskMapper = reviewTaskMapper;
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
        this.reviewTaskClaimMapper = reviewTaskClaimMapper;
        this.defaultMaxLevel = defaultMaxLevel;
    }

    public int getMaxReviewLevel(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task != null && task.getReviewLevelCount() != null) {
            return task.getReviewLevelCount();
        }
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
        // 若下一级已被某审核员整任务领取，直接归属；否则回到未分配池。
        Long nextClaimant = reviewTaskClaimMapper.selectReviewerForTaskLevel(
                submission.getTaskId(), nextLevel);
        if (nextClaimant != null) {
            submission.setReviewFlowStatus(ReviewFlowStatus.ASSIGNED.name());
            submission.setAssignedReviewerId(nextClaimant);
        } else {
            submission.setReviewFlowStatus(ReviewFlowStatus.UNASSIGNED.name());
            submission.setAssignedReviewerId(null);
        }
        submissionMapper.updateById(submission);
    }
}
