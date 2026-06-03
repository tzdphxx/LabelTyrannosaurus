package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.MarketTaskQueryRequest;
import com.labelhub.modules.assignment.dto.MarketTaskResponse;
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
@Tag(name = "标注员任务广场", description = "标注员浏览可领取任务、查看任务详情和可领取题目")
public class MarketTaskController {

    private final TaskMarketService taskMarketService;

    public MarketTaskController(TaskMarketService taskMarketService) {
        this.taskMarketService = taskMarketService;
    }

    @GetMapping
    @Operation(summary = "任务广场列表", description = "查询当前标注员可领取的已发布任务，支持关键词、标签和任务状态筛选。")
    public ApiResponse<List<MarketTaskResponse>> listMarketTasks(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String tag,
                                                                 @RequestParam(required = false) TaskStatus status) {
        return ApiResponse.ok(taskMarketService.listMarketTasks(
                CurrentUserContext.getUserId(),
                new MarketTaskQueryRequest(keyword, tag, status)
        ));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "任务广场详情", description = "查询已发布任务详情，以及当前标注员可领取的数据项分页预览。")
    public ApiResponse<MarketTaskResponse> getMarketTaskDetail(@PathVariable Long taskId,
                                                               @RequestParam(defaultValue = "1") int itemPage,
                                                               @RequestParam(defaultValue = "20") int itemSize) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(taskMarketService.getMarketTaskDetail(
                CurrentUserContext.getUserId(), taskId, itemPage, itemSize));
    }
}
