package com.labelhub.modules.agent.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.dto.AgentRunDetailResponse;
import com.labelhub.modules.agent.service.AgentRunQueryService;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentRunControllerTest {

    @Mock private AgentRunQueryService queryService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void delegatesToQueryServiceWithCurrentUser() {
        AgentRunController controller = new AgentRunController(queryService);
        CurrentUser reviewer = new CurrentUser(20L, "reviewer", "test@labelhub.dev", Set.of(RoleCode.REVIEWER), 1);
        CurrentUserContext.set(reviewer);
        AgentRunDetailResponse detail = new AgentRunDetailResponse(
                1L, "AI_REVIEW", 30L, 3L, "qwen-plus", "v1",
                AgentRunStatus.SUCCESS, Map.of("promptMode", "TEXT_ONLY"), Map.of(),
                null, null, null, null, false);
        when(queryService.getDetail(reviewer, 1L)).thenReturn(detail);

        ApiResponse<AgentRunDetailResponse> response = controller.getDetail(1L);

        assertThat(response.data()).isEqualTo(detail);
        verify(queryService).getDetail(reviewer, 1L);
    }

    @Test
    void requiresAuthenticatedUser() {
        AgentRunController controller = new AgentRunController(queryService);

        assertThatThrownBy(() -> controller.getDetail(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(401001));
    }
}
