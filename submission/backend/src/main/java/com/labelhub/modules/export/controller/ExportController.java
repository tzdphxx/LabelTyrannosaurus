package com.labelhub.modules.export.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.export.dto.CreateExportRequest;
import com.labelhub.modules.export.dto.ExportJobPageResponse;
import com.labelhub.modules.export.dto.ExportJobResponse;
import com.labelhub.modules.export.service.ExportJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "导出", description = "任务数据导出任务")
public class ExportController {

    private final ExportJobService exportJobService;

    public ExportController(ExportJobService exportJobService) {
        this.exportJobService = exportJobService;
    }

    /**
     * 创建导出任务。
     */
    @PostMapping
    @Operation(summary = "创建导出任务", description = """
            按任务创建异步导出任务。
            前端不需要强制传 X-Trace-Id；服务端会通过 TraceIdProvider 解析或生成 traceId，
            并提交异步导出作业。""")
    public ApiResponse<ExportJobResponse> create(@PathVariable Long taskId,
                                                 @Valid @RequestBody CreateExportRequest request) {
        return ApiResponse.ok(exportJobService.createExport(taskId, request));
    }

    /**
     * 查询导出历史。
     */
    @GetMapping
    @Operation(summary = "导出任务列表", description = "分页查询任务导出历史。")
    public ApiResponse<ExportJobPageResponse> list(@PathVariable Long taskId,
                                                   @RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(exportJobService.listExports(taskId, page, pageSize));
    }

    /**
     * 查询导出任务详情。
     */
    @GetMapping("/{exportJobId}")
    @Operation(summary = "导出任务详情", description = "查询导出任务状态和下载信息。")
    public ApiResponse<ExportJobResponse> detail(@PathVariable Long taskId, @PathVariable Long exportJobId) {
        return ApiResponse.ok(exportJobService.getExportJob(taskId, exportJobId));
    }
}
