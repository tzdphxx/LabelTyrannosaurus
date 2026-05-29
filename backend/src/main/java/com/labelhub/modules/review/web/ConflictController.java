package com.labelhub.modules.review.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.review.dto.ConflictGroupResponse;
import com.labelhub.modules.review.dto.ConflictResolveRequest;
import com.labelhub.modules.review.dto.ConflictResolveResponse;
import com.labelhub.modules.review.service.ConflictResolveService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviewer/conflict-groups")
public class ConflictController {

    private final ConflictResolveService conflictResolveService;

    public ConflictController(ConflictResolveService conflictResolveService) {
        this.conflictResolveService = conflictResolveService;
    }

    @GetMapping
    public ApiResponse<List<ConflictGroupResponse>> listOpenGroups() {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(conflictResolveService.listOpenGroups());
    }

    @GetMapping("/{groupId}")
    public ApiResponse<ConflictGroupResponse> getGroup(@PathVariable Long groupId) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(conflictResolveService.getGroup(groupId));
    }

    @PostMapping("/{groupId}/resolve")
    public ApiResponse<ConflictResolveResponse> resolve(@PathVariable Long groupId,
                                                         @Valid @RequestBody ConflictResolveRequest request) {
        CurrentUserContext.requireRole(RoleCode.REVIEWER);
        return ApiResponse.ok(conflictResolveService.resolve(
                groupId, CurrentUserContext.getUserId(), request));
    }
}
