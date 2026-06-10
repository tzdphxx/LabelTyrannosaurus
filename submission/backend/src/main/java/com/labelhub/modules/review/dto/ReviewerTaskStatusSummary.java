package com.labelhub.modules.review.dto;

public record ReviewerTaskStatusSummary(
        long unclaimedCount,
        long claimedCount,
        long draftCount,
        long submittedCount,
        long returnedCount,
        long approvedCount
) {
}
