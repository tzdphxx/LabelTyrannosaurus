# export

# Export API Contract

Owner：BE\-B

## POST /api/v1/tasks/\{taskId\}/exports

权限：OWNER。

请求字段：

```Plaintext
format = JSON/JSONL/CSV/EXCEL
includeAiReview
includeAuditTrail
fieldMapping[]
```

内部依赖：

```Plaintext
BE-A queryExportableGoldenSubmissions(taskId, pageRequest)
```

导出范围：

```Plaintext
submission.status = APPROVED
submission.isGolden = true
```

响应字段：

```Plaintext
exportJobId
status = PENDING
```

## GET /api/v1/tasks/\{taskId\}/exports

响应字段：

```Plaintext
exportJobId
format
status
createdAt
downloadUrl
errorMessage
```

## GET /api/v1/tasks/\{taskId\}/exports/\{exportJobId\}

响应字段：

```Plaintext
exportJobId
status
downloadUrl
errorMessage
```

