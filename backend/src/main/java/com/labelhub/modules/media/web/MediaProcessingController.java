package com.labelhub.modules.media.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.media.dto.MediaContextResponse;
import com.labelhub.modules.media.dto.MediaProcessingJobResponse;
import com.labelhub.modules.media.service.MediaProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "媒体处理", description = "多模态媒体上下文处理：对题目中的图片/视频/音频进行预处理和上下文提取")
public class MediaProcessingController {

    private final MediaProcessingService mediaProcessingService;

    public MediaProcessingController(MediaProcessingService mediaProcessingService) {
        this.mediaProcessingService = mediaProcessingService;
    }

    @PostMapping("/api/v1/dataset-items/{itemId}/media/process")
    @Operation(summary = "触发媒体处理", description = "Owner 或管理员触发指定数据项的多媒体素材解析，生成可供 AI 审核和标注预览使用的媒体上下文。")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ApiResponse<MediaProcessingJobResponse> process(@PathVariable Long itemId) {
        return ApiResponse.ok(mediaProcessingService.triggerProcessing(itemId));
    }

    @GetMapping("/api/v1/dataset-items/{itemId}/media-context")
    @Operation(summary = "查询媒体上下文", description = "查询指定数据项已生成的多媒体上下文，标注员、审核员和 Owner 均可按权限查看。")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','REVIEWER','LABELER')")
    public ApiResponse<MediaContextResponse> context(@PathVariable Long itemId) {
        return ApiResponse.ok(mediaProcessingService.getContext(itemId));
    }

    @GetMapping("/api/v1/media-processing/jobs/{jobId}")
    @Operation(summary = "查询媒体处理任务", description = "查询媒体处理任务的执行状态、错误信息和产物信息。")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','REVIEWER')")
    public ApiResponse<MediaProcessingJobResponse> job(@PathVariable Long jobId) {
        return ApiResponse.ok(mediaProcessingService.getJob(jobId));
    }
}
