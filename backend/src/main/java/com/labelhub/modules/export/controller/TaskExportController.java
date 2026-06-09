package com.labelhub.modules.export.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.export.dto.DirectTaskExportRequest;
import com.labelhub.modules.export.dto.DirectTaskExportResponse;
import com.labelhub.modules.export.service.DirectTaskExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/exports")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
@Tag(name = "Direct Task Export", description = "Task-level approved submission export")
public class TaskExportController {

    private final DirectTaskExportService directTaskExportService;

    public TaskExportController(DirectTaskExportService directTaskExportService) {
        this.directTaskExportService = directTaskExportService;
    }

    @PostMapping("/direct")
    @Operation(summary = "Create direct export file", description = "Generate and upload a task-level export file for approved submissions.")
    public ApiResponse<DirectTaskExportResponse> directExport(@PathVariable Long taskId,
                                                              @RequestBody(required = false) DirectTaskExportRequest request) {
        CurrentUserContext.requireAnyRole(Set.of(RoleCode.ADMIN, RoleCode.OWNER));
        return ApiResponse.ok(directTaskExportService.exportDirect(taskId, request));
    }
}
