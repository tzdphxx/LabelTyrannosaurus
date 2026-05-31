# Dataset 数据集接口

## 创建追加导入任务

- URL: `/api/v1/tasks/{taskId}/dataset/import`
- Method: `POST`
- 权限角色: `ADMIN`、任务 `OWNER`
- Owner 模块: BE-B

请求体：

```json
{
  "fileId": 99,
  "datasetType": "QA_QUALITY"
}
```

说明：
- `fileId` 来自 `/api/v1/files/upload`，导入接口不重复接收 multipart。
- `datasetType` 当前只允许 `QA_QUALITY`、`PREFERENCE_COMPARE`。
- 支持源文件格式：JSON、JSONL、Excel。
- 同一任务下 `externalId` 唯一；重复行进入错误报告，不中断其他行。

## 创建覆盖导入任务

- URL: `/api/v1/tasks/{taskId}/dataset/import/overwrite`
- Method: `POST`
- 权限角色: `ADMIN`、任务 `OWNER`

请求体同追加导入。覆盖导入仅允许任务状态为 `DRAFT`，否则返回 `409301`。

## 查询导入任务

- URL: `/api/v1/tasks/{taskId}/dataset/import-jobs/{jobId}`
- Method: `GET`
- 权限角色: `ADMIN`、任务 `OWNER`

响应体：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "jobId": 300,
    "taskId": 1,
    "status": "PARTIAL_SUCCESS",
    "importMode": "APPEND",
    "totalCount": 10,
    "successCount": 8,
    "failedCount": 2,
    "errorReportFileId": 120,
    "errorReportUrl": "https://cos.example.com/signed",
    "errorMessage": null,
    "startedAt": "2026-05-28T10:00:00",
    "finishedAt": "2026-05-28T10:00:05",
    "createdAt": "2026-05-28T09:59:59"
  },
  "traceId": null
}
```

状态值：
- `PENDING`
- `RUNNING`
- `SUCCESS`
- `FAILED`
- `PARTIAL_SUCCESS`

错误报告为 JSONL，每行结构：

```json
{"rowNo":2,"externalId":"q1","errorCode":"DUPLICATE_EXTERNAL_ID","errorMessage":"externalId already exists in this task","rawRow":{"externalId":"q1"}}
```

## 错误码

| code | 场景 |
|---|---|
| `400102` | 任务、文件或导入任务不存在；请求参数非法；不支持的文件格式 |
| `401001` | 未登录或 token 失效 |
| `403001` | 非管理员且不是任务 Owner |
| `409301` | 覆盖导入时任务不是 `DRAFT` |
| `500001` | 系统错误 |

## 查询题目列表

- URL: `/api/v1/tasks/{taskId}/dataset/items`
- Method: `GET`
- 权限角色: `ADMIN`、任务 `OWNER`
- Owner 模块: BE-B

查询参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `page` | number | 否 | 页码，从 1 开始，默认 1 |
| `pageSize` | number | 否 | 每页条数，默认 20，最大 100 |
| `datasetType` | string | 否 | `QA_QUALITY` 或 `PREFERENCE_COMPARE` |
| `externalId` | string | 否 | 按题目业务编号查询 |

响应体：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "items": [
      {
        "itemId": 100,
        "taskId": 1,
        "externalId": "q1",
        "datasetType": "QA_QUALITY",
        "itemJson": {"question": "示例问题"},
        "metadataJson": {"source": "manual"},
        "assignedCount": 0,
        "submittedCount": 0,
        "approvedCount": 0,
        "createdAt": "2026-05-28T10:00:00",
        "updatedAt": "2026-05-28T10:00:00"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1
  },
  "traceId": null
}
```

说明：
- 只返回 `deleted=false` 的题目。
- BE-B 仅读取任务归属和题目计数，不修改 BE-A 的 assignment/submission 状态。

## 批量追加题目

- URL: `/api/v1/tasks/{taskId}/dataset/items/batch-append`
- Method: `POST`
- 权限角色: `ADMIN`、任务 `OWNER`
- Owner 模块: BE-B

请求体：

```json
{
  "items": [
    {
      "externalId": "q1",
      "datasetType": "QA_QUALITY",
      "itemJson": {"question": "示例问题"},
      "metadataJson": {"source": "manual"}
    }
  ]
}
```

响应体：

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "itemId": 100,
      "externalId": "q1",
      "success": true,
      "errorCode": null,
      "errorMessage": null
    }
  ],
  "traceId": null
}
```

说明：
- 同一任务下活跃题目的 `externalId` 唯一。
- 重复 `externalId` 只让该条失败，错误码 `400102`，不影响其他行。
- 成功追加会写入 `dataset_item_change_logs`，`changeType=BATCH_APPEND`。

## 批量更新题目

- URL: `/api/v1/tasks/{taskId}/dataset/items/batch-update`
- Method: `POST`
- 权限角色: `ADMIN`、任务 `OWNER`
- Owner 模块: BE-B

请求体：

```json
{
  "items": [
    {
      "itemId": 100,
      "itemJson": {"question": "更新后的问题"},
      "metadataJson": {"source": "manual"}
    }
  ]
}
```

响应体同批量追加，逐条返回 `BatchItemResult`。

说明：
- 只允许更新未删除、未领取、未提交的题目。
- `assignedCount > 0` 或 `submittedCount > 0` 时该条失败，错误码 `400101`。
- 成功更新会写入 `dataset_item_change_logs`，`changeType=BATCH_UPDATE`。

## 批量删除题目

- URL: `/api/v1/tasks/{taskId}/dataset/items/batch-delete`
- Method: `POST`
- 权限角色: `ADMIN`、任务 `OWNER`
- Owner 模块: BE-B

请求体：

```json
{
  "itemIds": [100, 101]
}
```

响应体同批量追加，逐条返回 `BatchItemResult`。

说明：
- 删除为软删除，只更新 `dataset_items.deleted=true`。
- 已领取或已提交题目不可删除，错误码 `400101`。
- 成功删除会写入 `dataset_item_change_logs`，`changeType=BATCH_DELETE`。

## 内部快照与领取预留能力

BE-B 提供给 BE-A 的 Java Service 能力：

```text
DatasetSnapshotService.getDatasetItemSnapshot(itemId)
DatasetSnapshotService.reserveClaimableItem(taskId, labelerId)
DatasetSnapshotService.increaseSubmittedCount(itemId)
DatasetSnapshotService.increaseApprovedCount(itemId)
```

规则：
- `getDatasetItemSnapshot` 只返回未删除题目的稳定数据快照。
- `reserveClaimableItem` 只递增 `dataset_items.assigned_count`，不创建 assignment。
- `reserveClaimableItem` 仅允许 `PUBLISHED` 任务，并要求 `assignedCount < task.overlapCount`。
- 无可领取题目返回 `409201`。
- `increaseSubmittedCount` 和 `increaseApprovedCount` 必须由 BE-A 或事件消费链路显式调用，BE-B 不私自推断 submission 状态。

## 题目批量编辑错误码

| code | 场景 |
|---|---|
| `400101` | 已领取、已提交题目不可更新或删除；任务状态不允许领取 |
| `400102` | 任务或题目不存在；请求参数非法；`externalId` 重复 |
| `401001` | 未登录或 token 失效 |
| `403001` | 非管理员且不是任务 Owner |
| `409201` | 无可领取题目或领取预留冲突 |
| `500001` | 系统错误 |
