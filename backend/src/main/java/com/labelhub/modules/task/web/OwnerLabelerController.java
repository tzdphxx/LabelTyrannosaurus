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
@Tag(name = "Owner labelers", description = "Owner reads assignable labelers for assigned tasks")
public class OwnerLabelerController {

    private final OwnerAssignableLabelerService ownerAssignableLabelerService;

    public OwnerLabelerController(OwnerAssignableLabelerService ownerAssignableLabelerService) {
        this.ownerAssignableLabelerService = ownerAssignableLabelerService;
    }

    @GetMapping("/assignable")
    @Operation(summary = "List assignable labelers",
            description = "Returns users with the LABELER role for Owner task assignment.")
    public ApiResponse<PageResponse<AssignableLabelerResponse>> listAssignableLabelers(
            @Parameter(description = "Search username, email, or display name")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Only include enabled and login-enabled labelers")
            @RequestParam(defaultValue = "true") boolean enabledOnly,
            @Parameter(description = "Page number, starts from 1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size, max 100")
            @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.OWNER);
        return ApiResponse.ok(ownerAssignableLabelerService.listAssignableLabelers(
                keyword, enabledOnly, page, size));
    }
}
