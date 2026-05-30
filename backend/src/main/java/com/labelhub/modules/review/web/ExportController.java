package com.labelhub.modules.review.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.review.dto.ExportPageRequest;
import com.labelhub.modules.review.dto.ExportPageResponse;
import com.labelhub.modules.review.service.ExportSnapshotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/export")
public class ExportController {

    private final ExportSnapshotService exportSnapshotService;

    public ExportController(ExportSnapshotService exportSnapshotService) {
        this.exportSnapshotService = exportSnapshotService;
    }

    @GetMapping("/golden-submissions")
    public ApiResponse<ExportPageResponse> queryGoldenSubmissions(
            @RequestParam Long taskId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "50") int limit) {
        CurrentUserContext.requireRole(RoleCode.OWNER);
        return ApiResponse.ok(exportSnapshotService.queryExportableGoldenSubmissions(
                CurrentUserContext.getUserId(),
                new ExportPageRequest(taskId, lastId, limit)));
    }
}
