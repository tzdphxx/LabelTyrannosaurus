package com.labelhub.modules.submission.dto;

import java.util.List;

public record AnswerDiffResponse(
        Long baseSubmissionId,
        Integer baseVersionNo,
        Long targetSubmissionId,
        Integer targetVersionNo,
        List<FieldDiff> diffs
) {}
