package com.labelhub.modules.assignment.dto;

public record AssignmentClaimResponse(Long assignmentId,
                                      Long datasetItemId,
                                      Long templateVersionId,
                                      String schemaJson,
                                      String itemJson,
                                      String draftAnswerJson,
                                      Integer draftVersion) {
}
