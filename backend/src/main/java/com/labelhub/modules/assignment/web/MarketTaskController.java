package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.assignment.dto.MarketTaskQueryRequest;
import com.labelhub.modules.assignment.dto.MarketTaskResponse;
import com.labelhub.modules.assignment.service.TaskMarketService;
import com.labelhub.modules.task.domain.TaskStatus;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market/tasks")
public class MarketTaskController {

    private final TaskMarketService taskMarketService;

    public MarketTaskController(TaskMarketService taskMarketService) {
        this.taskMarketService = taskMarketService;
    }

    @GetMapping
    public ApiResponse<List<MarketTaskResponse>> listMarketTasks(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String tag,
                                                                 @RequestParam(required = false) TaskStatus status) {
        return ApiResponse.ok(taskMarketService.listMarketTasks(
                CurrentUserContext.getUserId(),
                new MarketTaskQueryRequest(keyword, tag, status)
        ));
    }
}
