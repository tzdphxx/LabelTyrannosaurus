package com.labelhub.modules.assignment.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.assignment.dto.MarketTaskResponse;
import com.labelhub.modules.assignment.service.TaskMarketService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketTaskControllerTest {

    @Mock
    private TaskMarketService taskMarketService;

    @Mock
    private CurrentUserContext currentUserContext;

    @Test
    void readsCurrentUserWhenListingMarketTasks() {
        when(currentUserContext.currentUserId()).thenReturn(20L);
        when(taskMarketService.listMarketTasks(any(), any())).thenReturn(List.of());
        MarketTaskController controller = new MarketTaskController(taskMarketService, currentUserContext);

        ApiResponse<List<MarketTaskResponse>> response = controller.listMarketTasks(null, null, null);

        assertThat(response.data()).isEmpty();
        verify(taskMarketService).listMarketTasks(20L, new com.labelhub.modules.assignment.dto.MarketTaskQueryRequest(null, null, null));
    }
}
