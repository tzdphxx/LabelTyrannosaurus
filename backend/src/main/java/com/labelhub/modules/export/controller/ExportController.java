package com.labelhub.modules.export.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.export.dto.CreateExportRequest;
import com.labelhub.modules.export.dto.ExportJobPageResponse;
import com.labelhub.modules.export.dto.ExportJobResponse;
import com.labelhub.modules.export.service.ExportJobService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 导出任务接口入口。
 *
 * <p>Controller 只负责 HTTP 契约和权限入口，导出范围、异步执行和对象存储都由 Service 层统一处理。</p>
 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/exports")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
public class ExportController {

    private final ExportJobService exportJobService;

    public ExportController(ExportJobService exportJobService) {
        this.exportJobService = exportJobService;
    }

    /**
     * 创建导出任务。
     */
    @PostMapping
    public ApiResponse<ExportJobResponse> create(@PathVariable Long taskId,
                                                 @Valid @RequestBody CreateExportRequest request,
                                                 HttpServletRequest httpServletRequest) {
        return ApiResponse.ok(exportJobService.createExport(taskId, request, httpServletRequest.getHeader("X-Trace-Id")));
    }

    /**
     * 查询导出历史。
     */
    @GetMapping
    public ApiResponse<ExportJobPageResponse> list(@PathVariable Long taskId,
                                                   @RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(exportJobService.listExports(taskId, page, pageSize));
    }

    /**
     * 查询导出任务详情。
     */
    @GetMapping("/{exportJobId}")
    public ApiResponse<ExportJobResponse> detail(@PathVariable Long taskId, @PathVariable Long exportJobId) {
        return ApiResponse.ok(exportJobService.getExportJob(taskId, exportJobId));
    }
}
