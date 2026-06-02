package com.labelhub.modules.reward.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.reward.dto.ContributionOverviewResponse;
import com.labelhub.modules.reward.dto.DailyContributionPoint;
import com.labelhub.modules.reward.dto.RewardLedgerResponse;
import com.labelhub.modules.reward.dto.TaskContributionResponse;
import com.labelhub.modules.reward.service.ContributionStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标注员贡献查询接口。所有查询基于当前 JWT 用户，不接受前端传入 labelerId。
 */
@RestController
@RequestMapping("/api/v1/labeler")
@PreAuthorize("hasAnyRole('LABELER','ADMIN')")
@Tag(name = "奖励", description = "标注员贡献统计和奖励流水")
public class ContributionController {

    private final ContributionStatsService contributionStatsService;

    public ContributionController(ContributionStatsService contributionStatsService) {
        this.contributionStatsService = contributionStatsService;
    }

    /**
     * 查询当前标注员贡献总览，待审核提交不进入通过率分母。
     */
    @GetMapping("/contribution/overview")
    @Operation(summary = "贡献概览", description = "查询当前标注员贡献总览。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<ContributionOverviewResponse> overview() {
        return ApiResponse.ok(contributionStatsService.getOverview());
    }

    /**
     * 查询近 N 日贡献趋势；缺失日期由服务层补零。
     */
    @GetMapping("/contribution/trend")
    @Operation(summary = "贡献趋势", description = "查询最近 N 天贡献趋势。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<DailyContributionPoint>> trend(@RequestParam(required = false) Integer days) {
        return ApiResponse.ok(contributionStatsService.getTrend(days));
    }

    /**
     * 查询当前标注员按任务聚合的贡献统计。
     */
    @GetMapping("/contribution/tasks")
    @Operation(summary = "任务贡献统计", description = "按任务聚合查询当前标注员贡献。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<TaskContributionResponse>> tasks(@RequestParam(required = false) Integer limit,
                                                             @RequestParam(required = false) Integer offset) {
        return ApiResponse.ok(contributionStatsService.getTasks(limit, offset));
    }

    /**
     * 查询当前标注员奖励流水，包含正向奖励和冲正记录。
     */
    @GetMapping("/rewards/ledger")
    @Operation(summary = "奖励流水", description = "查询当前标注员奖励和冲正流水。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<RewardLedgerResponse>> ledger(@RequestParam(required = false) Integer limit,
                                                          @RequestParam(required = false) Integer offset) {
        return ApiResponse.ok(contributionStatsService.getLedger(limit, offset));
    }
}
