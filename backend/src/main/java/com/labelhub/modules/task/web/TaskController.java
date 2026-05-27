package com.labelhub.modules.task.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.task.dto.CreateTaskRequest;
import com.labelhub.modules.task.dto.TaskLifecycleResponse;
import com.labelhub.modules.task.dto.UpdateTaskRequest;
import com.labelhub.modules.task.service.TaskLifecycleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskLifecycleService taskLifecycleService;

    public TaskController(TaskLifecycleService taskLifecycleService) {
        this.taskLifecycleService = taskLifecycleService;
    }

    @PostMapping
    public ApiResponse<TaskLifecycleResponse> create(@RequestHeader("X-User-Id") Long ownerId,
                                                     @Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.ok(taskLifecycleService.create(ownerId, request));
    }

    @PutMapping("/{taskId}")
    public ApiResponse<TaskLifecycleResponse> updateDraft(@RequestHeader("X-User-Id") Long ownerId,
                                                          @PathVariable Long taskId,
                                                          @Valid @RequestBody UpdateTaskRequest request) {
        return ApiResponse.ok(taskLifecycleService.updateDraft(ownerId, taskId, request));
    }

    @PostMapping("/{taskId}/publish")
    public ApiResponse<TaskLifecycleResponse> publish(@RequestHeader("X-User-Id") Long ownerId,
                                                      @PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.publish(ownerId, taskId));
    }

    @PostMapping("/{taskId}/pause")
    public ApiResponse<TaskLifecycleResponse> pause(@RequestHeader("X-User-Id") Long ownerId,
                                                    @PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.pause(ownerId, taskId));
    }

    @PostMapping("/{taskId}/resume")
    public ApiResponse<TaskLifecycleResponse> resume(@RequestHeader("X-User-Id") Long ownerId,
                                                     @PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.resume(ownerId, taskId));
    }

    @PostMapping("/{taskId}/end")
    public ApiResponse<TaskLifecycleResponse> end(@RequestHeader("X-User-Id") Long ownerId,
                                                  @PathVariable Long taskId) {
        return ApiResponse.ok(taskLifecycleService.end(ownerId, taskId));
    }
}
