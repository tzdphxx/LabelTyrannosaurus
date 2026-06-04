package com.labelhub.modules.reward.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.reward.dto.ContributionOverviewResponse;
import com.labelhub.modules.reward.dto.DailyContributionPoint;
import com.labelhub.modules.reward.dto.RewardLedgerResponse;
import com.labelhub.modules.reward.dto.TaskContributionResponse;
import com.labelhub.modules.reward.service.ContributionStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labeler")
@PreAuthorize("hasAnyRole('LABELER','ADMIN')")
@Tag(name = "Rewards", description = "Labeler contribution stats and reward ledger")
public class ContributionController {

    private final ContributionStatsService contributionStatsService;

    public ContributionController(ContributionStatsService contributionStatsService) {
        this.contributionStatsService = contributionStatsService;
    }

    @GetMapping("/contribution/overview")
    @Operation(summary = "Contribution overview")
    public ApiResponse<ContributionOverviewResponse> overview() {
        return ApiResponse.ok(contributionStatsService.getOverview());
    }

    @GetMapping("/contribution/trend")
    @Operation(summary = "Contribution trend")
    public ApiResponse<List<DailyContributionPoint>> trend(
            @Parameter(description = "Number of days to query, default 7", example = "7")
            @RequestParam(required = false) Integer days) {
        return ApiResponse.ok(contributionStatsService.getTrend(days));
    }

    @GetMapping("/contribution/tasks")
    @Operation(summary = "Task contribution stats")
    public ApiResponse<List<TaskContributionResponse>> tasks(
            @Parameter(description = "Maximum number of rows, default 20", example = "20")
            @RequestParam(required = false) Integer limit,
            @Parameter(description = "Pagination offset, default 0", example = "0")
            @RequestParam(required = false) Integer offset) {
        return ApiResponse.ok(contributionStatsService.getTasks(limit, offset));
    }

    @GetMapping("/rewards/ledger")
    @Operation(summary = "Reward ledger")
    public ApiResponse<List<RewardLedgerResponse>> ledger(
            @Parameter(description = "Maximum number of rows, default 20", example = "20")
            @RequestParam(required = false) Integer limit,
            @Parameter(description = "Pagination offset, default 0", example = "0")
            @RequestParam(required = false) Integer offset) {
        return ApiResponse.ok(contributionStatsService.getLedger(limit, offset));
    }
}
