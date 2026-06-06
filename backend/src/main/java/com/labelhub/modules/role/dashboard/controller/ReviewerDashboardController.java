package com.labelhub.modules.role.dashboard.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.role.dashboard.dto.DashboardRange;
import com.labelhub.modules.role.dashboard.dto.ReviewerDashboardOverviewResponse;
import com.labelhub.modules.role.dashboard.service.ReviewerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviewer/dashboard")
@PreAuthorize("hasRole('REVIEWER')")
@Tag(name = "Reviewer 角色数据看板", description = "审核员查看审核队列压力与处理数据")
public class ReviewerDashboardController {

    private final ReviewerDashboardService service;

    public ReviewerDashboardController(ReviewerDashboardService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @Operation(summary = "Reviewer 看板总览", description = "返回当前审核员可见审核队列、趋势和 AI 复核摘要")
    public ApiResponse<ReviewerDashboardOverviewResponse> overview(
            @RequestParam(required = false) String range,
            HttpServletRequest request) {
        DashboardRequestGuard.rejectUserIdParams(request);
        return ApiResponse.ok(service.getOverview(DashboardRange.from(range, DashboardRange.LAST_7_DAYS)));
    }
}
