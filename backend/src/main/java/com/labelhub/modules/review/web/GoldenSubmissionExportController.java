package com.labelhub.modules.review.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.review.dto.ExportPageRequest;
import com.labelhub.modules.review.dto.ExportPageResponse;
import com.labelhub.modules.review.service.ExportSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/export")
@Tag(name = "导出", description = "优质提交导出查询")
public class GoldenSubmissionExportController {

    private final ExportSnapshotService exportSnapshotService;

    public GoldenSubmissionExportController(ExportSnapshotService exportSnapshotService) {
        this.exportSnapshotService = exportSnapshotService;
    }

    @GetMapping("/golden-submissions")
    @Operation(summary = "优质提交分页", description = "查询可导出的优质提交快照。")
    public ApiResponse<ExportPageResponse> queryGoldenSubmissions(
            @RequestParam Long taskId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(exportSnapshotService.queryExportableGoldenSubmissions(
                CurrentUserContext.getUserId(),
                new ExportPageRequest(taskId, lastId, limit)));
    }
}
