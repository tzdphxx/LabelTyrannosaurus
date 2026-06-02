package com.labelhub.modules.assignment.service;

import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.dto.LabelerAssignmentListItem;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LabelerAssignmentQueryService {

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
