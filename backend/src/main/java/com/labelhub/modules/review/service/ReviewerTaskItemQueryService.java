package com.labelhub.modules.review.service;

import com.labelhub.common.api.PageResponse;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.review.dto.ReviewerTaskItemPageResponse;
import com.labelhub.modules.review.dto.ReviewerTaskItemRow;
import com.labelhub.modules.review.dto.ReviewerTaskStatusCount;
import com.labelhub.modules.review.dto.ReviewerTaskStatusSummary;
import com.labelhub.modules.review.mapper.ReviewerTaskItemMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReviewerTaskItemQueryService {

    private static final int FORBIDDEN = 403001;
    private static final int TASK_NOT_FOUND = 404001;

    private final ReviewerTaskItemMapper reviewerTaskItemMapper;
    private final TaskMapper taskMapper;

    public ReviewerTaskItemQueryService(ReviewerTaskItemMapper reviewerTaskItemMapper,
                                        TaskMapper taskMapper) {
        this.reviewerTaskItemMapper = reviewerTaskItemMapper;
        this.taskMapper = taskMapper;
    }

    public ReviewerTaskItemPageResponse queryTaskItems(Long taskId,
                                                       Long reviewerId,
                                                       String itemStatus,
                                                       String submissionStatus,
                                                       String aiDecision,
                                                       String keyword,
                                                       int page,
                                                       int size) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(TASK_NOT_FOUND, "任务不存在");
        }
        if (!canAccessTask(taskId, reviewerId)) {
            throw new BusinessException(FORBIDDEN, "无权查看该审核任务");
        }

        String normalizedItemStatus = normalize(itemStatus);
        String normalizedSubmissionStatus = normalize(submissionStatus);
        String normalizedAiDecision = normalize(aiDecision);
        String normalizedKeyword = normalize(keyword);
        int offset = (page - 1) * size;

        long total = reviewerTaskItemMapper.countTaskItems(
                taskId, normalizedItemStatus, normalizedSubmissionStatus,
                normalizedAiDecision, normalizedKeyword);
        List<ReviewerTaskItemRow> rows = reviewerTaskItemMapper.selectTaskItems(
                taskId, reviewerId, normalizedItemStatus, normalizedSubmissionStatus,
                normalizedAiDecision, normalizedKeyword, offset, size);
        ReviewerTaskStatusSummary summary = buildSummary(
                reviewerTaskItemMapper.selectStatusCounts(taskId));
        long totalItemCount = summary.unclaimedCount()
                + summary.claimedCount()
                + summary.draftCount()
                + summary.submittedCount()
                + summary.returnedCount()
                + summary.approvedCount();

        return new ReviewerTaskItemPageResponse(
                task.getId(),
                task.getTitle(),
                task.getStatus() == null ? null : task.getStatus().name(),
                totalItemCount,
                summary,
                new PageResponse<>(rows, page, size, total)
        );
    }

    private boolean canAccessTask(Long taskId, Long reviewerId) {
        return reviewerTaskItemMapper.countTaskReviewerAccess(taskId, reviewerId) > 0
                || reviewerTaskItemMapper.countSubmissionReviewerAccess(taskId, reviewerId) > 0
                || reviewerTaskItemMapper.countReviewTaskAccess(taskId, reviewerId) > 0;
    }

    private ReviewerTaskStatusSummary buildSummary(List<ReviewerTaskStatusCount> counts) {
        long unclaimed = 0;
        long claimed = 0;
        long draft = 0;
        long submitted = 0;
        long returned = 0;
        long approved = 0;
        for (ReviewerTaskStatusCount count : counts) {
            String status = count.itemStatus();
            if ("UNCLAIMED".equals(status)) {
                unclaimed += count.count();
            } else if ("CLAIMED".equals(status)) {
                claimed += count.count();
            } else if ("DRAFT".equals(status)) {
                draft += count.count();
            } else if ("SUBMITTED".equals(status)) {
                submitted += count.count();
            } else if ("RETURNED".equals(status)) {
                returned += count.count();
            } else if ("APPROVED".equals(status)) {
                approved += count.count();
            }
        }
        return new ReviewerTaskStatusSummary(unclaimed, claimed, draft, submitted, returned, approved);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
