package com.labelhub.modules.preannotation.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.preannotation.dto.PreAnnotationResponse;
import com.labelhub.modules.preannotation.service.PreAnnotationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PreAnnotationController {

    private final PreAnnotationService preAnnotationService;

    public PreAnnotationController(PreAnnotationService preAnnotationService) {
        this.preAnnotationService = preAnnotationService;
    }

    @PostMapping("/api/v1/assignments/{assignmentId}/pre-annotations/run")
    public ApiResponse<PreAnnotationResponse> run(@PathVariable Long assignmentId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(preAnnotationService.run(assignmentId, CurrentUserContext.getUserId()));
    }

    @GetMapping("/api/v1/assignments/{assignmentId}/pre-annotations/latest")
    public ApiResponse<PreAnnotationResponse> latest(@PathVariable Long assignmentId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(preAnnotationService.latest(assignmentId, CurrentUserContext.getUserId()));
    }

    @GetMapping("/api/v1/pre-annotations/{preAnnotationId}")
    public ApiResponse<PreAnnotationResponse> detail(@PathVariable Long preAnnotationId) {
        return ApiResponse.ok(preAnnotationService.getDetail(preAnnotationId, CurrentUserContext.requireCurrentUser()));
    }
}
