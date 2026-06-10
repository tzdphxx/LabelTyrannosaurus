package com.labelhub.modules.submission.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.submission.dto.AnswerDiffResponse;
import com.labelhub.modules.submission.dto.MultiVersionCompareResponse;
import com.labelhub.modules.submission.dto.SubmissionItemHistoryResponse;
import com.labelhub.modules.submission.dto.VersionHistoryItem;
import com.labelhub.modules.submission.service.AnswerDiffService;
import com.labelhub.modules.submission.service.SubmissionItemHistoryService;
import com.labelhub.modules.submission.service.SubmissionVersionService;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
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
    private final SubmissionItemHistoryService itemHistoryService;
    private final SubmissionMapper submissionMapper;

    public SubmissionTraceController(AnswerDiffService answerDiffService,
                                     SubmissionVersionService versionService,
                                     SubmissionItemHistoryService itemHistoryService,
                                     SubmissionMapper submissionMapper) {
        this.answerDiffService = answerDiffService;
        this.versionService = versionService;
        this.itemHistoryService = itemHistoryService;
        this.submissionMapper = submissionMapper;
    }

    @GetMapping("/{submissionId}/diff")
    @Operation(summary = "答案 Diff 对比", description = "对比指定提交与基准版本之间的答案差异，返回字段级别的变更详情。仅 OWNER 和 REVIEWER 可用。")
    public ApiResponse<AnswerDiffResponse> diff(
            @Parameter(description = "提交 ID") @PathVariable Long submissionId,
            @Parameter(description = "基准版本号") @RequestParam Integer baseVersionNo) {
        CurrentUserContext.requireAnyRole(Set.of(RoleCode.OWNER, RoleCode.REVIEWER));
        return ApiResponse.ok(answerDiffService.diff(submissionId, baseVersionNo));
    }

    @GetMapping("/{submissionId}/versions")
    @Operation(summary = "版本历史", description = "查询指定提交所属 assignment 的所有提交版本列表，按版本号排序。OWNER、REVIEWER、LABELER 均可查看。")
    public ApiResponse<List<VersionHistoryItem>> versions(
            @Parameter(description = "提交 ID") @PathVariable Long submissionId) {
        CurrentUserContext.requireAnyRole(Set.of(RoleCode.OWNER, RoleCode.REVIEWER, RoleCode.LABELER));
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return ApiResponse.ok(List.of());
        }
        return ApiResponse.ok(versionService.getVersionHistory(submission.getAssignmentId()));
    }

    @GetMapping("/{submissionId}/item-history")
    @Operation(summary = "题目提交审核历史",
            description = "从指定提交定位题目，按当前角色返回该题目的提交、AI 审核和多轮人工审核历史。Labeler 只能看到自己的提交历史。")
    public ApiResponse<SubmissionItemHistoryResponse> itemHistory(
            @Parameter(description = "提交 ID") @PathVariable Long submissionId) {
        return ApiResponse.ok(itemHistoryService.getItemHistory(submissionId));
    }

    @GetMapping("/compare")
    @Operation(summary = "多版本对比", description = "传入多个提交 ID，返回按版本的字段级并排对比。所有 ID 必须属于同一 assignment。OWNER、REVIEWER、LABELER 可用。")
    public ApiResponse<MultiVersionCompareResponse> compare(
            @Parameter(description = "提交 ID 列表，逗号分隔，例如 101,102,103") @RequestParam String ids) {
        CurrentUserContext.requireAnyRole(Set.of(RoleCode.OWNER, RoleCode.REVIEWER, RoleCode.LABELER));
        List<Long> submissionIds;
        try {
            submissionIds = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();
        } catch (NumberFormatException ex) {
            throw new BusinessException(400801, "Invalid submission ID in parameter: " + ids);
        }
        return ApiResponse.ok(answerDiffService.multiCompare(submissionIds));
    }
}
