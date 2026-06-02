package com.labelhub.modules.submission.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.submission.dto.AnswerDiffResponse;
import com.labelhub.modules.submission.dto.VersionHistoryItem;
import com.labelhub.modules.submission.service.AnswerDiffService;
import com.labelhub.modules.submission.service.SubmissionVersionService;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/submissions")
@Tag(name = "提交追溯", description = "查询提交的版本历史和答案 Diff 对比")
public class SubmissionTraceController {

    private final AnswerDiffService answerDiffService;
    private final SubmissionVersionService versionService;
    private final SubmissionMapper submissionMapper;

    public SubmissionTraceController(AnswerDiffService answerDiffService,
                                     SubmissionVersionService versionService,
                                     SubmissionMapper submissionMapper) {
        this.answerDiffService = answerDiffService;
        this.versionService = versionService;
        this.submissionMapper = submissionMapper;
    }

    @GetMapping("/{submissionId}/diff")
    @Operation(summary = "答案 Diff 对比", description = "对比指定提交与基准版本之间的答案差异，返回字段级别的变更详情。仅 OWNER 和 REVIEWER 可用。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<AnswerDiffResponse> diff(
            @Parameter(description = "提交 ID") @PathVariable Long submissionId,
            @Parameter(description = "基准版本号") @RequestParam Integer baseVersionNo) {
        CurrentUserContext.requireAnyRole(Set.of(RoleCode.OWNER, RoleCode.REVIEWER));
        return ApiResponse.ok(answerDiffService.diff(submissionId, baseVersionNo));
    }

    @GetMapping("/{submissionId}/versions")
    @Operation(summary = "版本历史", description = "查询指定提交所属 assignment 的所有提交版本列表，按版本号排序。OWNER、REVIEWER、LABELER 均可查看。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<VersionHistoryItem>> versions(
            @Parameter(description = "提交 ID") @PathVariable Long submissionId) {
        CurrentUserContext.requireAnyRole(Set.of(RoleCode.OWNER, RoleCode.REVIEWER, RoleCode.LABELER));
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return ApiResponse.ok(List.of());
        }
        return ApiResponse.ok(versionService.getVersionHistory(submission.getAssignmentId()));
    }
}
