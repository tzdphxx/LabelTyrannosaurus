# 任务级直接导出接口

## 背景

金标导出语义已废弃。新的任务级导出只以提交审核状态为依据：只导出 `submissions.status = 'APPROVED'` 的提交。

## 创建并返回下载链接

`POST /api/v1/tasks/{taskId}/exports/direct`


请求体：

```json
{
  "format": "JSONL"
}
```

字段：
- `format`：可选，支持 `JSON`、`JSONL`、`CSV`、`XLSX`。为空时默认 `JSONL`。

响应：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "fileId": 900,
    "filename": "task-1-approved-submissions.jsonl",
    "contentType": "application/x-ndjson",
    "fileSize": 1234,
    "checksum": "sha256...",
    "downloadUrl": "https://cos.example.com/signed-url",
    "exportedCount": 10
  },
  "traceId": null
}
```

行为：
- 同步查询、生成文件、上传对象存储，并立即返回短期签名下载链接。
- 文件记录写入 `object_files`，`businessType=export`。
- 空结果也生成合法文件，`exportedCount=0`。
- 不提供后端流式下载；客户端下载 `downloadUrl`。



## 导出字段

每条记录包含：

| 字段 | 来源 |
|---|---|
| `taskId` | `submissions.task_id` |
| `submissionId` | `submissions.id` |
| `datasetItemId` | `submissions.dataset_item_id` |
| `labelerId` | `submissions.labeler_id` |
| `versionNo` | `submissions.version_no` |
| `submittedAt` | `submissions.submitted_at` |
| `itemSnapshot` | `dataset_items.item_json` |
| `answerJson` | `submissions.answer_json` |
| `aiReviewSnapshot` | 最新 AI 审核摘要，可为空 |
| `reviewComment` | 最新人工审核 `comment/reason`，可为空 |

`aiReviewSnapshot` 和 `reviewComment` 只作为摘要输出，不作为是否可导出的判断依据。

## 文件格式

- `JSON`：数组结构。
- `JSONL`：一行一条记录。
- `CSV`：固定列，JSON 字段序列化为字符串，并做 CSV 转义。
- `XLSX`：单 sheet，列与 CSV 一致，可直接下载打开。
