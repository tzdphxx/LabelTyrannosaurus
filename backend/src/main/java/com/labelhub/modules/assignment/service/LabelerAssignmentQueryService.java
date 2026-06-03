package com.labelhub.modules.assignment.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.dto.LabelerClaimedItemResponse;
import com.labelhub.modules.assignment.dto.LabelerClaimedTaskResponse;
import com.labelhub.modules.assignment.dto.LabelerAssignmentListItem;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.task.domain.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LabelerAssignmentQueryService {

    private static final int CLAIMED_TASK_NOT_FOUND = 404402;
    private static final int DEFAULT_ITEM_PREVIEW_SIZE = 20;

    private final AssignmentMapper assignmentMapper;

    public LabelerAssignmentQueryService(AssignmentMapper assignmentMapper) {
        this.assignmentMapper = assignmentMapper;
    }

    public List<LabelerAssignmentListItem> list(Long labelerId,
                                                Long taskId,
                                                String status,
                                                int page,
                                                int size) {
        int normalizedPage = Math.max(1, page);
        int normalizedSize = Math.min(Math.max(1, size), 100);
        int offset = (normalizedPage - 1) * normalizedSize;

        return assignmentMapper.selectLabelerAssignments(
                        labelerId, taskId, status, normalizedSize, offset)
                .stream()
                .map(this::toListItem)
                .toList();
    }

    public long count(Long labelerId, Long taskId, String status) {
        return assignmentMapper.countLabelerAssignments(labelerId, taskId, status);
    }

    public List<LabelerClaimedTaskResponse> listClaimedTasks(Long labelerId, int page, int size) {
        int normalizedPage = Math.max(1, page);
        int normalizedSize = Math.min(Math.max(1, size), 100);
        int offset = (normalizedPage - 1) * normalizedSize;

        return assignmentMapper.selectLabelerClaimedTasks(labelerId, normalizedSize, offset)
                .stream()
                .map(row -> toClaimedTask(labelerId, row, null, 1, DEFAULT_ITEM_PREVIEW_SIZE))
                .toList();
    }

    public LabelerClaimedTaskResponse getClaimedTaskDetail(Long labelerId,
                                                           Long taskId,
                                                           String status,
                                                           int page,
                                                           int size) {
        Map<String, Object> row = assignmentMapper.selectLabelerClaimedTask(labelerId, taskId);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(CLAIMED_TASK_NOT_FOUND, "Claimed task not found");
        }
        return toClaimedTask(labelerId, row, status, page, size);
    }

    private LabelerAssignmentListItem toListItem(Map<String, Object> row) {
        return new LabelerAssignmentListItem(
                toLong(row.get("id")),
                toLong(row.get("task_id")),
                (String) row.get("task_title"),
                toLong(row.get("dataset_item_id")),
                AssignmentStatus.valueOf((String) row.get("status")),
                toInt(row.get("draft_version")),
                toLocalDateTime(row.get("claimed_at")),
                toLocalDateTime(row.get("returned_at")),
                toLocalDateTime(row.get("updated_at"))
        );
    }

    private LabelerClaimedTaskResponse toClaimedTask(Long labelerId,
                                                     Map<String, Object> row,
                                                     String status,
                                                     int itemPage,
                                                     int itemSize) {
        Long taskId = toLong(row.get("task_id"));
        int normalizedPage = Math.max(1, itemPage);
        int normalizedSize = Math.min(Math.max(1, itemSize), 100);
        int offset = (normalizedPage - 1) * normalizedSize;
        return new LabelerClaimedTaskResponse(
                taskId,
                (String) row.get("title"),
                (String) row.get("description"),
                (String) row.get("instruction_rich_text"),
                TaskStatus.valueOf((String) row.get("status")),
                toInt(row.get("quota")),
                toInt(row.get("overlap_count")),
                toLocalDateTime(row.get("deadline_at")),
                toLong(row.get("published_template_version_id")),
                toLong(row.get("claimed_item_count")),
                toLocalDateTime(row.get("updated_at")),
                listClaimedItems(labelerId, taskId, status, normalizedSize, offset)
        );
    }

    private List<LabelerClaimedItemResponse> listClaimedItems(Long labelerId,
                                                              Long taskId,
                                                              String status,
                                                              int limit,
                                                              int offset) {
        return assignmentMapper.selectLabelerClaimedItems(labelerId, taskId, normalize(status), limit, offset)
                .stream()
                .map(this::toClaimedItem)
                .toList();
    }

    private LabelerClaimedItemResponse toClaimedItem(Map<String, Object> row) {
        return new LabelerClaimedItemResponse(
                toLong(row.get("assignment_id")),
                toLong(row.get("dataset_item_id")),
                AssignmentStatus.valueOf((String) row.get("assignment_status")),
                (String) row.get("item_json"),
                (String) row.get("metadata_json"),
                toInt(row.get("draft_version")),
                (String) row.get("latest_submission_status"),
                toLocalDateTime(row.get("updated_at"))
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long l) return l;
        return ((Number) val).longValue();
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer i) return i;
        return ((Number) val).intValue();
    }

    private LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDateTime ldt) return ldt;
        return (LocalDateTime) val;
    }
}
