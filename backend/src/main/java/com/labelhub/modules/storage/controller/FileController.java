package com.labelhub.modules.storage.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.storage.dto.FileUploadResponse;
import com.labelhub.modules.storage.dto.SignedUrlResponse;
import com.labelhub.modules.storage.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "文件", description = "文件上传和短期签名下载地址")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 上传文件到对象存储并记录元数据。
     *
     * <p>归属用户由 JWT 上下文决定，Controller 不信任前端传入 owner 信息。</p>
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件", description = "上传文件到对象存储，并记录当前用户归属的文件元数据。")
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("businessType") String businessType) {
        return ApiResponse.ok(fileService.upload(file, businessType));
    }

    /**
     * 获取文件短期签名下载地址。
     *
     * <p>权限由服务层按 object_files.owner_id 和 ADMIN 角色校验。</p>
     */
    @GetMapping("/{fileId}/signed-url")
    @Operation(summary = "获取签名下载地址", description = "根据文件权限生成短期有效的下载地址。")
    public ApiResponse<SignedUrlResponse> signedUrl(@PathVariable Long fileId) {
        return ApiResponse.ok(fileService.generateSignedUrl(fileId));
    }
}
