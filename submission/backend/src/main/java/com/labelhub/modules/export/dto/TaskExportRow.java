package com.labelhub.modules.export.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record TaskExportRow(Long taskId,
                            Long submissionId,
                            Long datasetItemId,
                            Long labelerId,
                            Integer versionNo,
                            LocalDateTime submittedAt,
                            JsonNode itemSnapshot,
                            JsonNode answerJson,
                            JsonNode aiReviewSnapshot,
                            String reviewComment) {
}
