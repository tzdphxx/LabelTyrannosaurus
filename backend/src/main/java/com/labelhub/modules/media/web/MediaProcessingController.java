package com.labelhub.modules.media.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.media.dto.MediaContextResponse;
import com.labelhub.modules.media.dto.MediaProcessingJobResponse;
import com.labelhub.modules.media.service.MediaProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Media Processing", description = "BE-A multimodal media context processing")
public class MediaProcessingController {

    private final MediaProcessingService mediaProcessingService;

    public MediaProcessingController(MediaProcessingService mediaProcessingService) {
        this.mediaProcessingService = mediaProcessingService;
    }

    @PostMapping("/api/v1/dataset-items/{itemId}/media/process")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @Operation(summary = "触发媒体处理", description = "对指定数据项的媒体内容触发异步处理任务。")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", description = "请求参数校验失败"), @ApiResponse(responseCode = "401", description = "未认证"), @ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<MediaProcessingJobResponse> process(
            @Parameter(description = "数据项 ID") @PathVariable Long itemId) {
        return ApiResponse.ok(mediaProcessingService.triggerProcessing(itemId));
    }

    @GetMapping("/api/v1/dataset-items/{itemId}/media-context")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','REVIEWER','LABELER')")
    @Operation(summary = "获取媒体上下文", description = "获取指定数据项的媒体上下文信息，包含处理后的多媒体数据。")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", description = "请求参数校验失败"), @ApiResponse(responseCode = "401", description = "未认证"), @ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<MediaContextResponse> context(
            @Parameter(description = "数据项 ID") @PathVariable Long itemId) {
        return ApiResponse.ok(mediaProcessingService.getContext(itemId));
    }

    @GetMapping("/api/v1/media-processing/jobs/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','REVIEWER')")
    @Operation(summary = "查询处理任务", description = "查询媒体处理任务的状态和结果。")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", description = "请求参数校验失败"), @ApiResponse(responseCode = "401", description = "未认证"), @ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<MediaProcessingJobResponse> job(
            @Parameter(description = "处理任务 ID") @PathVariable Long jobId) {
        return ApiResponse.ok(mediaProcessingService.getJob(jobId));
    }
}
