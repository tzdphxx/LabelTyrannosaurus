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
 * 审计时间线查询接口。
 *
 * <p>该 Controller 向前端审计时间线暴露 BE-B 维护的只追加审计日志。
 * 审计写入仍通过内部的 {@link AuditLogService} 完成。</p>
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
     * 查询单个业务对象的审计日志。
     *
     * @param bizType 业务对象类型，例如 SUBMISSION 或 AI_REVIEW
     * @param bizId 业务对象 id
     * @return 排序后的审计时间线
     */
    @GetMapping
    public ApiResponse<List<AuditLogResponse>> listByBiz(@RequestParam String bizType,
                                                         @RequestParam Long bizId) {
        return ApiResponse.ok(auditLogService.listByBiz(bizType, bizId));
    }
}
