package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.LabelerClaimedTaskResponse;
import com.labelhub.modules.assignment.service.LabelerAssignmentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labeler/claimed-tasks")
@Tag(name = "Labeler Claimed Tasks", description = "Task-level views for the current labeler's claimed items")
public class LabelerClaimedTaskController {

    private final LabelerAssignmentQueryService queryService;

    public LabelerClaimedTaskController(LabelerAssignmentQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List claimed tasks", description = "List tasks that contain items claimed by the current labeler.")
    public ApiResponse<List<LabelerClaimedTaskResponse>> listClaimedTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(queryService.listClaimedTasks(CurrentUserContext.getUserId(), page, size));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get claimed task detail", description = "Get one task and the current labeler's claimed items under it.")
    public ApiResponse<LabelerClaimedTaskResponse> getClaimedTaskDetail(
            @PathVariable Long taskId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(queryService.getClaimedTaskDetail(
                CurrentUserContext.getUserId(), taskId, status, page, size));
    }
}
