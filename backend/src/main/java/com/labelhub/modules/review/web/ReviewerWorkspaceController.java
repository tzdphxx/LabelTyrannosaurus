package com.labelhub.modules.review.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.review.dto.ReviewerAiReviewStatusItem;
import com.labelhub.modules.review.dto.ReviewerDashboardResponse;
import com.labelhub.modules.review.dto.ReviewerTaskItemPageResponse;
import com.labelhub.modules.review.dto.ReviewerTaskSummary;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.review.mapper.ReviewerSubmissionListMapper;
import com.labelhub.modules.review.service.ReviewerTaskItemQueryService;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviewer")
@Tag(name = "审核员工作台", description = "审核员任务导航和工作统计")
public class ReviewerWorkspaceController {

    private final ReviewerSubmissionListMapper reviewerListMapper;
    private final SubmissionMapper submissionMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final ReviewerTaskItemQueryService taskItemQueryService;

    public ReviewerWorkspaceController(ReviewerSubmissionListMapper reviewerListMapper,
                                       SubmissionMapper submissionMapper,
                                       ReviewRecordMapper reviewRecordMapper,
                                       ReviewerTaskItemQueryService taskItemQueryService) {
        this.reviewerListMapper = reviewerListMapper;
        this.submissionMapper = submissionMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.taskItemQueryService = taskItemQueryService;
    }

    @GetMapping("/tasks")
    @Operation(summary = "审核员任务列表",
            description = "查看有待审提交的任务列表，作为审核工作台入口导航。")
    public ApiResponse<List<ReviewerTaskSummary>> tasks() {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        Long reviewerId = CurrentUserContext.getUserId();
        return ApiResponse.ok(reviewerListMapper.selectTaskSummariesForReviewer(reviewerId));
    }

    @GetMapping("/tasks/{taskId}/items")
    @Operation(summary = "审核员任务题目分页详情",
            description = "分页查看当前审核员已领取或已分配任务下的全部题目及题目审核状态。")
    public ApiResponse<ReviewerTaskItemPageResponse> taskItems(
            @PathVariable Long taskId,
            @RequestParam(required = false) String itemStatus,
            @RequestParam(required = false) String submissionStatus,
            @RequestParam(required = false) String aiDecision,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        Long reviewerId = CurrentUserContext.getUserId();
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return ApiResponse.ok(taskItemQueryService.queryTaskItems(
                taskId, reviewerId, itemStatus, submissionStatus, aiDecision, keyword, safePage, safeSize));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "审核员工作台概览",
            description = "返回当前审核员的工作统计。")
    public ApiResponse<ReviewerDashboardResponse> dashboard() {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        Long reviewerId = CurrentUserContext.getUserId();
        int pending = submissionMapper.countPendingByReviewer(reviewerId);
        int todayReviewed = reviewRecordMapper.countTodayReviewed(reviewerId);
        int totalApproved = reviewRecordMapper.countTotalApproved(reviewerId);
        int totalRejected = reviewRecordMapper.countTotalRejected(reviewerId);
        BigDecimal approvalRate = (totalApproved + totalRejected) > 0
                ? BigDecimal.valueOf(totalApproved)
                    .divide(BigDecimal.valueOf(totalApproved + totalRejected), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        return ApiResponse.ok(new ReviewerDashboardResponse(
                pending, todayReviewed, totalApproved, totalRejected, approvalRate));
    }

    @GetMapping("/ai-review-status")
    @Operation(summary = "审查员 AI 预审状态列表",
            description = "获取当前审查员所有负责提交的 AI 预审状态，包含评分、决策和分配情况。")
    public ApiResponse<List<ReviewerAiReviewStatusItem>> aiReviewStatus() {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        Long reviewerId = CurrentUserContext.getUserId();
        return ApiResponse.ok(reviewerListMapper.selectAiReviewStatusForReviewer(reviewerId));
    }
}
