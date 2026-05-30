package com.labelhub.modules.preannotation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.preannotation.domain.PreAnnotationStatus;
import com.labelhub.modules.preannotation.dto.PreAnnotationResponse;
import com.labelhub.modules.preannotation.service.PreAnnotationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreAnnotationControllerTest {

    @Mock
    private PreAnnotationService preAnnotationService;

    @AfterEach
    void clear() {
        CurrentUserContext.clear();
    }

    @Test
    void runUsesCurrentLabeler() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "l@test.dev", Set.of(RoleCode.LABELER), 1));
        PreAnnotationController controller = new PreAnnotationController(preAnnotationService);
        PreAnnotationResponse serviceResponse = response();
        when(preAnnotationService.run(10L, 20L)).thenReturn(serviceResponse);

        ApiResponse<PreAnnotationResponse> response = controller.run(10L);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(preAnnotationService).run(10L, 20L);
    }

    @Test
    void latestUsesCurrentLabeler() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "l@test.dev", Set.of(RoleCode.LABELER), 1));
        PreAnnotationController controller = new PreAnnotationController(preAnnotationService);
        PreAnnotationResponse serviceResponse = response();
        when(preAnnotationService.latest(10L, 20L)).thenReturn(serviceResponse);

        ApiResponse<PreAnnotationResponse> response = controller.latest(10L);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(preAnnotationService).latest(10L, 20L);
    }

    @Test
    void detailPassesCurrentUser() {
        CurrentUser currentUser = new CurrentUser(30L, "reviewer", "r@test.dev", Set.of(RoleCode.REVIEWER), 1);
        CurrentUserContext.set(currentUser);
        PreAnnotationController controller = new PreAnnotationController(preAnnotationService);
        PreAnnotationResponse serviceResponse = response();
        when(preAnnotationService.getDetail(99L, currentUser)).thenReturn(serviceResponse);

        ApiResponse<PreAnnotationResponse> response = controller.detail(99L);

        assertThat(response.data()).isEqualTo(serviceResponse);
        verify(preAnnotationService).getDetail(99L, currentUser);
    }

    private PreAnnotationResponse response() {
        return new PreAnnotationResponse(
                1L, 10L, 50L, PreAnnotationStatus.SUCCESS,
                Map.of("label", "cat"), List.of(), List.of(), new BigDecimal("0.86"),
                List.of(), "IMAGE_SINGLE", false, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
