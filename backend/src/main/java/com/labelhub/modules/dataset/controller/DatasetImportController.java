package com.labelhub.modules.dataset.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.dataset.dto.DatasetImportJobResponse;
import com.labelhub.modules.dataset.dto.DatasetImportRequest;
import com.labelhub.modules.dataset.service.DatasetImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据集导入接口入口。
 *
 * <p>导入源文件必须先通过文件模块上传，再以 {@code fileId} 方式提交导入任务；
 * Controller 只负责 HTTP 契约，任务归属、状态限制和逐行导入规则统一放在 Service 层。</p>
 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/dataset")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
@Tag(name = "数据集", description = "数据集导入任务")
public class DatasetImportController {

    private final DatasetImportService datasetImportService;

    public DatasetImportController(DatasetImportService datasetImportService) {
        this.datasetImportService = datasetImportService;
    }

    /**
     * 创建追加导入任务。
     *
     * <p>追加导入不会覆盖已有题目，同任务重复 {@code externalId} 会进入错误报告。</p>
     */
    @PostMapping("/import")
    @Operation(summary = "追加导入数据集", description = "从已上传文件创建追加导入任务。")
    public ApiResponse<DatasetImportJobResponse> appendImport(@PathVariable Long taskId,
                                                              @Valid @RequestBody DatasetImportRequest request) {
        return ApiResponse.ok(datasetImportService.createAppendImport(taskId, request));
    }

    /**
     * 创建覆盖导入任务。
     *
     * <p>覆盖导入只允许任务处于 DRAFT 状态，避免修改已发布、已领取或已提交的题目内容。</p>
     */
    @PostMapping("/import/overwrite")
    @Operation(summary = "覆盖导入数据集", description = "从已上传文件创建覆盖导入任务，仅允许草稿任务。")
    public ApiResponse<DatasetImportJobResponse> overwriteImport(@PathVariable Long taskId,
                                                                 @Valid @RequestBody DatasetImportRequest request) {
        return ApiResponse.ok(datasetImportService.createOverwriteImport(taskId, request));
    }

    /**
     * 查询导入任务状态。
     *
     * <p>如果任务已生成错误报告，响应中会包含短期签名下载地址。</p>
     */
    @GetMapping("/import-jobs/{jobId}")
    @Operation(summary = "导入任务详情", description = "查询导入任务状态和错误报告下载地址。")
    public ApiResponse<DatasetImportJobResponse> getImportJob(@PathVariable Long taskId, @PathVariable Long jobId) {
        return ApiResponse.ok(datasetImportService.getImportJob(taskId, jobId));
    }
}
