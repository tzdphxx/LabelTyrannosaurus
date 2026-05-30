package com.labelhub.modules.submission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.submission.dto.ExportPageRequest;
import com.labelhub.modules.submission.repository.AuditRefRecord;
import com.labelhub.modules.submission.repository.ExportableSubmissionRecord;
import com.labelhub.modules.submission.repository.SubmissionExportMapper;
import com.labelhub.modules.submission.service.SubmissionExportQueryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionExportQueryServiceTest {

    private final SubmissionExportMapper submissionExportMapper = mock(SubmissionExportMapper.class);
    private final SubmissionExportQueryService service = new SubmissionExportQueryService(
            submissionExportMapper,
            new ObjectMapper()
    );

    @Test
    void queryExportableGoldenSubmissionsMapsJsonAndAuditRefs() {
        when(submissionExportMapper.selectExportableGoldenSubmissions(1L, null, 500, true, true))
                .thenReturn(List.of(record(200L), record(201L)));
        when(submissionExportMapper.selectAuditRefs(List.of(200L, 201L))).thenReturn(List.of(
                new AuditRefRecord(1L, 200L, "APPROVE", "trace-1", LocalDateTime.parse("2026-05-01T10:00:00")),
                new AuditRefRecord(2L, 201L, "RESOLVE_CONFLICT", "trace-2", LocalDateTime.parse("2026-05-01T11:00:00"))
        ));

        var snapshots = service.queryExportableGoldenSubmissions(
                1L,
                new ExportPageRequest(null, 500, true, true, true, true)
        );

        assertThat(snapshots).hasSize(2);
        assertThat(snapshots.get(0).itemSnapshot().get("question").asText()).isEqualTo("Q200");
        assertThat(snapshots.get(0).answerJson().get("answer").asText()).isEqualTo("A200");
        assertThat(snapshots.get(0).aiReviewSnapshot().get("decision").asText()).isEqualTo("PASS");
        assertThat(snapshots.get(0).aiReviewSnapshot().get("averageScore").asDouble()).isEqualTo(98.5);
        assertThat(snapshots.get(0).aiReviewSnapshot().get("riskFlags").get(0).asText()).isEqualTo("low_confidence");
        assertThat(snapshots.get(0).auditRefs()).extracting("action").containsExactly("APPROVE");
        assertThat(snapshots.get(0).labelerInfo().username()).isEqualTo("labeler");
    }

    @Test
    void queryExportableGoldenSubmissionsSkipsAuditQueryWhenNotRequested() {
        when(submissionExportMapper.selectExportableGoldenSubmissions(1L, null, 500, false, false))
                .thenReturn(List.of(record(200L)));

        service.queryExportableGoldenSubmissions(
                1L,
                new ExportPageRequest(null, 500, false, false, false, false)
        );

        verify(submissionExportMapper).selectExportableGoldenSubmissions(1L, null, 500, false, false);
        verify(submissionExportMapper, never()).selectAuditRefs(org.mockito.Mockito.anyList());
    }

    private ExportableSubmissionRecord record(Long submissionId) {
        return new ExportableSubmissionRecord(
                submissionId,
                11L,
                "{\"question\":\"Q" + submissionId + "\"}",
                "{\"answer\":\"A" + submissionId + "\"}",
                "{\"status\":\"SUCCESS\",\"decision\":\"PASS\",\"averageScore\":98.5,\"dimensionScores\":{\"accuracy\":99.0},\"riskFlags\":[\"low_confidence\"],\"suggestion\":\"Keep up the good work\",\"promptSnapshot\":\"prompt text\",\"providerId\":7,\"modelName\":\"qwen-plus\",\"retryCount\":1,\"createdAt\":\"2026-05-01T09:59:00\",\"updatedAt\":\"2026-05-01T10:00:00\"}",
                "通过",
                20L,
                "labeler",
                "标注员",
                "labeler@example.com",
                LocalDateTime.parse("2026-05-01T10:00:00")
        );
    }
}
