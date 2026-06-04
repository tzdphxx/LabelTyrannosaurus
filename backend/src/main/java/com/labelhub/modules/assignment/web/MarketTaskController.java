package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.MarketTaskQueryRequest;
import com.labelhub.modules.assignment.dto.TaskMarketResponse;
import com.labelhub.modules.assignment.service.TaskMarketService;
import com.labelhub.modules.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market/tasks")
@Tag(name = "Market Tasks", description = "Labeler-readable published task market")
public class MarketTaskController {

    private final TaskMarketService taskMarketService;

    public MarketTaskController(TaskMarketService taskMarketService) {
        this.taskMarketService = taskMarketService;
    }

    @GetMapping
    @Operation(summary = "List market tasks", description = "List published tasks available to the current labeler.")
    public ApiResponse<List<TaskMarketResponse>> listMarketTasks(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String tag,
                                                                 @RequestParam(required = false) TaskStatus status) {
        return ApiResponse.ok(taskMarketService.listMarketTasks(
                CurrentUserContext.getUserId(),
                new MarketTaskQueryRequest(keyword, tag, status)
        ));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get market task detail", description = "Get a published task and its claimable dataset items.")
    public ApiResponse<TaskMarketResponse> getMarketTaskDetail(@PathVariable Long taskId,
                                                               @RequestParam(defaultValue = "1") int itemPage,
                                                               @RequestParam(defaultValue = "20") int itemSize) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(taskMarketService.getMarketTaskDetail(
                CurrentUserContext.getUserId(), taskId, itemPage, itemSize));
    }
}
