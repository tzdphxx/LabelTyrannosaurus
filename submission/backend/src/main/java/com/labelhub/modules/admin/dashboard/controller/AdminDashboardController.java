package com.labelhub.modules.admin.dashboard.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardOverviewResponse;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardRange;
import com.labelhub.modules.admin.dashboard.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "管理端数据看板", description = "管理员查看平台运营聚合数据")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/overview")
    @Operation(summary = "管理端看板总览", description = "返回 ADMIN 首页需要的 KPI、趋势、排行和异常提醒")
    public ApiResponse<AdminDashboardOverviewResponse> overview(@RequestParam(required = false) String range) {
        return ApiResponse.ok(adminDashboardService.getOverview(AdminDashboardRange.from(range)));
    }
}
