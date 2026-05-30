# Export 导出任务契约

## 创建导出任务

- URL: `/api/v1/tasks/{taskId}/exports`
- Method: `POST`
- 权限角色: `ADMIN`、任务 `OWNER`
- Owner 模块: BE-B

请求体:

```json
{
  "exportFormat": "JSONL",
  "includeAiReview": true,
  "includeAuditTrail": false,
  "includeReviewComment": false,
  "includeLabelerInfo": false,
  "fieldMappings": [
    {
      "sourceJsonPath": "$.submissionId",
      "targetName": "submission_id",
      "formatter": null,
      "include": true
    },
    {
      "sourceJsonPath": "$.answerJson.answer",
      "targetName": "answer",
      "formatter": null,
      "include": true
    }
  ]
}
```

说明:
- `exportFormat` 支持 `JSON`、`JSONL`、`CSV`、`EXCEL`，缺省为 `JSONL`。
- `fieldMappings` 缺省时输出标准字段；当前 `sourceJsonPath` 支持 `$.submissionId`、`$.datasetItemId`、`$.itemSnapshot.xxx`、`$.answerJson.xxx`、`$.aiReviewSnapshot.xxx`、`$.auditRefs` 等属性路径，不支持过滤器和函数。
- 创建后返回 `PENDING` 任务，文件在后台异步生成。

响应体:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "exportJobId": 500,
    "taskId": 1,
    "exportFormat": "JSONL",
    "status": "PENDING",
    "includeAiReview": true,
    "includeAuditTrail": false,
    "includeReviewComment": false,
    "includeLabelerInfo": false,
    "fieldMappingJson": "[...]",
    "resultFileId": null,
    "downloadUrl": null,
    "errorMessage": null,
    "traceId": "trace-id",
    "startedAt": null,
    "finishedAt": null,
    "createdAt": "2026-05-29T10:00:00"
  },
  "traceId": null
}
```

## 查询导出历史

- URL: `/api/v1/tasks/{taskId}/exports?page=1&pageSize=20`
- Method: `GET`
- 权限角色: `ADMIN`、任务 `OWNER`

响应体:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "total": 0
  },
  "traceId": null
}
```

## 查询导出详情

- URL: `/api/v1/tasks/{taskId}/exports/{exportJobId}`
- Method: `GET`
- 权限角色: `ADMIN`、任务 `OWNER`

说明:
- `SUCCESS` 状态会返回对象存储短期签名 `downloadUrl`。
- `FAILED` 状态必须返回 `errorMessage` 和 `traceId`。

## 导出范围

导出数据范围由 BE-A 状态决定:

```text
submission.status = APPROVED
submission.isGolden = true
```

BE-B 必须调用 BE-A 内部服务:

```text
SubmissionExportQueryService.queryExportableGoldenSubmissions(taskId, pageRequest)
```

BE-B 不直接写入 `submission.status`、`review.status`、`is_golden`，也不绕过 BE-A 快照拼装导出数据。

## 错误码

| code | 场景 |
|---|---|
| `400101` | 草稿任务不可导出 |
| `400102` | 任务或导出任务不存在；导出格式或字段映射非法；结果文件不存在 |
| `401001` | 未登录或 token 失效 |
| `403001` | 非管理员且不是任务 Owner |
| `500001` | 系统错误或导出文件生成失败 |
