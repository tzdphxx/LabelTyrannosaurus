package com.labelhub.modules.task.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.api.PageResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.task.dto.AssignableLabelerResponse;
import com.labelhub.modules.task.service.OwnerAssignableLabelerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/labelers")
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Owner labelers", description = "OWNER 查看可分配的标注员列表")
public class OwnerLabelerController {

    private final OwnerAssignableLabelerService ownerAssignableLabelerService;

    public OwnerLabelerController(OwnerAssignableLabelerService ownerAssignableLabelerService) {
        this.ownerAssignableLabelerService = ownerAssignableLabelerService;
    }

    @GetMapping("/assignable")
    @Operation(summary = "获取可分配标注员列表",
            description = "返回具有标注员角色的用户列表，供任务所有者进行任务分配。")
    public ApiResponse<PageResponse<AssignableLabelerResponse>> listAssignableLabelers(
            @Parameter(description = "搜索关键词：用户名、邮箱或显示名称")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "仅包含已启用且允许登录的标注员")
            @RequestParam(defaultValue = "true") boolean enabledOnly,
            @Parameter(description = "页码，从1开始")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量，最大100")
            @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.OWNER);
        return ApiResponse.ok(ownerAssignableLabelerService.listAssignableLabelers(
                keyword, enabledOnly, page, size));
    }
}
