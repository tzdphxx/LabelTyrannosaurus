package com.labelhub.modules.audit.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.audit.dto.AuditLogResponse;
import com.labelhub.modules.audit.service.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Audit timeline query API.
 *
 * <p>The controller exposes BE-B maintained append-only audit logs for FE audit
 * timelines. Audit writes remain internal through {@link AuditLogService}.</p>
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN','OWNER','REVIEWER')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Queries audit logs for one business object.
     *
     * @param bizType business object type, for example SUBMISSION or AI_REVIEW
     * @param bizId business object id
     * @return ordered audit timeline
     */
    @GetMapping
    public ApiResponse<List<AuditLogResponse>> listByBiz(@RequestParam String bizType,
                                                         @RequestParam Long bizId) {
        return ApiResponse.ok(auditLogService.listByBiz(bizType, bizId));
    }
}
