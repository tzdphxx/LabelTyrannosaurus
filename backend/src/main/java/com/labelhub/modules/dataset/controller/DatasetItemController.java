package com.labelhub.modules.dataset.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.dataset.dto.BatchAppendItemsRequest;
import com.labelhub.modules.dataset.dto.BatchDeleteItemsRequest;
import com.labelhub.modules.dataset.dto.BatchItemResult;
import com.labelhub.modules.dataset.dto.BatchUpdateItemsRequest;
import com.labelhub.modules.dataset.dto.DatasetItemPageResponse;
import com.labelhub.modules.dataset.dto.DatasetItemQuery;
import com.labelhub.modules.dataset.service.DatasetItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 题目批量编辑接口入口。
 *
 * <p>Controller 只声明 HTTP 契约和角色入口，任务归属、已领取题不可改等业务边界由 Service 层统一执行。</p>
 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/dataset/items")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
@Tag(name = "数据集", description = "任务数据项查询和批量编辑")
public class DatasetItemController {

    private final DatasetItemService datasetItemService;

    public DatasetItemController(DatasetItemService datasetItemService) {
        this.datasetItemService = datasetItemService;
    }

    /**
     * 查询任务下未删除题目列表。
     */
    @GetMapping
    @Operation(summary = "数据项列表", description = "分页查询任务下未删除的数据项。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<DatasetItemPageResponse> listItems(@PathVariable Long taskId,
                                                          @RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer pageSize,
                                                          @RequestParam(required = false) String externalId) {
        return ApiResponse.ok(datasetItemService.listItems(taskId,
                new DatasetItemQuery(page, pageSize, externalId)));
    }

    /**
     * 批量追加题目。
     */
    @PostMapping("/batch-append")
    @Operation(summary = "批量追加数据项", description = "向任务数据集追加多个数据项。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<BatchItemResult>> batchAppend(@PathVariable Long taskId,
                                                          @Valid @RequestBody BatchAppendItemsRequest request) {
        return ApiResponse.ok(datasetItemService.batchAppend(taskId, request));
    }

    /**
     * 批量更新题目内容。
     */
    @PostMapping("/batch-update")
    @Operation(summary = "批量更新数据项", description = "批量更新任务数据项内容。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<BatchItemResult>> batchUpdate(@PathVariable Long taskId,
                                                          @Valid @RequestBody BatchUpdateItemsRequest request) {
        return ApiResponse.ok(datasetItemService.batchUpdate(taskId, request));
    }

    /**
     * 批量软删除题目。
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除数据项", description = "批量软删除任务数据项。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<BatchItemResult>> batchDelete(@PathVariable Long taskId,
                                                          @Valid @RequestBody BatchDeleteItemsRequest request) {
        return ApiResponse.ok(datasetItemService.batchDelete(taskId, request));
    }

}
