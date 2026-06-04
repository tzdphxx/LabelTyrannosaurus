package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.DispatchEntryResponse;
import com.labelhub.modules.assignment.dto.DispatchRequest;
import com.labelhub.modules.assignment.service.AssignmentDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/dispatches")
@Tag(name = "指派管理", description = "Owner 手动指派标注任务")
public class AssignmentDispatchController {

    private final AssignmentDispatchService dispatchService;

    public AssignmentDispatchController(AssignmentDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping
    @Operation(summary = "批量指派", description = "Owner 将指定数据项指派给标注员（仅 ASSIGNED 策略）")
    public ApiResponse<List<DispatchEntryResponse>> dispatch(
            @PathVariable Long taskId,
            @Valid @RequestBody DispatchRequest request) {
        CurrentUserContext.requireRole(RoleCode.OWNER);
        return ApiResponse.ok(dispatchService.dispatch(
                taskId, CurrentUserContext.getUserId(), request));
    }

    @GetMapping
    @Operation(summary = "查看指派列表", description = "Owner 查看任务所有指派记录")
    public ApiResponse<List<DispatchEntryResponse>> listDispatches(
            @PathVariable Long taskId) {
        CurrentUserContext.requireRole(RoleCode.OWNER);
        return ApiResponse.ok(dispatchService.listDispatches(
                taskId, CurrentUserContext.getUserId()));
    }

    @DeleteMapping("/{dispatchId}")
    @Operation(summary = "撤销指派", description = "Owner 撤销未领取的指派")
    public ApiResponse<Void> revokeDispatch(
            @PathVariable Long taskId,
            @PathVariable Long dispatchId) {
        CurrentUserContext.requireRole(RoleCode.OWNER);
        dispatchService.revokeDispatch(taskId, CurrentUserContext.getUserId(), dispatchId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/my")
    @Operation(summary = "我的指派", description = "标注员查看自己被指派的任务")
    public ApiResponse<List<DispatchEntryResponse>> listMyDispatches(
            @PathVariable Long taskId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(dispatchService.listMyDispatches(
                taskId, CurrentUserContext.getUserId()));
    }
}
