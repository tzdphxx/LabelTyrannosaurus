package com.labelhub.modules.review.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.review.dto.ReviewerDashboardResponse;
import com.labelhub.modules.review.dto.ReviewerTaskSummary;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.review.mapper.ReviewerSubmissionListMapper;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviewer")
@Tag(name = "审核员工作台", description = "审核员任务导航和工作统计")
public class ReviewerWorkspaceController {

    private final ReviewerSubmissionListMapper reviewerListMapper;
    private final SubmissionMapper submissionMapper;
    private final ReviewRecordMapper reviewRecordMapper;

    public ReviewerWorkspaceController(ReviewerSubmissionListMapper reviewerListMapper,
                                       SubmissionMapper submissionMapper,
                                       ReviewRecordMapper reviewRecordMapper) {
        this.reviewerListMapper = reviewerListMapper;
        this.submissionMapper = submissionMapper;
        this.reviewRecordMapper = reviewRecordMapper;
    }

    @GetMapping("/tasks")
    @Operation(summary = "审核员任务列表",
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", description = "请求参数校验失败"), @ApiResponse(responseCode = "401", description = "未认证"), @ApiResponse(responseCode = "403", description = "权限不足")})
            description = "查看有待审提交的任务列表，作为审核工作台入口导航。")
    public ApiResponse<List<ReviewerTaskSummary>> tasks() {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        Long reviewerId = CurrentUserContext.getUserId();
        return ApiResponse.ok(reviewerListMapper.selectTaskSummariesForReviewer(reviewerId));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "审核员工作台概览",
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", description = "请求参数校验失败"), @ApiResponse(responseCode = "401", description = "未认证"), @ApiResponse(responseCode = "403", description = "权限不足")})
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
}