package com.labelhub.modules.review.dto;

import java.util.List;

public record ReviewClaimResponse(
        List<Long> claimedSubmissionIds,
        int claimedCount
) {
}
