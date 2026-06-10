package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "批量指派请求，Owner 将指定数据项逐条指派给标注员")
public record DispatchRequest(
        @Schema(description = "指派条目列表，单次最多 500 条", required = true)
        @NotNull
        @Size(min = 1, max = 500)
        @Valid
        List<DispatchEntry> dispatches
) {

    @Schema(description = "单条指派：将一条数据项指派给一个标注员")
    public record DispatchEntry(
            @Schema(description = "标注员用户 ID", example = "100")
            @NotNull
            Long labelerId,

            @Schema(description = "数据集项的 ID，必须属于该任务且未被指派", example = "500")
            @NotNull
            Long datasetItemId
    ) {
    }
}
