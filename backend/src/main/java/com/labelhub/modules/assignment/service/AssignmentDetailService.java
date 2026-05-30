package com.labelhub.modules.assignment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.dto.AssignmentDetailResponse;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.review.domain.ReviewAction;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import org.springframework.stereotype.Service;

@Service
public class AssignmentDetailService {

    private static final int ASSIGNMENT_NOT_FOUND = 404401;
    private static final int FORBIDDEN = 403401;

    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final TemplateVersionMapper templateVersionMapper;
    private final ReviewRecordMapper reviewRecordMapper;

    public AssignmentDetailService(AssignmentMapper assignmentMapper,
                                   SubmissionMapper submissionMapper,
                                   DatasetItemMapper datasetItemMapper,
                                   TemplateVersionMapper templateVersionMapper,
                                   ReviewRecordMapper reviewRecordMapper) {
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.templateVersionMapper = templateVersionMapper;
        this.reviewRecordMapper = reviewRecordMapper;
    }

    public AssignmentDetailResponse getDetail(Long assignmentId, Long labelerId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException(ASSIGNMENT_NOT_FOUND, "Assignment not found");
        }
        if (!assignment.getLabelerId().equals(labelerId)) {
            throw new BusinessException(FORBIDDEN, "Forbidden");
        }

        DatasetItem item = datasetItemMapper.selectById(assignment.getDatasetItemId());
        TemplateVersion tv = templateVersionMapper.selectById(assignment.getTemplateVersionId());
        Submission latest = submissionMapper.selectLatestActiveByAssignmentId(assignmentId);

        String returnedReason = null;
        if (latest != null) {
            ReviewRecord rejectRecord = reviewRecordMapper.selectOne(
                    new LambdaQueryWrapper<ReviewRecord>()
                            .eq(ReviewRecord::getSubmissionId, latest.getId())
                            .eq(ReviewRecord::getAction, ReviewAction.REJECT)
                            .orderByDesc(ReviewRecord::getCreatedAt)
                            .last("LIMIT 1"));
            if (rejectRecord != null) {
                returnedReason = rejectRecord.getReason();
            }
        }

        return new AssignmentDetailResponse(
                assignment.getId(),
                assignment.getTaskId(),
                assignment.getDatasetItemId(),
                assignment.getTemplateVersionId(),
                assignment.getStatus(),
                tv != null ? tv.getSchemaJson() : null,
                item != null ? item.getItemJson() : null,
                assignment.getDraftAnswerJson(),
                assignment.getDraftVersion(),
                latest != null ? latest.getId() : null,
                latest != null ? latest.getStatus() : null,
                returnedReason,
                assignment.getReturnedAt(),
                assignment.getClaimedAt(),
                assignment.getUpdatedAt()
        );
    }
}
