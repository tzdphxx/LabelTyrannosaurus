package com.labelhub.modules.task.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.api.PageResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.task.dto.CreateTaskRequest;
import com.labelhub.modules.task.dto.CreateTaskResponse;
import com.labelhub.modules.task.dto.TaskLabelerResponse;
import com.labelhub.modules.task.dto.TaskResponse;
import com.labelhub.modules.task.dto.TaskStatisticsResponse;
import com.labelhub.modules.task.dto.TaskStatusResponse;
import com.labelhub.modules.task.dto.TaskSummaryResponse;
import com.labelhub.modules.task.dto.UpdateTaskRequest;
import com.labelhub.modules.task.service.TaskLifecycleService;
import com.labelhub.modules.task.service.TaskManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "任务", description = "任务创建、编辑、详情和生命周期管理")
public class TaskController {

    private final TaskLifecycleService taskLifecycleService;
    private final TaskManagementService taskManagementService;

    public TaskController(TaskLifecycleService taskLifecycleService,
                          TaskManagementService taskManagementService) {
        this.taskLifecycleService = taskLifecycleService;
        this.taskManagementService = taskManagementService;
    }

    @PostMapping
    @Operation(summary = "创建任务", description = """
            创建草稿任务，归属当前 OWNER 用户。
            当前要求创建时必须内联配置 AI 审核：需传 aiProviderId、aiPrompt、aiScoringDimensions 创建配置。
            aiReviewConfigId 会在任务创建并保存 AI 配置后由后端自动回写，创建新任务时不作为通过条件。
            内联场景下 aiPassThreshold/aiManualReviewThreshold 可缺省，后端默认 80.00/60.00；
            aiFlowPolicy 可缺省，默认 MANUAL_FIRST。""")
    public ApiResponse<CreateTaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.ok(taskLifecycleService.createWithDataset(
                CurrentUserContext.getUserId(), request));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "任务详情", description = "查询当前用户拥有的任务详情。")
    public ApiResponse<TaskResponse> detail(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.getOwnedTask(
                CurrentUserContext.getUserId(), taskId));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "编辑草稿任务", description = """
            仅允许编辑 DRAFT 状态任务。
            如果传入 aiFlowPolicy 且任务已有 AI 审核配置，后端会同步更新该配置的 AI 流转策略；
            如果任务尚无 AI 配置，aiFlowPolicy 不会创建新配置。""")
    public ApiResponse<TaskStatusResponse> updateDraft(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ApiResponse.ok(taskLifecycleService.updateDraft(
                CurrentUserContext.getUserId(), taskId, request));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除草稿任务", description = "仅允许删除 DRAFT 状态任务，已发布任务不可删除。")
    public ApiResponse<Void> deleteDraft(@PathVariable Long taskId) {
        taskManagementService.deleteDraft(CurrentUserContext.getUserId(), taskId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{taskId}/statistics")
    @Operation(summary = "任务统计", description = "查询任务的提交统计数据，包含总题目数、已领取、已提交、通过、驳回、待审核数量和通过率。")
    public ApiResponse<TaskStatisticsResponse> statistics(@PathVariable Long taskId) {
        return ApiResponse.ok(taskManagementService.getStatistics(
                CurrentUserContext.getUserId(), taskId));
    }

    @GetMapping("/{taskId}/labelers")
    @Operation(summary = "任务标注员列表", description = "查询任务下参与的标注员列表及其进度统计。")
    public ApiResponse<java.util.List<TaskLabelerResponse>> labelers(@PathVariable Long taskId) {
        return ApiResponse.ok(taskManagementService.getLabelers(
                CurrentUserContext.getUserId(), taskId));
    }

    @PostMapping("/{taskId}/publish")
    @Operation(summary = "发布任务", description = "发布草稿任务。校验所有前置条件（数据集、模板、AI配置、奖励规则）后，将任务状态从 DRAFT 转为 PUBLISHED。策略和配额发布后即冻结不可更改。")
    public ApiResponse<TaskStatusResponse> publish(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.publish(
                CurrentUserContext.getUserId(), taskId));
    }

    @PostMapping("/{taskId}/pause")
    @Operation(summary = "暂停任务", description = "暂停已发布的任务，标注员暂时无法继续领取新的标注工作。")
    public ApiResponse<TaskStatusResponse> pause(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.pause(
                CurrentUserContext.getUserId(), taskId));
    }

    @PostMapping("/{taskId}/resume")
    @Operation(summary = "恢复任务", description = "恢复已暂停的任务，标注员可重新领取标注工作。")
    public ApiResponse<TaskStatusResponse> resume(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.resume(
                CurrentUserContext.getUserId(), taskId));
    }

    @PostMapping("/{taskId}/end")
    @Operation(summary = "结束任务", description = "永久结束任务，从活跃分发和审核流程中移除。结束后标注员无法继续领取或提交。")
    public ApiResponse<TaskStatusResponse> end(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.end(
                CurrentUserContext.getUserId(), taskId));
    }
}
