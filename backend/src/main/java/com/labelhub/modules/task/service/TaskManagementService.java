package com.labelhub.modules.task.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.dto.OwnerTaskPageResponse;
import com.labelhub.modules.task.dto.OwnerTaskSummaryResponse;
import com.labelhub.modules.task.dto.TaskLabelerResponse;
import com.labelhub.modules.task.dto.TaskStatisticsResponse;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskManagementService {

    private static final int TASK_NOT_FOUND = 404001;
    private static final int TASK_STATUS_NOT_ALLOWED = 400101;

    private final TaskMapper taskMapper;
    private final TaskTagMapper taskTagMapper;
    private final AssignmentMapper assignmentMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final SubmissionMapper submissionMapper;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;

    public TaskManagementService(TaskMapper taskMapper,
                                 TaskTagMapper taskTagMapper,
                                 AssignmentMapper assignmentMapper,
                                 DatasetItemMapper datasetItemMapper,
                                 SubmissionMapper submissionMapper,
                                 AuditAppender auditAppender,
                                 TraceIdProvider traceIdProvider) {
        this.taskMapper = taskMapper;
        this.taskTagMapper = taskTagMapper;
        this.assignmentMapper = assignmentMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.submissionMapper = submissionMapper;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
    }

    public OwnerTaskPageResponse listOwnerTasksPage(Long ownerId,
                                                    String status,
                                                    String keyword,
                                                    int page,
                                                    int size) {
        int normalizedPage = Math.max(1, page);
        int normalizedSize = Math.min(Math.max(1, size), 100);
        int offset = (normalizedPage - 1) * normalizedSize;

        long total = taskMapper.countOwnerTasks(ownerId, status, keyword);
        List<OwnerTaskSummaryResponse> items = taskMapper
                .selectOwnerTasksPage(ownerId, status, keyword, normalizedSize, offset)
                .stream()
                .map(task -> new OwnerTaskSummaryResponse(
                        task.getId(), task.getTitle(), task.getStatus(),
                        listTags(task.getId()), task.getQuota(),
                        task.getClaimedCount(), task.getOverlapCount(),
                        task.getStrategy(),
                        task.getDeadlineAt(), task.getPublishedAt(),
                        task.getEndedAt(), task.getCreatedAt(), task.getUpdatedAt()))
                .toList();

        return new OwnerTaskPageResponse(items, normalizedPage, normalizedSize, total);
    }

    public TaskStatisticsResponse getStatistics(Long ownerId, Long taskId) {
        Task task = loadOwnedTask(ownerId, taskId);
        int totalItems = datasetItemMapper.countByTaskId(taskId);
        int submitted = submissionMapper.countByTaskIdAndStatus(taskId, "PENDING_FINAL");
        int approved = submissionMapper.countByTaskIdAndStatus(taskId, "APPROVED");
        int rejected = submissionMapper.countByTaskIdAndStatus(taskId, "REJECTED");
        int pendingReview = submissionMapper.countByTaskIdAndStatus(taskId, "PENDING_FINAL");

        String passRate = "0.00%";
        int total = approved + rejected;
        if (total > 0) {
            passRate = BigDecimal.valueOf(approved)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                    .toPlainString() + "%";
        }

        return new TaskStatisticsResponse(taskId, totalItems,
                task.getClaimedCount() != null ? task.getClaimedCount() : 0,
                submitted + approved + rejected, approved, rejected,
                pendingReview, passRate);
    }

    @Transactional
    public void deleteDraft(Long ownerId, Long taskId) {
        Task task = loadOwnedTask(ownerId, taskId);
        if (task.getStatus() != TaskStatus.DRAFT) {
            throw new BusinessException(TASK_STATUS_NOT_ALLOWED,
                    "Only draft tasks can be deleted");
        }
        taskTagMapper.delete(new QueryWrapper<TaskTag>().eq("task_id", taskId));
        taskMapper.deleteById(taskId);
        auditAppender.append(new AuditCommand("USER", ownerId, "TASK", taskId,
                "TASK_DELETED", Map.of("status", "DRAFT"), null,
                traceIdProvider.currentTraceId(), null));
    }

    public List<TaskLabelerResponse> getLabelers(Long ownerId, Long taskId) {
        loadOwnedTask(ownerId, taskId);
        return assignmentMapper.selectLabelersByTask(taskId)
                .stream()
                .map(row -> new TaskLabelerResponse(
                        toLong(row.get("labeler_id")),
                        (String) row.get("username"),
                        (String) row.get("display_name"),
                        toInt(row.get("claimed_count")),
                        toInt(row.get("submitted_count")),
                        toInt(row.get("approved_count")),
                        toInt(row.get("rejected_count")),
                        toInt(row.get("cancelled_count")),
                        toLocalDateTime(row.get("first_claimed_at")),
                        toLocalDateTime(row.get("last_activity_at"))
                ))
                .toList();
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long l) return l;
        return ((Number) val).longValue();
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Integer i) return i;
        return ((Number) val).intValue();
    }

    private java.time.LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        return (java.time.LocalDateTime) val;
    }

    private Task loadOwnedTask(Long ownerId, Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || !ownerId.equals(task.getOwnerId())) {
            throw new BusinessException(TASK_NOT_FOUND, "Task not found");
        }
        return task;
    }

    private List<String> listTags(Long taskId) {
        return taskTagMapper.selectList(new QueryWrapper<TaskTag>()
                        .eq("task_id", taskId).orderByAsc("id"))
                .stream()
                .map(TaskTag::getTagName)
                .toList();
    }
}