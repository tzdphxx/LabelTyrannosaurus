# Dataset 数据集导入接口

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
