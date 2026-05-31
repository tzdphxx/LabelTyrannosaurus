package com.labelhub.modules.task.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.task.dto.CreateTaskRequest;
import com.labelhub.modules.task.dto.OwnerTaskSummaryResponse;
import com.labelhub.modules.task.dto.TaskDetailResponse;
import com.labelhub.modules.task.dto.TaskLifecycleResponse;
import com.labelhub.modules.task.dto.UpdateTaskRequest;
import com.labelhub.modules.task.service.TaskLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "任务", description = "任务创建、编辑、详情和生命周期管理")
public class TaskController {

    private final TaskLifecycleService taskLifecycleService;

    public TaskController(TaskLifecycleService taskLifecycleService) {
        this.taskLifecycleService = taskLifecycleService;
    }

    @PostMapping
    @Operation(summary = "创建任务", description = "创建草稿任务，归属当前 OWNER 用户。")
    public ApiResponse<TaskLifecycleResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.ok(taskLifecycleService.create(CurrentUserContext.getUserId(), request));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "任务详情", description = "查询当前用户拥有的任务详情。")
    public ApiResponse<TaskDetailResponse> detail(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.getOwnedTask(CurrentUserContext.getUserId(), taskId));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "编辑草稿任务", description = "仅允许编辑 DRAFT 状态任务。")
    public ApiResponse<TaskLifecycleResponse> updateDraft(@PathVariable Long taskId,
                                                           @Valid @RequestBody UpdateTaskRequest request) {
        return ApiResponse.ok(taskLifecycleService.updateDraft(CurrentUserContext.getUserId(), taskId, request));
    }

    @PostMapping("/{taskId}/publish")
    @Operation(summary = "发布任务", description = "发布前校验数据集、模板、奖励规则、截止时间、配额和重叠数。")
    public ApiResponse<TaskLifecycleResponse> publish(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.publish(CurrentUserContext.getUserId(), taskId));
    }

    @PostMapping("/{taskId}/pause")
    @Operation(summary = "暂停任务", description = "将已发布任务切换为暂停状态。")
    public ApiResponse<TaskLifecycleResponse> pause(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.pause(CurrentUserContext.getUserId(), taskId));
    }

    @PostMapping("/{taskId}/resume")
    @Operation(summary = "恢复任务", description = "将暂停任务恢复为发布状态。")
    public ApiResponse<TaskLifecycleResponse> resume(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.resume(CurrentUserContext.getUserId(), taskId));
    }

    @PostMapping("/{taskId}/end")
    @Operation(summary = "结束任务", description = "结束已发布或暂停的任务。")
    public ApiResponse<TaskLifecycleResponse> end(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.end(CurrentUserContext.getUserId(), taskId));
    }
}

@RestController
@RequestMapping("/api/v1/owner/tasks")
@Tag(name = "任务", description = "任务创建者视角的任务列表")
class OwnerTaskController {

    private final TaskLifecycleService taskLifecycleService;

    OwnerTaskController(TaskLifecycleService taskLifecycleService) {
        this.taskLifecycleService = taskLifecycleService;
    }

    @GetMapping
    @Operation(summary = "我的任务列表", description = "查询当前 OWNER 用户创建的任务摘要列表。")
    public ApiResponse<List<OwnerTaskSummaryResponse>> listOwnerTasks() {
        return ApiResponse.ok(taskLifecycleService.listOwnerTasks(CurrentUserContext.getUserId()));
    }
}
