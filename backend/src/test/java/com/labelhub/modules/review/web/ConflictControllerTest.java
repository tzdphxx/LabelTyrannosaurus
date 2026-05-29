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
import com.labelhub.modules.review.domain.ConflictStatus;
import com.labelhub.modules.review.dto.ConflictGroupResponse;
import com.labelhub.modules.review.dto.ConflictResolveRequest;
import com.labelhub.modules.review.dto.ConflictResolveResponse;
import com.labelhub.modules.review.service.ConflictResolveService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConflictControllerTest {

    @Mock
    private ConflictResolveService conflictResolveService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void labelerCannotListConflictGroups() {
        CurrentUserContext.set(labeler());
        ConflictController controller = new ConflictController(conflictResolveService);

        assertThatThrownBy(controller::listOpenGroups)
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void labelerCannotResolveConflict() {
        CurrentUserContext.set(labeler());
        ConflictController controller = new ConflictController(conflictResolveService);

        assertThatThrownBy(() -> controller.resolve(10L, new ConflictResolveRequest(20L, "best")))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403001));
    }

    @Test
    void reviewerCanListConflictGroups() {
        CurrentUserContext.set(reviewer());
        ConflictController controller = new ConflictController(conflictResolveService);
        ConflictGroupResponse serviceResponse = new ConflictGroupResponse(
                10L, 20L, 30L, ConflictStatus.OPEN, BigDecimal.ONE, null, null, null);
        when(conflictResolveService.listOpenGroups()).thenReturn(List.of(serviceResponse));

        ApiResponse<List<ConflictGroupResponse>> response = controller.listOpenGroups();

        assertThat(response.data()).containsExactly(serviceResponse);
    }

    @Test
    void reviewerResolvePassesCurrentUserId() {
        CurrentUserContext.set(reviewer());
        ConflictController controller = new ConflictController(conflictResolveService);
        ConflictResolveRequest request = new ConflictResolveRequest(20L, "best");
        ConflictResolveResponse serviceResponse = new ConflictResolveResponse(
                10L, ConflictStatus.RESOLVED, 20L, 30L);
        when(conflictResolveService.resolve(10L, 1L, request)).thenReturn(serviceResponse);

        ApiResponse<ConflictResolveResponse> response = controller.resolve(10L, request);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(conflictResolveService).resolve(10L, 1L, request);
    }

    private CurrentUser reviewer() {
        return new CurrentUser(1L, "reviewer", "reviewer@labelhub.dev", Set.of(RoleCode.REVIEWER), 1);
    }

    private CurrentUser labeler() {
        return new CurrentUser(2L, "labeler", "labeler@labelhub.dev", Set.of(RoleCode.LABELER), 1);
    }
}
