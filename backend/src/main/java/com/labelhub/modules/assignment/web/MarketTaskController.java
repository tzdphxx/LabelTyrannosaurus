package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.assignment.dto.MarketTaskQueryRequest;
import com.labelhub.modules.assignment.dto.MarketTaskResponse;
import com.labelhub.modules.assignment.service.TaskMarketService;
import com.labelhub.modules.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market/tasks")
@Tag(name = "标注市场", description = "标注员可领取任务列表")
public class MarketTaskController {

    private final TaskMarketService taskMarketService;

    public MarketTaskController(TaskMarketService taskMarketService) {
        this.taskMarketService = taskMarketService;
    }

    @GetMapping
    @Operation(summary = "任务市场列表", description = "查询当前标注员可领取的已发布任务。")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", description = "请求参数校验失败"), @ApiResponse(responseCode = "401", description = "未认证"), @ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<MarketTaskResponse>> listMarketTasks(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String tag,
                                                                 @RequestParam(required = false) TaskStatus status) {
        return ApiResponse.ok(taskMarketService.listMarketTasks(
                CurrentUserContext.getUserId(),
                new MarketTaskQueryRequest(keyword, tag, status)
        ));
    }
}
