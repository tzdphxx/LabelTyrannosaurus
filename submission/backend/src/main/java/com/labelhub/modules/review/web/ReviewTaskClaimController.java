package com.labelhub.modules.review.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.review.dto.ReviewTaskClaimResponse;
import com.labelhub.modules.review.service.ReviewTaskClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviewer/tasks/{taskId}/claim")
@Tag(name = "审核领取", description = "审核员整任务领取")
public class ReviewTaskClaimController {

    private final ReviewTaskClaimService claimService;

    public ReviewTaskClaimController(ReviewTaskClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    @Operation(summary = "领取整任务",
            description = "审核员领取某任务某审核级别的全部待审题目。一个 (任务,级别) 只能被一名审核员领取，"
                    + "领取后该级别下当前及后续进入待审池的提交都会自动归属给该审核员。")
    public ApiResponse<ReviewTaskClaimResponse> claim(
            @Parameter(description = "任务 ID") @PathVariable Long taskId,
            @Parameter(description = "审核级别，默认 1；多级审核任务可领取更高级别")
            @RequestParam(defaultValue = "1") Integer reviewLevel) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(claimService.claim(
                CurrentUserContext.getUserId(), taskId, reviewLevel));
    }

    @DeleteMapping
    @Operation(summary = "释放整任务领取",
            description = "审核员释放已领取的任务级别。名下该级别仍待审的提交会回到未分配状态，可被重新领取。")
    public ApiResponse<Void> release(
            @Parameter(description = "任务 ID") @PathVariable Long taskId,
            @Parameter(description = "审核级别，默认 1") @RequestParam(defaultValue = "1") Integer reviewLevel) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        claimService.release(CurrentUserContext.getUserId(), taskId, reviewLevel);
        return ApiResponse.ok(null);
    }
}
