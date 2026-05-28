package com.labelhub.modules.dataset.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.dataset.dto.DatasetImportJobResponse;
import com.labelhub.modules.dataset.dto.DatasetImportRequest;
import com.labelhub.modules.dataset.service.DatasetImportService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/dataset")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
public class DatasetImportController {

    private final DatasetImportService datasetImportService;

    public DatasetImportController(DatasetImportService datasetImportService) {
        this.datasetImportService = datasetImportService;
    }

    @PostMapping("/import")
    public ApiResponse<DatasetImportJobResponse> appendImport(@PathVariable Long taskId,
                                                              @Valid @RequestBody DatasetImportRequest request) {
        return ApiResponse.ok(datasetImportService.createAppendImport(taskId, request));
    }

    @PostMapping("/import/overwrite")
    public ApiResponse<DatasetImportJobResponse> overwriteImport(@PathVariable Long taskId,
                                                                 @Valid @RequestBody DatasetImportRequest request) {
        return ApiResponse.ok(datasetImportService.createOverwriteImport(taskId, request));
    }

    @GetMapping("/import-jobs/{jobId}")
    public ApiResponse<DatasetImportJobResponse> getImportJob(@PathVariable Long taskId, @PathVariable Long jobId) {
        return ApiResponse.ok(datasetImportService.getImportJob(taskId, jobId));
    }
}
