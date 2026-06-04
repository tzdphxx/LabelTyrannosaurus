package com.labelhub.modules.role.dashboard.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.role.dashboard.dto.OwnerDashboardOverviewResponse;
import com.labelhub.modules.role.dashboard.dto.TrendDays;
import com.labelhub.modules.role.dashboard.service.OwnerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/dashboard")
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Owner 角色数据看板", description = "任务负责人查看自有任务交付数据")
public class OwnerDashboardController {

    private final OwnerDashboardService service;

    public OwnerDashboardController(OwnerDashboardService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @Operation(summary = "Owner 看板总览", description = "返回当前任务负责人自有任务的 KPI、趋势和待关注项")
    public ApiResponse<OwnerDashboardOverviewResponse> overview(
            @RequestParam(required = false) Integer trendDays,
            HttpServletRequest request) {
        DashboardRequestGuard.rejectUserIdParams(request);
        return ApiResponse.ok(service.getOverview(TrendDays.from(trendDays)));
    }
}
