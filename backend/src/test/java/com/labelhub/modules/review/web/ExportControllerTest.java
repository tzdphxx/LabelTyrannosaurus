package com.labelhub.modules.review.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.review.dto.ExportPageRequest;
import com.labelhub.modules.review.dto.ExportPageResponse;
import com.labelhub.modules.review.service.ExportSnapshotService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private ExportSnapshotService exportSnapshotService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void labelerCannotQueryGoldenSubmissions() {
        CurrentUserContext.set(labeler());
        ExportController controller = new ExportController(exportSnapshotService);

        assertThatThrownBy(() -> controller.queryGoldenSubmissions(10L, null, 50))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void ownerCanQueryGoldenSubmissions() {
        CurrentUserContext.set(owner());
        ExportController controller = new ExportController(exportSnapshotService);
        ExportPageResponse serviceResponse = new ExportPageResponse(List.of(), null, false);
        when(exportSnapshotService.queryExportableGoldenSubmissions(
                1L, new ExportPageRequest(10L, 20L, 30))).thenReturn(serviceResponse);

        ApiResponse<ExportPageResponse> response =
                controller.queryGoldenSubmissions(10L, 20L, 30);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(exportSnapshotService).queryExportableGoldenSubmissions(
                1L, new ExportPageRequest(10L, 20L, 30));
    }

    private CurrentUser owner() {
        return new CurrentUser(1L, "owner", "owner@labelhub.dev", Set.of(RoleCode.OWNER), 1);
    }

    private CurrentUser labeler() {
        return new CurrentUser(2L, "labeler", "labeler@labelhub.dev", Set.of(RoleCode.LABELER), 1);
    }
}
