# 题目提交审核历史接口对接文档

## 1. 接口概览

从某条提交记录出发，查询这条提交所属题目的提交与审核历史。


| 项目 | 说明 |
| --- | --- |
| 接口名称 | 题目提交审核历史 |
| 请求方法 | `GET` |
| 接口路径 | `/api/v1/submissions/{submissionId}/item-history` |


## 3. 请求参数

### 3.1 路径参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `submissionId` | `Long` | 是 | 当前进入详情页或审核页的提交 ID |

### 3.2 查询参数

无。

## 4. 成功响应

HTTP 状态码：`200 OK`

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "taskId": 1,
    "datasetItemId": 10,
    "histories": [
      {
        "submissionId": 100,
        "assignmentId": 20,
        "versionNo": 1,
        "status": "PENDING_FINAL",
        "submittedBy": 7,
        "submittedByName": "张三",
        "submittedAt": "2026-06-08T10:00:00",
        "aiReview": {
          "aiReviewResultId": 300,
          "agentRunId": 400,
          "status": "SUCCESS",
          "decision": "PASS",
          "reviewedAt": "2026-06-08T10:01:00"
        },
        "reviewRounds": [
          {
            "reviewRecordId": 500,
            "reviewLevel": 1,
            "reviewerId": 30,
            "reviewerName": "审核员A",
            "action": "APPROVE",
            "reason": null,
            "reviewComment": "通过",
            "reviewedAt": "2026-06-08T10:10:00"
          },
          {
            "reviewRecordId": 501,
            "reviewLevel": 2,
            "reviewerId": 31,
            "reviewerName": "审核员B",
            "action": "APPROVE",
            "reason": null,
            "reviewComment": "二审通过",
            "reviewedAt": "2026-06-08T10:20:00"
          }
        ]
      }
    ]
  },
  "traceId": null
}
```

## 5. 响应字段说明

### 5.1 外层响应

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `Integer` | 业务码，`0` 表示成功 |
| `message` | `String` | 响应消息，成功时为 `OK` |
| `data` | `SubmissionItemHistoryResponse/null` | 题目历史数据 |
| `traceId` | `String/null` | 请求追踪 ID |

### 5.2 data 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | `Long` | 任务 ID |
| `datasetItemId` | `Long` | 题目 ID |
| `histories` | `HistoryItem[]` | 该题目的提交历史列表 |

### 5.3 HistoryItem

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `submissionId` | `Long` | 提交 ID |
| `assignmentId` | `Long` | 领取记录 ID |
| `versionNo` | `Integer` | 当前 assignment 内的提交版本号 |
| `status` | `String` | 提交状态 |
| `submittedBy` | `Long` | 实际提交人或修订人 ID |
| `submittedByName` | `String/null` | 实际提交人或修订人展示名 |
| `submittedAt` | `String/null` | 提交时间，ISO-8601 格式 |
| `aiReview` | `AiReviewHistory/null` | AI 审核信息；未产生 AI 审核时为 `null` |
| `reviewRounds` | `ReviewRoundHistory[]` | 人工审核轮次列表 |

### 5.4 AiReviewHistory

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `aiReviewResultId` | `Long` | AI 审核结果 ID |
| `agentRunId` | `Long/null` | AI 执行记录 ID |
| `status` | `String` | AI 审核状态 |
| `decision` | `String/null` | AI 审核结论，例如 `PASS`、`RETURN`、`MANUAL_REVIEW` |
| `reviewedAt` | `String/null` | AI 审核完成时间 |

AI 审核时间取值优先级：

1. `agent_runs.finished_at`
2. `ai_review_results.updated_at`
3. `ai_review_results.created_at`

### 5.5 ReviewRoundHistory

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `reviewRecordId` | `Long` | 人工审核记录 ID |
| `reviewLevel` | `Integer/null` | 审核轮次，例如 `1`、`2`、`3` |
| `reviewerId` | `Long/null` | 审核员 ID |
| `reviewerName` | `String/null` | 审核员展示名 |
| `action` | `String/null` | 审核动作，例如 `APPROVE`、`REJECT`、`MARK_MANUAL_REQUIRED` |
| `reason` | `String/null` | 驳回或处理原因 |
| `reviewComment` | `String/null` | 审核备注 |
| `reviewedAt` | `String/null` | 人工审核时间 |

## 6. 排序规则

`histories` 排序：

1. `submittedAt ASC`
2. `submissionId ASC`

`reviewRounds` 排序：

1. `reviewLevel ASC`
2. `reviewedAt ASC`
3. `reviewRecordId ASC`

