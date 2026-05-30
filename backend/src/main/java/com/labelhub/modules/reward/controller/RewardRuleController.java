package com.labelhub.modules.reward.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.reward.dto.RewardRuleRequest;
import com.labelhub.modules.reward.dto.RewardRuleResponse;
import com.labelhub.modules.reward.service.RewardRuleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 奖励规则配置接口。仅任务 Owner 或 ADMIN 可配置，规则以版本追加保存。
 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/reward-rule")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
public class RewardRuleController {

    private final RewardRuleService rewardRuleService;

    public RewardRuleController(RewardRuleService rewardRuleService) {
        this.rewardRuleService = rewardRuleService;
    }

    /**
     * 保存任务奖励规则的新版本，历史流水不因本次配置变更重算。
     */
    @PostMapping
    public ApiResponse<RewardRuleResponse> saveRule(@PathVariable Long taskId,
                                                    @Valid @RequestBody RewardRuleRequest request) {
        return ApiResponse.ok(rewardRuleService.saveRule(taskId, request));
    }

    /**
     * 查询任务最新奖励规则，用于 Owner 配置页回显。
     */
    @GetMapping
    public ApiResponse<RewardRuleResponse> getLatestRule(@PathVariable Long taskId) {
        return ApiResponse.ok(rewardRuleService.getLatestRule(taskId));
    }
}
