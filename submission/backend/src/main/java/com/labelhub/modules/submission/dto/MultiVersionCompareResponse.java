package com.labelhub.modules.submission.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MultiVersionCompareResponse(
        List<VersionInfo> versions,
        List<FieldComparison> fields
) {
    public record VersionInfo(
            Long submissionId,
            Integer versionNo,
            LocalDateTime submittedAt,
            Long createdBy,
            String creatorName
    ) {}

    public record FieldComparison(
            String fieldPath,
            Map<Integer, Object> valuesByVersion,
            boolean hasDifference
    ) {}
}
