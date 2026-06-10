package com.labelhub.modules.task.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.api.PageResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.task.dto.TaskSummaryResponse;
import com.labelhub.modules.task.service.TaskLifecycleService;
import com.labelhub.modules.task.service.TaskManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/tasks")
@Tag(name = "Owner 任务列表", description = "任务创建者视角的任务列表")
public class OwnerTaskController {

    private final TaskManagementService taskManagementService;

    public OwnerTaskController(TaskManagementService taskManagementService) {
        this.taskManagementService = taskManagementService;
    }

    @GetMapping
    @Operation(summary = "我的任务列表（分页）",
            description = "分页查询当前 OWNER 用户创建的任务，支持按状态和关键词筛选。")
    public ApiResponse<PageResponse<TaskSummaryResponse>> listOwnerTasks(
            @Parameter(description = "按任务状态筛选：DRAFT / PUBLISHED / PAUSED / ENDED") @RequestParam(required = false) String status,
            @Parameter(description = "按标题或描述关键词搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，默认 20，最大 100") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(taskManagementService.listOwnerTasksPage(
                CurrentUserContext.getUserId(), status, keyword, page, size));
    }
}
