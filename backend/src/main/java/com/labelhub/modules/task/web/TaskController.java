package com.labelhub.modules.task.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.task.dto.CreateTaskRequest;
import com.labelhub.modules.task.dto.OwnerTaskSummaryResponse;
import com.labelhub.modules.task.dto.TaskDetailResponse;
import com.labelhub.modules.task.dto.TaskLifecycleResponse;
import com.labelhub.modules.task.dto.UpdateTaskRequest;
import com.labelhub.modules.task.service.TaskLifecycleService;
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
public class TaskController {

    private final TaskLifecycleService taskLifecycleService;
    private final CurrentUserContext currentUserContext;

    public TaskController(TaskLifecycleService taskLifecycleService, CurrentUserContext currentUserContext) {
        this.taskLifecycleService = taskLifecycleService;
        this.currentUserContext = currentUserContext;
    }

    @PostMapping
    public ApiResponse<TaskLifecycleResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.ok(taskLifecycleService.create(currentUserContext.currentUserId(), request));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskDetailResponse> detail(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.getOwnedTask(currentUserContext.currentUserId(), taskId));
    }

    @PutMapping("/{taskId}")
    public ApiResponse<TaskLifecycleResponse> updateDraft(@PathVariable Long taskId,
                                                          @Valid @RequestBody UpdateTaskRequest request) {
        return ApiResponse.ok(taskLifecycleService.updateDraft(currentUserContext.currentUserId(), taskId, request));
    }

    @PostMapping("/{taskId}/publish")
    public ApiResponse<TaskLifecycleResponse> publish(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.publish(currentUserContext.currentUserId(), taskId));
    }

    @PostMapping("/{taskId}/pause")
    public ApiResponse<TaskLifecycleResponse> pause(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.pause(currentUserContext.currentUserId(), taskId));
    }

    @PostMapping("/{taskId}/resume")
    public ApiResponse<TaskLifecycleResponse> resume(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.resume(currentUserContext.currentUserId(), taskId));
    }

    @PostMapping("/{taskId}/end")
    public ApiResponse<TaskLifecycleResponse> end(@PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.end(currentUserContext.currentUserId(), taskId));
    }
}

@RestController
@RequestMapping("/api/v1/owner/tasks")
class OwnerTaskController {

    private final TaskLifecycleService taskLifecycleService;
    private final CurrentUserContext currentUserContext;

    OwnerTaskController(TaskLifecycleService taskLifecycleService, CurrentUserContext currentUserContext) {
        this.taskLifecycleService = taskLifecycleService;
        this.currentUserContext = currentUserContext;
    }

    @GetMapping
    public ApiResponse<List<OwnerTaskSummaryResponse>> listOwnerTasks() {
        return ApiResponse.ok(taskLifecycleService.listOwnerTasks(currentUserContext.currentUserId()));
    }
}
