package com.labelhub.modules.submission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.submission.dto.AuditRef;
import com.labelhub.modules.submission.dto.ExportPageRequest;
import com.labelhub.modules.submission.dto.ExportableSubmissionSnapshot;
import com.labelhub.modules.submission.dto.LabelerInfo;
import com.labelhub.modules.submission.repository.AuditRefRecord;
import com.labelhub.modules.submission.repository.ExportableSubmissionRecord;
import com.labelhub.modules.submission.repository.SubmissionExportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * BE-A 金标导出快照查询服务。
 *
 * <p>该服务只做只读查询和 JSON 解析，不写入 submission、review、golden 状态。</p>
 */
@Service
public class SubmissionExportQueryService {

    private final SubmissionExportMapper submissionExportMapper;
    private final ObjectMapper objectMapper;

    public SubmissionExportQueryService(SubmissionExportMapper submissionExportMapper, ObjectMapper objectMapper) {
        this.submissionExportMapper = submissionExportMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询可导出的金标提交快照。
     */
    @Transactional(readOnly = true)
    public List<ExportableSubmissionSnapshot> queryExportableGoldenSubmissions(Long taskId, ExportPageRequest pageRequest) {
        if (taskId == null || pageRequest == null || pageRequest.pageSize() < 1) {
            throw new BusinessException(400102, "Invalid export page request");
        }
        List<ExportableSubmissionRecord> records = submissionExportMapper.selectExportableGoldenSubmissions(
                taskId,
                pageRequest.afterSubmissionId(),
                pageRequest.pageSize(),
                pageRequest.includeAiReview(),
                pageRequest.includeReviewComment()
        );
        List<Long> submissionIds = records.stream().map(ExportableSubmissionRecord::submissionId).toList();
        List<AuditRef> auditRefs = pageRequest.includeAuditTrail() && !submissionIds.isEmpty()
                ? loadAuditRefs(submissionIds)
                : List.of();
        return records.stream()
                .map(record -> toSnapshot(record, auditRefs, pageRequest.includeLabelerInfo()))
                .toList();
    }

    private List<AuditRef> loadAuditRefs(List<Long> submissionIds) {
        return submissionExportMapper.selectAuditRefs(submissionIds).stream()
                .map(this::toAuditRef)
                .toList();
    }

    private AuditRef toAuditRef(AuditRefRecord record) {
        return new AuditRef(record.auditId(), record.submissionId(), record.action(), record.traceId(), record.createdAt());
    }

    private ExportableSubmissionSnapshot toSnapshot(ExportableSubmissionRecord record,
                                                    List<AuditRef> auditRefs,
                                                    boolean includeLabelerInfo) {
        List<AuditRef> refs = auditRefs.stream()
                .filter(ref -> ref.submissionId() != null && ref.submissionId().equals(record.submissionId()))
                .toList();
        return new ExportableSubmissionSnapshot(
                record.submissionId(),
                record.datasetItemId(),
                readJson(record.itemJson()),
                readJson(record.answerJson()),
                readJson(record.aiReviewJson()),
                refs,
                record.reviewComment(),
                includeLabelerInfo && record.labelerId() != null
                        ? new LabelerInfo(record.labelerId(), record.username(), record.displayName(), record.email())
                        : null,
                record.submittedAt()
        );
    }

    private com.fasterxml.jackson.databind.JsonNode readJson(String json) {
        try {
            return StringUtils.hasText(json) ? objectMapper.readTree(json) : objectMapper.nullNode();
        } catch (Exception ex) {
            throw new BusinessException(500001, "Invalid export snapshot JSON");
        }
    }
}
