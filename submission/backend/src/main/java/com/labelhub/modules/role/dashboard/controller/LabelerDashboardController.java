package com.labelhub.modules.role.dashboard.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.role.dashboard.dto.DashboardRange;
import com.labelhub.modules.role.dashboard.dto.LabelerDashboardOverviewResponse;
import com.labelhub.modules.role.dashboard.service.LabelerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labeler/dashboard")
@PreAuthorize("hasRole('LABELER')")
@Tag(name = "Labeler 角色数据看板", description = "标注员查看个人产出与收益数据")
public class LabelerDashboardController {

    private final LabelerDashboardService service;

    public LabelerDashboardController(LabelerDashboardService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @Operation(summary = "Labeler 看板总览", description = "返回当前标注员的领取、提交、贡献和奖励数据")
    public ApiResponse<LabelerDashboardOverviewResponse> overview(
            @RequestParam(required = false) String range,
            HttpServletRequest request) {
        DashboardRequestGuard.rejectUserIdParams(request);
        return ApiResponse.ok(service.getOverview(DashboardRange.from(range, DashboardRange.LAST_30_DAYS)));
    }
}
