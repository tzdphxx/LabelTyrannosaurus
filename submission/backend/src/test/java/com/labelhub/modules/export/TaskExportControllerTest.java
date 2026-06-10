package com.labelhub.modules.export;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.export.controller.TaskExportController;
import com.labelhub.modules.export.dto.DirectTaskExportRequest;
import com.labelhub.modules.export.dto.DirectTaskExportResponse;
import com.labelhub.modules.export.service.DirectTaskExportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskExportControllerTest {

    private final DirectTaskExportService service = mock(DirectTaskExportService.class);
    private final TaskExportController controller = new TaskExportController(service);

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void directExportDefaultsToJsonlWhenFormatMissing() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        when(service.exportDirect(1L, new DirectTaskExportRequest(null))).thenReturn(response("jsonl"));

        var apiResponse = controller.directExport(1L, new DirectTaskExportRequest(null));

        assertThat(apiResponse.data().filename()).endsWith(".jsonl");
        ArgumentCaptor<DirectTaskExportRequest> requestCaptor = ArgumentCaptor.forClass(DirectTaskExportRequest.class);
        verify(service).exportDirect(org.mockito.Mockito.eq(1L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().format()).isNull();
    }

    @Test
    void directExportAcceptsJsonJsonlCsvAndXlsxFormats() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        for (String format : List.of("JSON", "JSONL", "CSV", "XLSX")) {
            when(service.exportDirect(1L, new DirectTaskExportRequest(format))).thenReturn(response(format.toLowerCase()));

            var apiResponse = controller.directExport(1L, new DirectTaskExportRequest(format));

            assertThat(apiResponse.data().filename()).endsWith("." + format.toLowerCase());
        }
    }

    @Test
    void labelerCannotUseOwnerAdminDirectExportEndpoint() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "labeler@example.com", Set.of(RoleCode.LABELER), 1));

        assertThatThrownBy(() -> controller.directExport(1L, new DirectTaskExportRequest("JSONL")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403001);
    }

    private DirectTaskExportResponse response(String extension) {
        return new DirectTaskExportResponse(
                900L,
                "task-1-approved-submissions." + extension,
                extension.equals("xlsx") ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "application/x-ndjson",
                12L,
                "checksum-1",
                "https://cos.example.com/download",
                1
        );
    }
}
