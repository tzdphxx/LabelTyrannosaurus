package com.labelhub.modules.submission.dto;

public record FieldDiff(
        String field,
        Object before,
        Object after,
        ChangeType changeType
) {
    public enum ChangeType {
        ADDED, MODIFIED, REMOVED
    }
}
