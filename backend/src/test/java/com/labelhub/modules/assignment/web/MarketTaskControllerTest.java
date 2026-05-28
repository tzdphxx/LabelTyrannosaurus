package com.labelhub.modules.assignment.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.MarketTaskResponse;
import com.labelhub.modules.assignment.service.TaskMarketService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketTaskControllerTest {

    @Mock
    private TaskMarketService taskMarketService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void readsCurrentUserWhenListingMarketTasks() {
        CurrentUserContext.set(new CurrentUser(20L, "labeler", "test@labelhub.dev", Set.of(RoleCode.LABELER), 1));
        when(taskMarketService.listMarketTasks(any(), any())).thenReturn(List.of());
        MarketTaskController controller = new MarketTaskController(taskMarketService);

        ApiResponse<List<MarketTaskResponse>> response = controller.listMarketTasks(null, null, null);

        assertThat(response.data()).isEmpty();
        verify(taskMarketService).listMarketTasks(20L, new com.labelhub.modules.assignment.dto.MarketTaskQueryRequest(null, null, null));
    }
}
