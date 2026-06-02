package com.labelhub.modules.submission.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.submission.dto.SubmissionSubmitRequest;
import com.labelhub.modules.submission.dto.SubmissionSubmitResponse;
import com.labelhub.modules.submission.service.SubmissionSubmitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assignments/{assignmentId}/submit")
@Tag(name = "提交", description = "标注答案提交")
public class AssignmentSubmitController {

    private final SubmissionSubmitService submissionSubmitService;

    public AssignmentSubmitController(SubmissionSubmitService submissionSubmitService) {
        this.submissionSubmitService = submissionSubmitService;
    }

    @PostMapping
    @Operation(summary = "提交标注答案", description = "提交当前 assignment 的最终答案。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<SubmissionSubmitResponse> submit(@PathVariable Long assignmentId,
                                                        @Valid @RequestBody SubmissionSubmitRequest request) {
        return ApiResponse.ok(submissionSubmitService.submit(
                assignmentId,
                CurrentUserContext.getUserId(),
                request
        ));
    }
}
