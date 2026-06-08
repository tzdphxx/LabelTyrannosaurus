package com.labelhub.modules.task.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.PageResponse;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.task.dto.AssignableLabelerResponse;
import com.labelhub.modules.task.service.OwnerAssignableLabelerService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OwnerLabelerControllerTest {

    private final OwnerAssignableLabelerService service =
            org.mockito.Mockito.mock(OwnerAssignableLabelerService.class);
    private final OwnerLabelerController controller = new OwnerLabelerController(service);

    @BeforeEach
    void setUp() {
        CurrentUserContext.set(new CurrentUser(1L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void ownerCanListAssignableLabelers() {
        PageResponse<AssignableLabelerResponse> page = new PageResponse<>(List.of(), 1, 20, 0);
        when(service.listAssignableLabelers(null, true, 1, 20)).thenReturn(page);

        var response = controller.listAssignableLabelers(null, true, 1, 20);

        assertThat(response.data()).isEqualTo(page);
    }

    @Test
    void nonOwnerCannotListAssignableLabelers() {
        CurrentUserContext.set(new CurrentUser(2L, "labeler", "l@example.com", Set.of(RoleCode.LABELER), 1));

        assertThatThrownBy(() -> controller.listAssignableLabelers(null, true, 1, 20))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void controllerDelegatesPaginationToService() {
        controller.listAssignableLabelers("qa", false, -3, 999);

        verify(service).listAssignableLabelers("qa", false, -3, 999);
    }
}
