package com.labelhub.modules.template.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.template.dto.SchemaValidationError;
import com.labelhub.modules.template.dto.ValidateAnswerRequest;
import com.labelhub.modules.template.service.SchemaValidationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Schema 答案校验接口。
 *
 * <p>该接口给 BE-A 提交编排和前端预校验复用，只返回校验明细，不修改提交或任务状态。</p>
 */
@RestController
@RequestMapping("/api/v1/schema")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
public class SchemaValidationController {

    private final SchemaValidationService schemaValidationService;

    public SchemaValidationController(SchemaValidationService schemaValidationService) {
        this.schemaValidationService = schemaValidationService;
    }

    /**
     * 按指定模板版本校验答案 JSON。
     */
    @PostMapping("/validate-answer")
    public ApiResponse<List<SchemaValidationError>> validateAnswer(
            @Valid @RequestBody ValidateAnswerRequest request) {
        return ApiResponse.ok(schemaValidationService.validateAnswer(
                request.schemaVersionId(),
                request.answerJson()
        ));
    }
}
