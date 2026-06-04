package com.labelhub.modules.assignment.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.LabelerTaskDetailResponse;
import com.labelhub.modules.assignment.dto.LabelerTaskTemplateResponse;
import com.labelhub.modules.assignment.service.LabelerTaskWorkspaceService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LabelerTaskWorkspaceControllerTest {

    private final LabelerTaskWorkspaceService service =
            org.mockito.Mockito.mock(LabelerTaskWorkspaceService.class);
    private final LabelerTaskWorkspaceController controller = new LabelerTaskWorkspaceController(service);

    @BeforeEach
    void setUp() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "labeler@example.com", Set.of(RoleCode.LABELER), 1));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void labelerCanGetTaskDetail() {
        LabelerTaskDetailResponse detail = new LabelerTaskDetailResponse(
                null, "desc", "instruction", 40L, 1, 0, null, List.of());
        when(service.getTaskDetail(20L, 10L, 1, 20)).thenReturn(detail);

        var response = controller.getTaskDetail(10L, 1, 20);

        assertThat(response.data()).isEqualTo(detail);
    }

    @Test
    void labelerCanGetAnswerTemplate() {
        LabelerTaskTemplateResponse template = new LabelerTaskTemplateResponse(10L, 40L, "{\"type\":\"object\"}");
        when(service.getAnswerTemplate(20L, 10L)).thenReturn(template);

        var response = controller.getAnswerTemplate(10L);

        assertThat(response.data()).isEqualTo(template);
    }

    @Test
    void nonLabelerCannotGetTaskDetail() {
        CurrentUserContext.set(new CurrentUser(30L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));

        assertThatThrownBy(() -> controller.getTaskDetail(10L, 1, 20))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void controllerPassesPagingToService() {
        controller.getTaskDetail(10L, -1, 500);

        verify(service).getTaskDetail(20L, 10L, -1, 500);
    }
}
